/*
 * Briar Desktop
 * Copyright (C) 2025 The Briar Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.anonchatsecure.anonchat.forum;

import org.anonchatsecure.bramble.api.FormatException;
import org.anonchatsecure.bramble.api.cleanup.CleanupHook;
import org.anonchatsecure.bramble.api.client.BdfIncomingMessageHook;
import org.anonchatsecure.bramble.api.client.ClientHelper;
import org.anonchatsecure.bramble.api.data.BdfDictionary;
import org.anonchatsecure.bramble.api.data.BdfEntry;
import org.anonchatsecure.bramble.api.data.BdfList;
import org.anonchatsecure.bramble.api.data.MetadataParser;
import org.anonchatsecure.bramble.api.db.DatabaseComponent;
import org.anonchatsecure.bramble.api.db.DbException;
import org.anonchatsecure.bramble.api.db.Transaction;
import org.anonchatsecure.bramble.api.system.Clock;
import org.anonchatsecure.bramble.api.identity.Author;
import org.anonchatsecure.bramble.api.identity.AuthorId;
import org.anonchatsecure.bramble.api.identity.LocalAuthor;
import org.anonchatsecure.bramble.api.sync.Group;
import org.anonchatsecure.bramble.api.sync.GroupId;
import org.anonchatsecure.bramble.api.sync.Message;
import org.anonchatsecure.bramble.api.sync.MessageId;
import org.anonchatsecure.anonchat.api.client.MessageTracker;
import org.anonchatsecure.anonchat.api.client.MessageTracker.GroupCount;
import org.anonchatsecure.anonchat.api.forum.Forum;
import org.anonchatsecure.anonchat.api.forum.ForumFactory;
import org.anonchatsecure.anonchat.api.forum.ForumManager;
import org.anonchatsecure.anonchat.api.forum.ForumPost;
import org.anonchatsecure.anonchat.api.forum.ForumPostFactory;
import org.anonchatsecure.anonchat.api.forum.ForumPostHeader;
import org.anonchatsecure.anonchat.api.forum.event.ForumPostReceivedEvent;
import org.anonchatsecure.anonchat.api.identity.AuthorInfo;
import org.anonchatsecure.anonchat.api.identity.AuthorManager;
import org.briarproject.nullsafety.NotNullByDefault;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;

import static org.anonchatsecure.bramble.api.sync.validation.IncomingMessageHook.DeliveryAction.ACCEPT_SHARE;
import static org.anonchatsecure.anonchat.api.forum.ForumConstants.KEY_AUTHOR;
import static org.anonchatsecure.anonchat.api.forum.ForumConstants.KEY_HAS_AUDIO;
import static org.anonchatsecure.anonchat.api.forum.ForumConstants.KEY_HAS_IMAGE;
import static org.anonchatsecure.anonchat.api.forum.ForumConstants.KEY_LOCAL;
import static org.anonchatsecure.anonchat.api.forum.ForumConstants.KEY_PARENT;
import static org.anonchatsecure.anonchat.api.forum.ForumConstants.KEY_TIMESTAMP;
import static org.anonchatsecure.anonchat.client.MessageTrackerConstants.MSG_KEY_READ;

@ThreadSafe
@NotNullByDefault
class ForumManagerImpl extends BdfIncomingMessageHook
		implements ForumManager, CleanupHook {

	private static final String KEY_RETENTION_DURATION = "retentionDuration";

	private final AuthorManager authorManager;
	private final ForumFactory forumFactory;
	private final ForumPostFactory forumPostFactory;
	private final MessageTracker messageTracker;
	private final Clock clock;
	private final List<RemoveForumHook> removeHooks;

	@Inject
	ForumManagerImpl(DatabaseComponent db, ClientHelper clientHelper,
			MetadataParser metadataParser, AuthorManager authorManager,
			ForumFactory forumFactory, ForumPostFactory forumPostFactory,
			MessageTracker messageTracker, Clock clock) {
		super(db, clientHelper, metadataParser);
		this.authorManager = authorManager;
		this.forumFactory = forumFactory;
		this.forumPostFactory = forumPostFactory;
		this.messageTracker = messageTracker;
		this.clock = clock;
		removeHooks = new CopyOnWriteArrayList<>();
	}

	@Override
	protected DeliveryAction incomingMessage(Transaction txn, Message m,
			BdfList body, BdfDictionary meta)
			throws DbException, FormatException {

		messageTracker.trackIncomingMessage(txn, m);

		// apply retention timer if set
		applyRetentionToMessage(txn, m, meta);

		ForumPostHeader header = getForumPostHeader(txn, m.getId(), meta);
		String text = getPostText(body);
		byte[] audioData = getAudioData(body);
		String audioContentType = getAudioContentType(body);
		ForumPostReceivedEvent event = new ForumPostReceivedEvent(
				m.getGroupId(), header, text, audioData, audioContentType);
		txn.attach(event);

		return ACCEPT_SHARE;
	}

	@Override
	public Forum addForum(String name) throws DbException {
		Forum f = forumFactory.createForum(name);
		db.transaction(false, txn -> db.addGroup(txn, f.getGroup()));
		return f;
	}

	@Override
	public void addForum(Transaction txn, Forum f) throws DbException {
		db.addGroup(txn, f.getGroup());
	}

	@Override
	public void removeForum(Forum f) throws DbException {
		db.transaction(false, txn -> removeForum(txn, f));
	}

	@Override
	public void removeForum(Transaction txn, Forum f) throws DbException {
		for (RemoveForumHook hook : removeHooks)
			hook.removingForum(txn, f);
		db.removeGroup(txn, f.getGroup());
	}

	@Override
	public ForumPost createLocalPost(GroupId groupId, String text,
			long timestamp, @Nullable MessageId parentId, LocalAuthor author) {
		ForumPost p;
		try {
			p = forumPostFactory.createPost(groupId, timestamp, parentId,
					author, text);
		} catch (GeneralSecurityException | FormatException e) {
			throw new AssertionError(e);
		}
		return p;
	}

	@Override
	public ForumPost createLocalAudioPost(GroupId groupId, String text,
			long timestamp, @Nullable MessageId parentId,
			LocalAuthor author, byte[] audioData, String contentType) {
		ForumPost p;
		try {
			p = forumPostFactory.createAudioPost(groupId, timestamp, parentId,
					author, text, audioData, contentType);
		} catch (GeneralSecurityException | FormatException e) {
			throw new AssertionError(e);
		}
		return p;
	}

	@Override
	public ForumPost createLocalImagePost(GroupId groupId, String text,
			long timestamp, @Nullable MessageId parentId,
			LocalAuthor author, byte[] imageData, String contentType) {
		ForumPost p;
		try {
			p = forumPostFactory.createImagePost(groupId, timestamp, parentId,
					author, text, imageData, contentType);
		} catch (GeneralSecurityException | FormatException e) {
			throw new AssertionError(e);
		}
		return p;
	}

	@Override
	public ForumPostHeader addLocalPost(ForumPost p) throws DbException {
		return db.transactionWithResult(false, txn -> addLocalPost(txn, p));
	}

	@Override
	public ForumPostHeader addLocalPost(Transaction txn, ForumPost p)
			throws DbException {
		try {
			BdfDictionary meta = new BdfDictionary();
			meta.put(KEY_TIMESTAMP, p.getMessage().getTimestamp());
			if (p.getParent() != null) meta.put(KEY_PARENT, p.getParent());
			Author a = p.getAuthor();
			meta.put(KEY_AUTHOR, clientHelper.toList(a));
			meta.put(KEY_LOCAL, true);
			meta.put(MSG_KEY_READ, true);
			// check if message body has media (6-field body)
			BdfList body = clientHelper.toList(p.getMessage());
			boolean hasAudio = false;
			boolean hasImage = false;
			if (body.size() == 6) {
				String ct = body.getString(5);
				if (ct.startsWith("audio/")) {
					hasAudio = true;
					meta.put(KEY_HAS_AUDIO, true);
				} else if (ct.startsWith("image/")) {
					hasImage = true;
					meta.put(KEY_HAS_IMAGE, true);
				}
			}
			clientHelper
					.addLocalMessage(txn, p.getMessage(), meta, true, false);
			messageTracker.trackOutgoingMessage(txn, p.getMessage());
			AuthorInfo authorInfo = authorManager.getMyAuthorInfo(txn);
			return new ForumPostHeader(p.getMessage().getId(), p.getParent(),
					p.getMessage().getTimestamp(), p.getAuthor(), authorInfo,
					true, hasAudio, hasImage);
		} catch (FormatException e) {
			throw new AssertionError(e);
		}
	}

	@Override
	public Forum getForum(GroupId g) throws DbException {
		return db.transactionWithResult(true, txn -> getForum(txn, g));
	}

	@Override
	public Forum getForum(Transaction txn, GroupId g) throws DbException {
		try {
			Group group = db.getGroup(txn, g);
			return parseForum(group);
		} catch (FormatException e) {
			throw new DbException(e);
		}
	}

	@Override
	public Collection<Forum> getForums() throws DbException {
		return db.transactionWithResult(true, this::getForums);
	}

	@Override
	public Collection<Forum> getForums(Transaction txn) throws DbException {
		Collection<Group> groups = db.getGroups(txn, CLIENT_ID, MAJOR_VERSION);
		try {
			List<Forum> forums = new ArrayList<>();
			for (Group g : groups) forums.add(parseForum(g));
			return forums;
		} catch (FormatException e) {
			throw new DbException(e);
		}
	}

	@Override
	public String getPostText(MessageId m) throws DbException {
		try {
			return getPostText(clientHelper.getMessageAsList(m));
		} catch (FormatException e) {
			throw new DbException(e);
		}
	}

	@Override
	public String getPostText(Transaction txn, MessageId m) throws DbException {
		try {
			return getPostText(clientHelper.getMessageAsList(txn, m));
		} catch (FormatException e) {
			throw new DbException(e);
		}
	}

	private String getPostText(BdfList body) throws FormatException {
		// Parent ID, author, text, signature [, audioData, contentType]
		return body.getString(2);
	}

	@Override
	@Nullable
	public byte[] getPostAudioData(MessageId m) throws DbException {
		try {
			return getAudioData(clientHelper.getMessageAsList(m));
		} catch (FormatException e) {
			throw new DbException(e);
		}
	}

	@Override
	@Nullable
	public byte[] getPostAudioData(Transaction txn, MessageId m)
			throws DbException {
		try {
			return getAudioData(clientHelper.getMessageAsList(txn, m));
		} catch (FormatException e) {
			throw new DbException(e);
		}
	}

	@Nullable
	private byte[] getAudioData(BdfList body) throws FormatException {
		if (body.size() == 6) return body.getRaw(4);
		return null;
	}

	@Override
	@Nullable
	public String getPostAudioContentType(MessageId m) throws DbException {
		try {
			return getAudioContentType(clientHelper.getMessageAsList(m));
		} catch (FormatException e) {
			throw new DbException(e);
		}
	}

	@Override
	@Nullable
	public String getPostAudioContentType(Transaction txn, MessageId m)
			throws DbException {
		try {
			return getAudioContentType(clientHelper.getMessageAsList(txn, m));
		} catch (FormatException e) {
			throw new DbException(e);
		}
	}

	@Nullable
	private String getAudioContentType(BdfList body) throws FormatException {
		if (body.size() == 6) return body.getString(5);
		return null;
	}

	@Override
	@Nullable
	public byte[] getPostImageData(MessageId m) throws DbException {
		try {
			return getImageData(clientHelper.getMessageAsList(m));
		} catch (FormatException e) {
			throw new DbException(e);
		}
	}

	@Override
	@Nullable
	public byte[] getPostImageData(Transaction txn, MessageId m)
			throws DbException {
		try {
			return getImageData(clientHelper.getMessageAsList(txn, m));
		} catch (FormatException e) {
			throw new DbException(e);
		}
	}

	@Nullable
	private byte[] getImageData(BdfList body) throws FormatException {
		if (body.size() == 6) {
			String ct = body.getString(5);
			if (ct.startsWith("image/")) return body.getRaw(4);
		}
		return null;
	}

	@Override
	@Nullable
	public String getPostImageContentType(MessageId m) throws DbException {
		try {
			return getImageContentType(clientHelper.getMessageAsList(m));
		} catch (FormatException e) {
			throw new DbException(e);
		}
	}

	@Override
	@Nullable
	public String getPostImageContentType(Transaction txn, MessageId m)
			throws DbException {
		try {
			return getImageContentType(clientHelper.getMessageAsList(txn, m));
		} catch (FormatException e) {
			throw new DbException(e);
		}
	}

	@Nullable
	private String getImageContentType(BdfList body) throws FormatException {
		if (body.size() == 6) {
			String ct = body.getString(5);
			if (ct.startsWith("image/")) return ct;
		}
		return null;
	}

	@Override
	public Collection<ForumPostHeader> getPostHeaders(GroupId g)
			throws DbException {
		return db.transactionWithResult(true, txn -> getPostHeaders(txn, g));
	}

	@Override
	public List<ForumPostHeader> getPostHeaders(Transaction txn, GroupId g)
			throws DbException {
		try {
			List<ForumPostHeader> headers = new ArrayList<>();
			Map<MessageId, BdfDictionary> metadata =
					clientHelper.getMessageMetadataAsDictionary(txn, g);
			// get all authors we need to get the info for
			Set<AuthorId> authors = new HashSet<>();
			for (Entry<MessageId, BdfDictionary> entry : metadata.entrySet()) {
				BdfList authorList = entry.getValue().getList(KEY_AUTHOR);
				Author a = clientHelper.parseAndValidateAuthor(authorList);
				authors.add(a.getId());
			}
			// get information for all authors
			Map<AuthorId, AuthorInfo> authorInfos = new HashMap<>();
			for (AuthorId id : authors) {
				authorInfos.put(id, authorManager.getAuthorInfo(txn, id));
			}
			// Parse the metadata
			for (Entry<MessageId, BdfDictionary> entry : metadata.entrySet()) {
				BdfDictionary meta = entry.getValue();
				headers.add(getForumPostHeader(txn, entry.getKey(), meta,
						authorInfos));
			}
			return headers;
		} catch (FormatException e) {
			throw new DbException(e);
		}
	}

	@Override
	public void registerRemoveForumHook(RemoveForumHook hook) {
		removeHooks.add(hook);
	}

	@Override
	public GroupCount getGroupCount(GroupId g) throws DbException {
		return messageTracker.getGroupCount(g);
	}

	@Override
	public GroupCount getGroupCount(Transaction txn, GroupId g)
			throws DbException {
		return messageTracker.getGroupCount(txn, g);
	}

	@Override
	public void setReadFlag(GroupId g, MessageId m, boolean read)
			throws DbException {
		db.transaction(false, txn ->
				messageTracker.setReadFlag(txn, g, m, read));
	}

	@Override
	public void setReadFlag(Transaction txn, GroupId g, MessageId m,
			boolean read) throws DbException {
		messageTracker.setReadFlag(txn, g, m, read);
	}

	private Forum parseForum(Group g) throws FormatException {
		byte[] descriptor = g.getDescriptor();
		// Name, salt
		BdfList forum = clientHelper.toList(descriptor);
		return new Forum(g, forum.getString(0), forum.getRaw(1));
	}

	@Override
	public long getRetentionDuration(GroupId g) throws DbException {
		try {
			return db.transactionWithResult(true, txn -> {
				BdfDictionary meta =
						clientHelper.getGroupMetadataAsDictionary(txn, g);
				return meta.getLong(KEY_RETENTION_DURATION, -1L);
			});
		} catch (FormatException e) {
			throw new DbException(e);
		}
	}

	@Override
	public void setRetentionDuration(GroupId g, long durationMs)
			throws DbException {
		db.transaction(false, txn -> {
			try {
				BdfDictionary meta = BdfDictionary.of(
						new BdfEntry(KEY_RETENTION_DURATION, durationMs));
				clientHelper.mergeGroupMetadata(txn, g, meta);
				applyRetentionToExistingMessages(txn, g, durationMs);
			} catch (FormatException e) {
				throw new DbException(e);
			}
		});
	}

	private void applyRetentionToExistingMessages(Transaction txn,
			GroupId g, long durationMs) throws DbException {
		try {
			Map<MessageId, BdfDictionary> metadata =
					clientHelper.getMessageMetadataAsDictionary(txn, g);
			long now = clock.currentTimeMillis();

			// Build parent->children map for thread coherence
			Map<MessageId, Long> deadlines = new HashMap<>();
			Map<MessageId, List<MessageId>> children = new HashMap<>();
			List<MessageId> roots = new ArrayList<>();

			for (Entry<MessageId, BdfDictionary> entry : metadata.entrySet()) {
				MessageId id = entry.getKey();
				BdfDictionary meta = entry.getValue();
				long timestamp = meta.getLong(KEY_TIMESTAMP);
				if (durationMs == -1) {
					deadlines.put(id, -1L);
				} else {
					deadlines.put(id, timestamp + durationMs);
				}
				if (meta.containsKey(KEY_PARENT)) {
					MessageId parentId =
							new MessageId(meta.getRaw(KEY_PARENT));
					children.computeIfAbsent(parentId,
							k -> new ArrayList<>()).add(id);
				} else {
					roots.add(id);
				}
			}

			// Walk tree root-first: replies never outlive their parent
			if (durationMs > 0) {
				propagateDeadlines(roots, children, deadlines);
			}

			// Apply deadlines
			for (Entry<MessageId, Long> entry : deadlines.entrySet()) {
				MessageId id = entry.getKey();
				long deadline = entry.getValue();
				if (deadline == -1) {
					db.stopCleanupTimer(txn, id);
				} else if (deadline <= now) {
					db.deleteMessage(txn, id);
					db.deleteMessageMetadata(txn, id);
				} else {
					long remaining = deadline - now;
					db.setCleanupTimerDuration(txn, id, remaining);
					db.startCleanupTimer(txn, id);
				}
			}
		} catch (FormatException e) {
			throw new DbException(e);
		}
	}

	private void propagateDeadlines(List<MessageId> nodes,
			Map<MessageId, List<MessageId>> children,
			Map<MessageId, Long> deadlines) {
		for (MessageId id : nodes) {
			List<MessageId> kids = children.get(id);
			if (kids != null) {
				long parentDeadline = deadlines.getOrDefault(id, -1L);
				for (MessageId kid : kids) {
					long kidDeadline = deadlines.getOrDefault(kid, -1L);
					if (parentDeadline > 0 && parentDeadline > kidDeadline) {
						deadlines.put(kid, parentDeadline);
					}
				}
				propagateDeadlines(kids, children, deadlines);
			}
		}
	}

	private void applyRetentionToMessage(Transaction txn, Message m,
			BdfDictionary meta) throws DbException, FormatException {
		BdfDictionary groupMeta = clientHelper
				.getGroupMetadataAsDictionary(txn, m.getGroupId());
		long retention = groupMeta.getLong(KEY_RETENTION_DURATION, -1L);
		if (retention > 0) {
			long deadline = m.getTimestamp() + retention;
			// For replies: inherit parent's deadline if later
			if (meta.containsKey(KEY_PARENT)) {
				MessageId parentId = new MessageId(meta.getRaw(KEY_PARENT));
				try {
					BdfDictionary parentMeta = clientHelper
							.getMessageMetadataAsDictionary(txn, parentId);
					long parentTimestamp = parentMeta.getLong(KEY_TIMESTAMP);
					long parentDeadline = parentTimestamp + retention;
					if (parentDeadline > deadline) {
						deadline = parentDeadline;
					}
				} catch (FormatException e) {
					// parent not found, use own deadline
				}
			}
			long duration = deadline - clock.currentTimeMillis();
			if (duration <= 0) {
				db.deleteMessage(txn, m.getId());
				db.deleteMessageMetadata(txn, m.getId());
			} else {
				db.setCleanupTimerDuration(txn, m.getId(), duration);
				db.startCleanupTimer(txn, m.getId());
			}
		}
	}

	@Override
	public void deleteMessages(Transaction txn, GroupId g,
			Collection<MessageId> messageIds) throws DbException {
		for (MessageId m : messageIds) {
			db.deleteMessage(txn, m);
			db.deleteMessageMetadata(txn, m);
		}
	}

	private ForumPostHeader getForumPostHeader(Transaction txn, MessageId id,
			BdfDictionary meta) throws DbException, FormatException {
		return getForumPostHeader(txn, id, meta, Collections.emptyMap());
	}

	private ForumPostHeader getForumPostHeader(Transaction txn, MessageId id,
			BdfDictionary meta, Map<AuthorId, AuthorInfo> authorInfos)
			throws DbException, FormatException {

		long timestamp = meta.getLong(KEY_TIMESTAMP);
		MessageId parentId = null;
		if (meta.containsKey(KEY_PARENT))
			parentId = new MessageId(meta.getRaw(KEY_PARENT));
		BdfList authorList = meta.getList(KEY_AUTHOR);
		Author author = clientHelper.parseAndValidateAuthor(authorList);
		AuthorInfo authorInfo = authorInfos.get(author.getId());
		if (authorInfo == null)
			authorInfo = authorManager.getAuthorInfo(txn, author.getId());
		boolean read = meta.getBoolean(MSG_KEY_READ);
		boolean hasAudio = meta.containsKey(KEY_HAS_AUDIO) &&
				meta.getBoolean(KEY_HAS_AUDIO);
		boolean hasImage = meta.containsKey(KEY_HAS_IMAGE) &&
				meta.getBoolean(KEY_HAS_IMAGE);

		return new ForumPostHeader(id, parentId, timestamp, author, authorInfo,
				read, hasAudio, hasImage);
	}

}

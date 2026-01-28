package org.anonomi.android.attachment;


import android.app.Application;
import android.net.Uri;

import org.anonchatsecure.bramble.api.db.DbException;
import org.anonchatsecure.bramble.api.lifecycle.IoExecutor;
import org.anonchatsecure.bramble.api.sync.GroupId;
import org.anonchatsecure.bramble.api.sync.MessageId;
import org.anonomi.R;
import org.anonomi.android.attachment.media.ImageCompressor;
import org.anonchatsecure.anonchat.api.attachment.Attachment;
import org.anonchatsecure.anonchat.api.attachment.AttachmentHeader;
import org.anonchatsecure.anonchat.api.attachment.FileTooBigException;
import org.anonchatsecure.anonchat.api.messaging.MessagingManager;
import org.briarproject.nullsafety.NotNullByDefault;

import java.util.Collections;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

import javax.inject.Inject;

import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import static java.util.logging.Level.WARNING;
import static java.util.logging.Logger.getLogger;
import static org.anonchatsecure.bramble.util.LogUtils.logException;
import static org.anonomi.android.attachment.AttachmentItem.State.ERROR;
import static org.anonomi.android.util.UiUtils.observeForeverOnce;
import static org.anonchatsecure.anonchat.api.attachment.MediaConstants.MAX_IMAGE_SIZE;

@NotNullByDefault
class AttachmentCreatorImpl implements AttachmentCreator {

	private static final Logger LOG =
			getLogger(AttachmentCreatorImpl.class.getName());

	private final Application app;
	@IoExecutor
	private final Executor ioExecutor;
	private final MessagingManager messagingManager;
	private final AttachmentRetriever retriever;
	private final ImageCompressor imageCompressor;

	private final CopyOnWriteArrayList<Uri> uris = new CopyOnWriteArrayList<>();
	private final CopyOnWriteArrayList<AttachmentItemResult> itemResults =
			new CopyOnWriteArrayList<>();

	@Nullable
	private AttachmentCreationTask task;

	@Nullable
	private volatile MutableLiveData<AttachmentResult> result;

	@Inject
	AttachmentCreatorImpl(Application app, @IoExecutor Executor ioExecutor,
			MessagingManager messagingManager, AttachmentRetriever retriever,
			ImageCompressor imageCompressor) {
		this.app = app;
		this.ioExecutor = ioExecutor;
		this.messagingManager = messagingManager;
		this.retriever = retriever;
		this.imageCompressor = imageCompressor;
	}

	@Override
	@UiThread
	public LiveData<AttachmentResult> storeAttachments(
			LiveData<GroupId> groupId, Collection<Uri> newUris) {
		if (task != null || result != null || !uris.isEmpty()) {
			// if (task != null) LOG.warning("Task already exists!");
			// if (result != null) LOG.warning("Result already exists!");
			// if (!uris.isEmpty()) LOG.warning("Uris available: " + uris);
			throw new IllegalStateException();
		}
		MutableLiveData<AttachmentResult> result = new MutableLiveData<>();
		this.result = result;
		uris.addAll(newUris);
		observeForeverOnce(groupId, id -> {
			if (id == null) throw new IllegalStateException();
			boolean needsSize = uris.size() == 1;
			task = new AttachmentCreationTask(messagingManager,
					app.getContentResolver(), this, imageCompressor, id,
					uris, needsSize);
			ioExecutor.execute(() -> task.storeAttachments());
		});
		return result;
	}

	@Override
	@UiThread
	public LiveData<AttachmentResult> getLiveAttachments() {
		MutableLiveData<AttachmentResult> result = this.result;
		if (task == null || result == null || uris.isEmpty()) {
			// if (task == null) LOG.warning("No Task!");
			// if (result == null) LOG.warning("No Result!");
			// if (uris.isEmpty()) LOG.warning("Uris empty!");
			throw new IllegalStateException();
		}
		// A task is already running. It will update the result LiveData.
		// So nothing more to do here.
		return result;
	}

	@Override
	@IoExecutor
	public void onAttachmentHeaderReceived(Uri uri, AttachmentHeader h, boolean needsSize) {
		try {
			AttachmentItem item;

			try {
				Attachment a = retriever.getMessageAttachment(h);
				item = retriever.createAttachmentItem(a, needsSize);
				if (item.getState() == ERROR) throw new IOException();
			} catch (UnsupportedOperationException | IOException e) {
				// Fallback for non-image attachments like audio
				item = new AttachmentItem(h, h.getContentType(), ERROR);
			}

			AttachmentItemResult itemResult = new AttachmentItemResult(uri, item);
			itemResults.add(itemResult);
			MutableLiveData<AttachmentResult> result = this.result;
			if (result != null) result.postValue(getResult(false));

		} catch (DbException e) {
			logException(LOG, WARNING, e);
			onAttachmentError(uri, e);
		}
	}

	@Override
	@IoExecutor
	public void onAttachmentError(Uri uri, Throwable t) {
		// get error message
		String errorMsg;
		if (t instanceof UnsupportedMimeTypeException) {
			String mimeType = ((UnsupportedMimeTypeException) t).getMimeType();
			errorMsg = app.getString(
					R.string.image_attach_error_invalid_mime_type, mimeType);
		} else if (t instanceof FileTooBigException) {
			int mb = MAX_IMAGE_SIZE / 1024 / 1024;
			errorMsg = app.getString(R.string.image_attach_error_too_big, mb);
		} else {
			errorMsg = null; // generic error
		}
		AttachmentItemResult itemResult =
				new AttachmentItemResult(uri, errorMsg);
		itemResults.add(itemResult);
		MutableLiveData<AttachmentResult> result = this.result;
		if (result != null) result.postValue(getResult(false));
		// expect to receive a cancel from the UI
	}

	@Override
	@IoExecutor
	public void onAttachmentCreationFinished() {
		MutableLiveData<AttachmentResult> result = this.result;
		if (result != null) result.postValue(getResult(true));
	}

	@Override
	@UiThread
	public List<AttachmentHeader> getAttachmentHeadersForSending() {
		List<AttachmentHeader> headers = new ArrayList<>(itemResults.size());
		for (AttachmentItemResult itemResult : itemResults) {
			// check if we are trying to send attachment items with errors
			if (itemResult.getItem() == null) throw new IllegalStateException();
			headers.add(itemResult.getItem().getHeader());
		}
		return headers;
	}

	@Override
	@UiThread
	public void onAttachmentsSent(MessageId id) {
		resetState();
	}

	@Override
	@UiThread
	public void cancel() {
		if (task != null) task.cancel();
		deleteUnsentAttachments();
		resetState();
	}

	@UiThread
	private void resetState() {
		task = null;
		uris.clear();
		itemResults.clear();
		MutableLiveData<AttachmentResult> result = this.result;
		if (result != null) {
			result.setValue(null);
			this.result = null;
		}
	}

	@Override
	@UiThread
	public void deleteUnsentAttachments() {
		// Make a copy for the IoExecutor as we clear the itemResults soon
		List<AttachmentHeader> headers = new ArrayList<>(itemResults.size());
		for (AttachmentItemResult itemResult : itemResults) {
			// check if we are trying to send attachment items with errors
			if (itemResult.getItem() != null)
				headers.add(itemResult.getItem().getHeader());
		}
		ioExecutor.execute(() -> {
			for (AttachmentHeader header : headers) {
				try {
					messagingManager.removeAttachment(header);
				} catch (DbException e) {
					logException(LOG, WARNING, e);
				}
			}
		});
	}

	@Override
	@UiThread
	public LiveData<AttachmentResult> storeSingleAttachment(LiveData<GroupId> groupId, Uri uri) {
		if (task != null || result != null || !uris.isEmpty()) {
			throw new IllegalStateException("Attachment task already running or not cleaned up");
		}

		MutableLiveData<AttachmentResult> liveResult = new MutableLiveData<>();
		this.result = liveResult;
		this.uris.add(uri);

		observeForeverOnce(groupId, id -> {
			if (id == null) {
				liveResult.setValue(new AttachmentResult(Collections.emptyList(), true));
				return;
			}
			boolean needsSize = true;
			task = new AttachmentCreationTask(
					messagingManager,
					app.getContentResolver(),
					this,
					imageCompressor,
					id,
					Collections.singletonList(uri),
					needsSize
			);
			ioExecutor.execute(() -> task.storeAttachments());
		});

		return liveResult;
	}
	private AttachmentResult getResult(boolean finished) {
		// Make a copy of the list,
		// because our copy will continue to change in the background.
		// (As it's a CopyOnWriteArrayList,
		//  the code that receives the result can safely do simple things
		//  like iterating over the list,
		//  but anything that involves calling more than one list method
		//  is still unsafe.)
		Collection<AttachmentItemResult> items = new ArrayList<>(itemResults);
		return new AttachmentResult(items, finished);
	}

}

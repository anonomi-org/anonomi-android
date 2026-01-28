package org.anonomi.android.attachment;

import android.content.ContentResolver;
import android.net.Uri;

import android.content.res.AssetFileDescriptor;
import java.io.BufferedInputStream;
import org.anonchatsecure.anonchat.api.attachment.FileTooBigException;

import org.anonchatsecure.bramble.api.db.DbException;
import org.anonchatsecure.bramble.api.lifecycle.IoExecutor;
import org.anonchatsecure.bramble.api.sync.GroupId;
import org.anonomi.android.attachment.media.ImageCompressor;
import org.anonchatsecure.anonchat.api.attachment.AttachmentHeader;
import org.anonchatsecure.anonchat.api.messaging.MessagingManager;
import org.briarproject.nullsafety.NotNullByDefault;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.logging.Logger;
import java.util.Arrays;

import androidx.annotation.Nullable;

import static java.util.logging.Level.WARNING;
import static java.util.logging.Logger.getLogger;
import static org.anonchatsecure.bramble.util.AndroidUtils.getSupportedImageContentTypes;
import static org.anonchatsecure.bramble.util.IoUtils.tryToClose;
import static org.anonchatsecure.bramble.util.LogUtils.logDuration;
import static org.anonchatsecure.bramble.util.LogUtils.logException;
import static org.anonchatsecure.bramble.util.LogUtils.now;
import static org.anonomi.android.attachment.media.ImageCompressor.MIME_TYPE;

@NotNullByDefault
class AttachmentCreationTask {

	private static final Logger LOG =
			getLogger(AttachmentCreationTask.class.getName());

	private boolean isSupportedContentType(String contentType) {
		return Arrays.asList(getSupportedImageContentTypes()).contains(contentType)
				|| Arrays.asList(getSupportedAudioContentTypes()).contains(contentType);
	}

	private static String[] getSupportedAudioContentTypes() {
		return new String[] {
				"image/*",
				"audio/ogg",
				"audio/m4a",
				"audio/mp4",
				"audio/x-m4a",
				"audio/x-wav",
				"audio/wav",
				"audio/webm"
		};
	}

	private final MessagingManager messagingManager;
	private final ContentResolver contentResolver;
	private final ImageCompressor imageCompressor;
	private final GroupId groupId;
	private final Collection<Uri> uris;
	private final boolean needsSize;
	@Nullable
	private volatile AttachmentCreator attachmentCreator;

	private volatile boolean canceled = false;

	AttachmentCreationTask(MessagingManager messagingManager,
			ContentResolver contentResolver,
			AttachmentCreator attachmentCreator,
			ImageCompressor imageCompressor,
			GroupId groupId, Collection<Uri> uris, boolean needsSize) {
		this.messagingManager = messagingManager;
		this.contentResolver = contentResolver;
		this.imageCompressor = imageCompressor;
		this.groupId = groupId;
		this.uris = uris;
		this.needsSize = needsSize;
		this.attachmentCreator = attachmentCreator;
	}

	void cancel() {
		canceled = true;
		attachmentCreator = null;
	}

	@IoExecutor
	void storeAttachments() {
		for (Uri uri : uris) processUri(uri);
		AttachmentCreator attachmentCreator = this.attachmentCreator;
		if (!canceled && attachmentCreator != null)
			attachmentCreator.onAttachmentCreationFinished();
		this.attachmentCreator = null;
	}

	@IoExecutor
	private void processUri(Uri uri) {
		if (canceled) return;
		try {
			AttachmentHeader h = storeAttachment(uri);
			AttachmentCreator attachmentCreator = this.attachmentCreator;
			if (attachmentCreator != null) {
				attachmentCreator.onAttachmentHeaderReceived(uri, h, needsSize);
			}
		} catch (DbException | IOException e) {
			logException(LOG, WARNING, e);
			AttachmentCreator attachmentCreator = this.attachmentCreator;
			if (attachmentCreator != null) {
				attachmentCreator.onAttachmentError(uri, e);
			}
			canceled = true;
		}
	}

	@IoExecutor
	private AttachmentHeader storeAttachment(Uri uri)
			throws IOException, DbException {

		long start = now();
		String contentType = detectMimeType(uri);
		if (contentType == null) throw new IOException("Unable to detect MIME type");
		if (!isSupportedContentType(contentType)) {
			throw new UnsupportedMimeTypeException(contentType, uri);
		}

		AssetFileDescriptor afd = contentResolver.openAssetFileDescriptor(uri, "r");
		if (afd == null) throw new IOException("Cannot open file descriptor");

		long fileSize = afd.getLength();
		final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB - adjust if needed

		if (fileSize > MAX_FILE_SIZE) {
			throw new FileTooBigException();
		}

		InputStream is = new BufferedInputStream(afd.createInputStream());

		// Only compress images
		if (contentType.startsWith("image/")) {
			is = imageCompressor.compressImage(is, contentType);
			contentType = MIME_TYPE; // Always "image/jpeg" after compression
		}

		long timestamp = System.currentTimeMillis();
		AttachmentHeader h = messagingManager.addLocalAttachment(
				groupId, timestamp, contentType, is);

		tryToClose(is, LOG, WARNING);
		logDuration(LOG, "Storing attachment", start);
		return h;
	}

	private @Nullable String detectMimeType(Uri uri) {
		String fallback = contentResolver.getType(uri);

		// Try extension-based detection if scheme is "file" or "content"
		String path = uri.getPath();
		if (path != null) {
			if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
			if (path.endsWith(".png")) return "image/png";
			if (path.endsWith(".webp")) return "image/webp";
			if (path.endsWith(".gif")) return "image/gif";
			if (path.endsWith(".m4a")) return "audio/mp4";
			if (path.endsWith(".mp4")) return "audio/mp4";
			if (path.endsWith(".ogg")) return "audio/ogg";
			if (path.endsWith(".wav")) return "audio/wav";
		}

		return fallback;
	}

}

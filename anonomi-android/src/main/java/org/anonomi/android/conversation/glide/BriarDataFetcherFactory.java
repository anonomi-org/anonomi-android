package org.anonomi.android.conversation.glide;

import org.anonchatsecure.bramble.api.db.DatabaseExecutor;
import org.anonchatsecure.anonchat.api.attachment.AttachmentHeader;
import org.anonchatsecure.anonchat.api.attachment.AttachmentReader;
import org.briarproject.nullsafety.NotNullByDefault;

import java.util.concurrent.Executor;

import javax.inject.Inject;

@NotNullByDefault
public class BriarDataFetcherFactory {

	private final AttachmentReader attachmentReader;
	@DatabaseExecutor
	private final Executor dbExecutor;

	@Inject
	public BriarDataFetcherFactory(AttachmentReader attachmentReader,
			@DatabaseExecutor Executor dbExecutor) {
		this.attachmentReader = attachmentReader;
		this.dbExecutor = dbExecutor;
	}

	BriarDataFetcher createBriarDataFetcher(AttachmentHeader model) {
		return new BriarDataFetcher(attachmentReader, dbExecutor, model);
	}

}

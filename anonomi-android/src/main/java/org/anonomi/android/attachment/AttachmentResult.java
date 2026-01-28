package org.anonomi.android.attachment;

import org.briarproject.nullsafety.NotNullByDefault;

import java.util.Collection;

import javax.annotation.concurrent.Immutable;

import java.util.List;
import java.util.ArrayList;
import org.anonchatsecure.anonchat.api.attachment.AttachmentHeader;

@Immutable
@NotNullByDefault
public class AttachmentResult {

	private final Collection<AttachmentItemResult> itemResults;
	private final boolean finished;

	public AttachmentResult(Collection<AttachmentItemResult> itemResults,
			boolean finished) {
		this.itemResults = itemResults;
		this.finished = finished;
	}

	public Collection<AttachmentItemResult> getItemResults() {
		return itemResults;
	}

	public List<AttachmentHeader> getAttachmentHeaders() {
		List<AttachmentHeader> headers = new ArrayList<>();
		for (AttachmentItemResult result : itemResults) {
			if (result.getItem() != null) {
				headers.add(result.getItem().getHeader());
			}
		}
		return headers;
	}

	public boolean isFinished() {
		return finished;
	}

}

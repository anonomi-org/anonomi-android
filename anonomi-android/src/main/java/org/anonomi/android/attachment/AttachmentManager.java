package org.anonomi.android.attachment;

import android.net.Uri;

import org.anonchatsecure.anonchat.api.attachment.AttachmentHeader;
import org.briarproject.nullsafety.NotNullByDefault;

import java.util.Collection;
import java.util.List;

import androidx.annotation.UiThread;
import androidx.lifecycle.LiveData;

@UiThread
@NotNullByDefault
public interface AttachmentManager {

	LiveData<AttachmentResult> storeAttachments(Collection<Uri> uri,
			boolean restart);

	List<AttachmentHeader> getAttachmentHeadersForSending();

	void cancel();

}

package org.anonchatsecure.bramble.api.qrcode;

import org.anonchatsecure.bramble.api.Pair;
import org.briarproject.nullsafety.NotNullByDefault;

@NotNullByDefault
public interface QrCodeClassifier {

	enum QrCodeType {
		BQP,
		MAILBOX,
		UNKNOWN
	}

	Pair<QrCodeType, Integer> classifyQrCode(String payload);
}

package org.anonchatsecure.bramble.qrcode;

import org.anonchatsecure.bramble.api.qrcode.QrCodeClassifier;

import dagger.Module;
import dagger.Provides;

@Module
public class QrCodeModule {

	@Provides
	QrCodeClassifier provideQrCodeClassifier(
			QrCodeClassifierImpl qrCodeClassifier) {
		return qrCodeClassifier;
	}
}

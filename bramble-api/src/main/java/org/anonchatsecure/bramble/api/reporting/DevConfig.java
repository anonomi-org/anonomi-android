package org.anonchatsecure.bramble.api.reporting;

import org.anonchatsecure.bramble.api.crypto.PublicKey;
import org.briarproject.nullsafety.NotNullByDefault;

import java.io.File;

@NotNullByDefault
public interface DevConfig {

	PublicKey getDevPublicKey();

	String getDevOnionAddress();

	File getReportDir();

	File getLogcatFile();
}

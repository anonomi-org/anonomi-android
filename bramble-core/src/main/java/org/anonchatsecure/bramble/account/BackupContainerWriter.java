package org.anonchatsecure.bramble.account;

import org.anonchatsecure.bramble.api.account.BackupManifest;
import org.anonchatsecure.bramble.api.account.BackupProgressListener;
import org.anonchatsecure.bramble.api.crypto.CryptoComponent;
import org.anonchatsecure.bramble.api.crypto.SecretKey;
import org.anonchatsecure.bramble.api.data.BdfDictionary;
import org.anonchatsecure.bramble.api.data.BdfWriter;
import org.anonchatsecure.bramble.api.data.BdfWriterFactory;
import org.anonchatsecure.bramble.api.transport.StreamWriter;
import org.anonchatsecure.bramble.api.transport.StreamWriterFactory;
import org.anonchatsecure.bramble.util.ByteUtils;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.briarproject.nullsafety.NotNullByDefault;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import static org.anonchatsecure.bramble.api.account.BackupConstants.BACKUP_FORMAT_VERSION;
import static org.anonchatsecure.bramble.api.account.BackupConstants.BACKUP_LOG_COST;
import static org.anonchatsecure.bramble.api.account.BackupConstants.BACKUP_PROGRESS_INTERVAL_BYTES;
import static org.anonchatsecure.bramble.api.account.BackupConstants.MANIFEST_APP_VERSION_CODE;
import static org.anonchatsecure.bramble.api.account.BackupConstants.MANIFEST_APP_VERSION_NAME;
import static org.anonchatsecure.bramble.api.account.BackupConstants.MANIFEST_CREATED;
import static org.anonchatsecure.bramble.api.account.BackupConstants.MANIFEST_DB_FILE_NAME;
import static org.anonchatsecure.bramble.api.account.BackupConstants.MANIFEST_DB_KEY;
import static org.anonchatsecure.bramble.api.account.BackupConstants.MANIFEST_DB_LENGTH;
import static org.anonchatsecure.bramble.api.account.BackupConstants.MANIFEST_DB_SHA_256;
import static org.anonchatsecure.bramble.api.account.BackupConstants.MANIFEST_FORMAT_VERSION;
import static org.anonchatsecure.bramble.api.account.BackupConstants.MANIFEST_HEADER;
import static org.anonchatsecure.bramble.api.account.BackupConstants.MANIFEST_SCHEMA_VERSION;
import static org.anonchatsecure.bramble.api.account.BackupConstants.MAX_MANIFEST_BYTES;
import static org.anonchatsecure.bramble.util.ByteUtils.INT_32_BYTES;
import static org.anonchatsecure.bramble.util.ByteUtils.INT_64_BYTES;

/**
 * Writes the backup container: a plaintext header, then a manifest and the
 * database file inside a stream encrypted under a key derived from the
 * recovery code.
 */
@NotNullByDefault
class BackupContainerWriter {

	private final CryptoComponent crypto;
	private final StreamWriterFactory streamWriterFactory;
	private final BdfWriterFactory bdfWriterFactory;
	private final int logCost;

	BackupContainerWriter(CryptoComponent crypto,
			StreamWriterFactory streamWriterFactory,
			BdfWriterFactory bdfWriterFactory) {
		this(crypto, streamWriterFactory, bdfWriterFactory, BACKUP_LOG_COST);
	}

	// Package access so tests can use a cheaper key derivation
	BackupContainerWriter(CryptoComponent crypto,
			StreamWriterFactory streamWriterFactory,
			BdfWriterFactory bdfWriterFactory, int logCost) {
		this.crypto = crypto;
		this.streamWriterFactory = streamWriterFactory;
		this.bdfWriterFactory = bdfWriterFactory;
		this.logCost = logCost;
	}

	/**
	 * Writes a backup to the given stream, taking the database bytes from
	 * the given stream. Neither stream is closed.
	 *
	 * @throws IOException if the database bytes do not match the length and
	 * hash recorded in the manifest, as well as for the usual reasons
	 */
	void write(OutputStream out, String recoveryCode, BackupManifest m,
			InputStream dbIn, BackupProgressListener listener)
			throws IOException {
		BackupHeader header =
				BackupHeader.create(crypto.getSecureRandom(), logCost);
		byte[] headerBytes = header.getBytes();
		out.write(headerBytes);
		SecretKey headerKey = header.deriveKey(crypto, recoveryCode);
		StreamWriter streamWriter =
				streamWriterFactory.createLogStreamWriter(out, headerKey);
		OutputStream encrypted = streamWriter.getOutputStream();
		byte[] manifest = encodeManifest(m, headerBytes);
		if (manifest.length > MAX_MANIFEST_BYTES) throw new IOException();
		byte[] manifestLength = new byte[INT_32_BYTES];
		ByteUtils.writeUint32(manifest.length, manifestLength, 0);
		encrypted.write(manifestLength);
		encrypted.write(manifest);
		byte[] dbLength = new byte[INT_64_BYTES];
		ByteUtils.writeUint64(m.getDbLength(), dbLength, 0);
		encrypted.write(dbLength);
		copyDatabase(dbIn, encrypted, m, listener);
		streamWriter.sendEndOfStream();
	}

	private byte[] encodeManifest(BackupManifest m, byte[] header)
			throws IOException {
		BdfDictionary d = new BdfDictionary();
		d.put(MANIFEST_FORMAT_VERSION, (long) BACKUP_FORMAT_VERSION);
		d.put(MANIFEST_APP_VERSION_CODE, m.getAppVersionCode());
		d.put(MANIFEST_APP_VERSION_NAME, m.getAppVersionName());
		d.put(MANIFEST_SCHEMA_VERSION, (long) m.getSchemaVersion());
		d.put(MANIFEST_CREATED, m.getCreated());
		d.put(MANIFEST_DB_KEY, m.getDbKey().getBytes());
		d.put(MANIFEST_DB_FILE_NAME, m.getDbFileName());
		d.put(MANIFEST_DB_LENGTH, m.getDbLength());
		d.put(MANIFEST_DB_SHA_256, m.getDbSha256());
		d.put(MANIFEST_HEADER, header);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		BdfWriter w = bdfWriterFactory.createWriter(out);
		w.writeDictionary(d);
		w.flush();
		return out.toByteArray();
	}

	/**
	 * Copies the database and checks it against the manifest as it goes. A
	 * backup that does not describe its own contents would fail on restore,
	 * where the file may be the only copy left.
	 */
	private void copyDatabase(InputStream in, OutputStream out,
			BackupManifest m, BackupProgressListener listener)
			throws IOException {
		SHA256Digest digest = new SHA256Digest();
		byte[] buf = new byte[4096];
		long total = m.getDbLength(), done = 0, reported = 0;
		listener.onBackupProgress(0, total);
		while (done < total) {
			int len = (int) Math.min(buf.length, total - done);
			int read = in.read(buf, 0, len);
			if (read == -1) throw new IOException();
			out.write(buf, 0, read);
			digest.update(buf, 0, read);
			done += read;
			if (done - reported >= BACKUP_PROGRESS_INTERVAL_BYTES) {
				listener.onBackupProgress(done, total);
				reported = done;
			}
		}
		listener.onBackupProgress(done, total);
		if (in.read() != -1) throw new IOException();
		byte[] hash = new byte[digest.getDigestSize()];
		digest.doFinal(hash, 0);
		if (!Arrays.equals(hash, m.getDbSha256())) throw new IOException();
	}
}

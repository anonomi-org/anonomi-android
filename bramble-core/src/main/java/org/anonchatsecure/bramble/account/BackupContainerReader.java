package org.anonchatsecure.bramble.account;

import org.anonchatsecure.bramble.api.FormatException;
import org.anonchatsecure.bramble.api.account.BackupError;
import org.anonchatsecure.bramble.api.account.BackupManifest;
import org.anonchatsecure.bramble.api.account.BackupProgressListener;
import org.anonchatsecure.bramble.api.account.InvalidBackupException;
import org.anonchatsecure.bramble.api.crypto.CryptoComponent;
import org.anonchatsecure.bramble.api.crypto.SecretKey;
import org.anonchatsecure.bramble.api.data.BdfDictionary;
import org.anonchatsecure.bramble.api.data.BdfReader;
import org.anonchatsecure.bramble.api.data.BdfReaderFactory;
import org.anonchatsecure.bramble.api.transport.StreamReaderFactory;
import org.anonchatsecure.bramble.util.ByteUtils;
import org.anonchatsecure.bramble.util.IoUtils;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.briarproject.nullsafety.NotNullByDefault;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import static org.anonchatsecure.bramble.api.account.BackupConstants.BACKUP_DB_FILE_NAME;
import static org.anonchatsecure.bramble.api.account.BackupConstants.BACKUP_FORMAT_VERSION;
import static org.anonchatsecure.bramble.api.account.BackupConstants.BACKUP_HEADER_BYTES;
import static org.anonchatsecure.bramble.api.account.BackupConstants.BACKUP_PROGRESS_INTERVAL_BYTES;
import static org.anonchatsecure.bramble.api.account.BackupConstants.DB_SHA_256_BYTES;
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
import static org.anonchatsecure.bramble.api.account.BackupError.CORRUPT;
import static org.anonchatsecure.bramble.api.account.BackupError.DATA_TOO_NEW;
import static org.anonchatsecure.bramble.api.account.BackupError.DATA_TOO_OLD;
import static org.anonchatsecure.bramble.api.account.BackupError.NOT_A_BACKUP;
import static org.anonchatsecure.bramble.api.account.BackupError.TRUNCATED;
import static org.anonchatsecure.bramble.api.account.BackupError.WRONG_CODE_OR_DAMAGED;
import static org.anonchatsecure.bramble.api.db.DatabaseSchema.CODE_SCHEMA_VERSION;
import static org.anonchatsecure.bramble.api.db.DatabaseSchema.MIN_SCHEMA_VERSION;
import static org.anonchatsecure.bramble.api.transport.TransportConstants.STREAM_HEADER_LENGTH;
import static org.anonchatsecure.bramble.util.ByteUtils.INT_32_BYTES;
import static org.anonchatsecure.bramble.util.ByteUtils.INT_64_BYTES;

/**
 * Reads a backup container written by {@link BackupContainerWriter}. Checks
 * that cost nothing come first, and the schema version is checked before any
 * of the database is written out.
 */
@NotNullByDefault
class BackupContainerReader {

	private final CryptoComponent crypto;
	private final StreamReaderFactory streamReaderFactory;
	private final BdfReaderFactory bdfReaderFactory;

	BackupContainerReader(CryptoComponent crypto,
			StreamReaderFactory streamReaderFactory,
			BdfReaderFactory bdfReaderFactory) {
		this.crypto = crypto;
		this.streamReaderFactory = streamReaderFactory;
		this.bdfReaderFactory = bdfReaderFactory;
	}

	/**
	 * Reads a backup from the given stream, writing the database to the given
	 * stream, and returns the manifest. Neither stream is closed.
	 */
	BackupManifest read(InputStream in, String recoveryCode, OutputStream dbOut,
			BackupProgressListener listener)
			throws IOException, InvalidBackupException {
		byte[] headerBytes = new byte[BACKUP_HEADER_BYTES];
		try {
			IoUtils.read(in, headerBytes);
		} catch (EOFException e) {
			throw new InvalidBackupException(NOT_A_BACKUP);
		}
		BackupHeader header = BackupHeader.parse(headerBytes);
		SecretKey headerKey = header.deriveKey(crypto, recoveryCode);
		CountingInputStream counted = new CountingInputStream(in);
		InputStream encrypted =
				streamReaderFactory.createLogStreamReader(counted, headerKey);
		byte[] manifestLength = new byte[INT_32_BYTES];
		try {
			IoUtils.read(encrypted, manifestLength);
		} catch (FormatException e) {
			throw new InvalidBackupException(errorFor(counted));
		} catch (EOFException e) {
			throw new InvalidBackupException(TRUNCATED);
		}
		long length = ByteUtils.readUint32(manifestLength, 0);
		if (length < 1 || length > MAX_MANIFEST_BYTES) {
			throw new InvalidBackupException(CORRUPT);
		}
		BdfDictionary manifest;
		long dbLength;
		try {
			byte[] manifestBytes = new byte[(int) length];
			IoUtils.read(encrypted, manifestBytes);
			manifest = parseManifest(manifestBytes);
			byte[] dbLengthBytes = new byte[INT_64_BYTES];
			IoUtils.read(encrypted, dbLengthBytes);
			dbLength = ByteUtils.readUint64(dbLengthBytes, 0);
		} catch (EOFException e) {
			throw new InvalidBackupException(TRUNCATED);
		} catch (FormatException e) {
			throw new InvalidBackupException(errorFor(counted));
		}
		BackupManifest m = checkManifest(manifest, headerBytes, dbLength);
		try {
			copyDatabase(encrypted, dbOut, m, listener);
		} catch (EOFException e) {
			throw new InvalidBackupException(TRUNCATED);
		} catch (FormatException e) {
			throw new InvalidBackupException(errorFor(counted));
		}
		return m;
	}

	/**
	 * A wrong recovery code always fails while authenticating the stream
	 * header, which is the first thing read; anything that fails later is
	 * damage, whatever code was entered.
	 */
	private BackupError errorFor(CountingInputStream counted) {
		return counted.getCount() <= STREAM_HEADER_LENGTH
				? WRONG_CODE_OR_DAMAGED : CORRUPT;
	}

	private BdfDictionary parseManifest(byte[] b) throws IOException {
		BdfReader r = bdfReaderFactory.createReader(new ByteArrayInputStream(b));
		BdfDictionary d = r.readDictionary();
		if (!r.eof()) throw new FormatException();
		return d;
	}

	private BackupManifest checkManifest(BdfDictionary d, byte[] headerBytes,
			long dbLength) throws InvalidBackupException {
		try {
			if (d.getLong(MANIFEST_FORMAT_VERSION) != BACKUP_FORMAT_VERSION) {
				throw new InvalidBackupException(CORRUPT);
			}
			if (!Arrays.equals(d.getRaw(MANIFEST_HEADER), headerBytes)) {
				throw new InvalidBackupException(CORRUPT);
			}
			// Refuse a database this code cannot open before writing any of it
			long schemaVersion = d.getLong(MANIFEST_SCHEMA_VERSION);
			if (schemaVersion > CODE_SCHEMA_VERSION) {
				throw new InvalidBackupException(DATA_TOO_NEW);
			}
			if (schemaVersion < MIN_SCHEMA_VERSION) {
				throw new InvalidBackupException(DATA_TOO_OLD);
			}
			if (dbLength < 0 || d.getLong(MANIFEST_DB_LENGTH) != dbLength) {
				throw new InvalidBackupException(CORRUPT);
			}
			byte[] dbKey = d.getRaw(MANIFEST_DB_KEY);
			if (dbKey.length != SecretKey.LENGTH) {
				throw new InvalidBackupException(CORRUPT);
			}
			byte[] dbSha256 = d.getRaw(MANIFEST_DB_SHA_256);
			if (dbSha256.length != DB_SHA_256_BYTES) {
				throw new InvalidBackupException(CORRUPT);
			}
			// The caller uses this as a file name, so accept only the one
			// name this format writes
			String dbFileName = d.getString(MANIFEST_DB_FILE_NAME);
			if (!BACKUP_DB_FILE_NAME.equals(dbFileName)) {
				throw new InvalidBackupException(CORRUPT);
			}
			return new BackupManifest(BACKUP_FORMAT_VERSION,
					d.getLong(MANIFEST_APP_VERSION_CODE),
					d.getString(MANIFEST_APP_VERSION_NAME),
					(int) schemaVersion, d.getLong(MANIFEST_CREATED),
					new SecretKey(dbKey), dbFileName, dbLength, dbSha256);
		} catch (FormatException e) {
			throw new InvalidBackupException(CORRUPT);
		}
	}

	private void copyDatabase(InputStream in, OutputStream out,
			BackupManifest m, BackupProgressListener listener)
			throws IOException, InvalidBackupException {
		SHA256Digest digest = new SHA256Digest();
		byte[] buf = new byte[4096];
		long total = m.getDbLength(), done = 0, reported = 0;
		listener.onBackupProgress(0, total);
		while (done < total) {
			int len = (int) Math.min(buf.length, total - done);
			int read = in.read(buf, 0, len);
			if (read == -1) throw new EOFException();
			out.write(buf, 0, read);
			digest.update(buf, 0, read);
			done += read;
			if (done - reported >= BACKUP_PROGRESS_INTERVAL_BYTES) {
				listener.onBackupProgress(done, total);
				reported = done;
			}
		}
		listener.onBackupProgress(done, total);
		// Reading on past the database reaches the authenticated final frame,
		// so a file cut short after the last whole frame is still caught
		if (in.read() != -1) throw new InvalidBackupException(CORRUPT);
		byte[] hash = new byte[digest.getDigestSize()];
		digest.doFinal(hash, 0);
		if (!Arrays.equals(hash, m.getDbSha256())) {
			throw new InvalidBackupException(CORRUPT);
		}
	}

	private static class CountingInputStream extends FilterInputStream {

		private long count = 0;

		private CountingInputStream(InputStream in) {
			super(in);
		}

		private long getCount() {
			return count;
		}

		@Override
		public int read() throws IOException {
			int b = in.read();
			if (b != -1) count++;
			return b;
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			int read = in.read(b, off, len);
			if (read != -1) count += read;
			return read;
		}
	}
}

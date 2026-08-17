package org.anonchatsecure.bramble.account;

import org.anonchatsecure.bramble.api.account.AccountBackupConfig;
import org.anonchatsecure.bramble.api.account.AccountBackupManager;
import org.anonchatsecure.bramble.api.account.AccountManager;
import org.anonchatsecure.bramble.api.account.BackupManifest;
import org.anonchatsecure.bramble.api.account.BackupProgressListener;
import org.anonchatsecure.bramble.api.account.InvalidBackupException;
import org.anonchatsecure.bramble.api.account.NotEnoughSpaceException;
import org.anonchatsecure.bramble.api.account.RecoveryCode;
import org.anonchatsecure.bramble.api.crypto.CryptoComponent;
import org.anonchatsecure.bramble.api.crypto.SecretKey;
import org.anonchatsecure.bramble.api.data.BdfReaderFactory;
import org.anonchatsecure.bramble.api.data.BdfWriterFactory;
import org.anonchatsecure.bramble.api.db.DatabaseComponent;
import org.anonchatsecure.bramble.api.db.DatabaseConfig;
import org.anonchatsecure.bramble.api.db.DbException;
import org.anonchatsecure.bramble.api.system.Clock;
import org.anonchatsecure.bramble.api.transport.StreamReaderFactory;
import org.anonchatsecure.bramble.api.transport.StreamWriterFactory;
import org.anonchatsecure.bramble.util.IoUtils;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.briarproject.nullsafety.NotNullByDefault;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.inject.Inject;

import static java.util.logging.Level.WARNING;
import static java.util.logging.Logger.getLogger;
import static org.anonchatsecure.bramble.api.account.BackupConstants.BACKUP_DB_FILE_NAME;
import static org.anonchatsecure.bramble.api.account.BackupConstants.BACKUP_FORMAT_VERSION;
import static org.anonchatsecure.bramble.api.db.DatabaseSchema.CODE_SCHEMA_VERSION;
import static org.anonchatsecure.bramble.util.IoUtils.tryToClose;

@NotNullByDefault
class AccountBackupManagerImpl implements AccountBackupManager {

	private static final Logger LOG =
			getLogger(AccountBackupManagerImpl.class.getName());

	private static final String TEMP_ZIP_NAME = "db.zip";

	private final DatabaseComponent db;
	private final DatabaseConfig databaseConfig;
	private final AccountManager accountManager;
	private final AccountBackupConfig config;
	private final Clock clock;
	private final BackupContainerWriter writer;
	private final BackupContainerReader reader;

	@Inject
	AccountBackupManagerImpl(DatabaseComponent db,
			DatabaseConfig databaseConfig, AccountManager accountManager,
			AccountBackupConfig config, Clock clock, CryptoComponent crypto,
			StreamWriterFactory streamWriterFactory,
			StreamReaderFactory streamReaderFactory,
			BdfWriterFactory bdfWriterFactory,
			BdfReaderFactory bdfReaderFactory) {
		this.db = db;
		this.databaseConfig = databaseConfig;
		this.accountManager = accountManager;
		this.config = config;
		this.clock = clock;
		writer = new BackupContainerWriter(crypto, streamWriterFactory,
				bdfWriterFactory);
		reader = new BackupContainerReader(crypto, streamReaderFactory,
				bdfReaderFactory);
	}

	@Override
	public BackupManifest exportAccount(OutputStream out, String recoveryCode,
			BackupProgressListener listener) throws DbException, IOException {
		if (RecoveryCode.normalise(recoveryCode) == null) {
			throw new IllegalArgumentException();
		}
		SecretKey dbKey = accountManager.getDatabaseKey();
		if (dbKey == null) throw new IllegalStateException();
		File tempDir = config.getBackupTempDirectory();
		if (!tempDir.exists() && !tempDir.mkdirs()) throw new IOException();
		File zip = new File(tempDir, TEMP_ZIP_NAME);
		try {
			// The snapshot is about the size of the database, so make sure
			// there is room for it before blocking writers to take it
			long dbSize = directorySize(databaseConfig.getDatabaseDirectory());
			if (tempDir.getUsableSpace() < dbSize) {
				throw new NotEnoughSpaceException();
			}
			db.backupDatabase(zip);
			// The manifest describes the database, so it has to be measured
			// and hashed before it can be written. Reading the archive twice
			// beats keeping a second copy of a large database on disk; it
			// holds ciphertext, so there is nothing to gain by compressing.
			SHA256Digest digest = new SHA256Digest();
			long dbLength = hashDatabase(zip, digest);
			byte[] hash = new byte[digest.getDigestSize()];
			digest.doFinal(hash, 0);
			BackupManifest m = new BackupManifest(BACKUP_FORMAT_VERSION,
					config.getAppVersionCode(), config.getAppVersionName(),
					CODE_SCHEMA_VERSION, clock.currentTimeMillis(), dbKey,
					BACKUP_DB_FILE_NAME, dbLength, hash);
			InputStream in = openDatabaseEntry(zip);
			try {
				writer.write(out, recoveryCode, m, in, listener);
			} finally {
				tryToClose(in, LOG, WARNING);
			}
			return m;
		} finally {
			IoUtils.deleteFileOrDir(zip);
		}
	}

	@Override
	public BackupManifest readBackup(InputStream in, String recoveryCode,
			File dbFile, BackupProgressListener listener)
			throws IOException, InvalidBackupException {
		if (RecoveryCode.normalise(recoveryCode) == null) {
			throw new IllegalArgumentException();
		}
		File parent = dbFile.getParentFile();
		if (parent != null && !parent.exists() && !parent.mkdirs()) {
			throw new IOException();
		}
		boolean restored = false;
		try {
			BackupManifest m;
			FileOutputStream out = new FileOutputStream(dbFile);
			try {
				m = reader.read(in, recoveryCode, out, listener);
				out.flush();
				// The key file is written once this returns, so the database
				// has to be on disk before then
				out.getFD().sync();
			} finally {
				out.close();
			}
			// Only now, because closing can still fail
			restored = true;
			return m;
		} finally {
			// Never leave a half-written database where a restore could
			// pick it up
			if (!restored) IoUtils.delete(dbFile);
		}
	}

	@Override
	public BackupManifest verifyBackup(InputStream in, String recoveryCode,
			BackupProgressListener listener)
			throws IOException, InvalidBackupException {
		if (RecoveryCode.normalise(recoveryCode) == null) {
			throw new IllegalArgumentException();
		}
		return reader.read(in, recoveryCode, new NullOutputStream(), listener);
	}

	/**
	 * Reads the database out of the archive, updating the given digest, and
	 * returns its length.
	 * <p>
	 * Also checks that the database is the whole archive. Anything else would
	 * be silently left out of the backup, and the hash would cover only what
	 * was written, so it would not be noticed until a restore.
	 */
	private long hashDatabase(File zip, SHA256Digest digest)
			throws IOException {
		ZipInputStream in = openDatabaseEntry(zip);
		try {
			byte[] buf = new byte[4096];
			long length = 0;
			int read;
			while ((read = in.read(buf)) != -1) {
				digest.update(buf, 0, read);
				length += read;
			}
			if (in.getNextEntry() != null) throw new IOException();
			return length;
		} finally {
			tryToClose(in, LOG, WARNING);
		}
	}

	/**
	 * Returns a stream positioned at the database entry of the archive, which
	 * has to be the first entry. The caller closes it.
	 * <p>
	 * Skipping past anything in front of it would leave that out of the
	 * backup as silently as trailing entries are refused by
	 * {@link #hashDatabase}.
	 */
	private ZipInputStream openDatabaseEntry(File zip) throws IOException {
		ZipInputStream in = new ZipInputStream(new FileInputStream(zip));
		try {
			ZipEntry e = in.getNextEntry();
			if (e != null && BACKUP_DB_FILE_NAME.equals(e.getName())) return in;
		} catch (IOException e) {
			tryToClose(in, LOG, WARNING);
			throw e;
		}
		tryToClose(in, LOG, WARNING);
		throw new IOException();
	}

	/**
	 * Throws rather than returning zero for a directory it cannot list: the
	 * size is there to decide whether the snapshot fits, and a zero would make
	 * that check pass without having measured anything.
	 */
	private long directorySize(File dir) throws IOException {
		File[] children = dir.listFiles();
		if (children == null) throw new IOException("Cannot list " + dir);
		long size = 0;
		for (File child : children) {
			size += child.isDirectory() ? directorySize(child) : child.length();
		}
		return size;
	}

	private static class NullOutputStream extends OutputStream {

		@Override
		public void write(int b) {
		}

		@Override
		public void write(byte[] b, int off, int len) {
		}
	}
}

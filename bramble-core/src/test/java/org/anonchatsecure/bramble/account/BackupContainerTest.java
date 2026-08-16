package org.anonchatsecure.bramble.account;

import org.anonchatsecure.bramble.api.account.BackupError;
import org.anonchatsecure.bramble.api.account.BackupManifest;
import org.anonchatsecure.bramble.api.account.BackupProgressListener;
import org.anonchatsecure.bramble.api.account.InvalidBackupException;
import org.anonchatsecure.bramble.api.account.RecoveryCode;
import org.anonchatsecure.bramble.api.crypto.CryptoComponent;
import org.anonchatsecure.bramble.api.crypto.SecretKey;
import org.anonchatsecure.bramble.api.data.BdfReaderFactory;
import org.anonchatsecure.bramble.api.data.BdfWriterFactory;
import org.anonchatsecure.bramble.api.transport.StreamReaderFactory;
import org.anonchatsecure.bramble.api.transport.StreamWriterFactory;
import org.anonchatsecure.bramble.test.BrambleTestCase;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import javax.inject.Inject;

import static org.anonchatsecure.bramble.api.account.BackupConstants.BACKUP_DB_FILE_NAME;
import static org.anonchatsecure.bramble.api.account.BackupConstants.BACKUP_FORMAT_VERSION;
import static org.anonchatsecure.bramble.api.account.BackupConstants.BACKUP_FORMAT_VERSION_OFFSET;
import static org.anonchatsecure.bramble.api.account.BackupConstants.BACKUP_HEADER_BYTES;
import static org.anonchatsecure.bramble.api.account.BackupConstants.BACKUP_KDF_OFFSET;
import static org.anonchatsecure.bramble.api.account.BackupConstants.BACKUP_LOG_COST_OFFSET;
import static org.anonchatsecure.bramble.api.account.BackupConstants.BACKUP_SALT_OFFSET;
import static org.anonchatsecure.bramble.api.account.BackupConstants.MAX_BACKUP_LOG_COST;
import static org.anonchatsecure.bramble.api.account.BackupConstants.MIN_BACKUP_LOG_COST;
import static org.anonchatsecure.bramble.api.account.BackupError.CORRUPT;
import static org.anonchatsecure.bramble.api.account.BackupError.DATA_TOO_NEW;
import static org.anonchatsecure.bramble.api.account.BackupError.DATA_TOO_OLD;
import static org.anonchatsecure.bramble.api.account.BackupError.NOT_A_BACKUP;
import static org.anonchatsecure.bramble.api.account.BackupError.TRUNCATED;
import static org.anonchatsecure.bramble.api.account.BackupError.UNSUPPORTED_FORMAT;
import static org.anonchatsecure.bramble.api.account.BackupError.WRONG_CODE_OR_DAMAGED;
import static org.anonchatsecure.bramble.api.db.DatabaseSchema.CODE_SCHEMA_VERSION;
import static org.anonchatsecure.bramble.api.db.DatabaseSchema.MIN_SCHEMA_VERSION;
import static org.anonchatsecure.bramble.api.transport.TransportConstants.MAX_FRAME_LENGTH;
import static org.anonchatsecure.bramble.api.transport.TransportConstants.STREAM_HEADER_LENGTH;
import static org.anonchatsecure.bramble.test.TestUtils.getRandomBytes;
import static org.anonchatsecure.bramble.test.TestUtils.getSecretKey;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class BackupContainerTest extends BrambleTestCase {

	private static final String CODE = "123456789012345678901234567890";
	private static final String WRONG_CODE = "999999999999999999999999999999";
	// Enough to span several frames, so a middle frame can be altered
	private static final int DB_BYTES = 3000;

	@Inject
	CryptoComponent crypto;
	@Inject
	StreamWriterFactory streamWriterFactory;
	@Inject
	StreamReaderFactory streamReaderFactory;
	@Inject
	BdfWriterFactory bdfWriterFactory;
	@Inject
	BdfReaderFactory bdfReaderFactory;

	private final byte[] db = getRandomBytes(DB_BYTES);
	private final SecretKey dbKey = getSecretKey();
	private final RecordingProgressListener progress =
			new RecordingProgressListener();
	private final BackupContainerWriter writer;
	private final BackupContainerReader reader;

	public BackupContainerTest() {
		BackupContainerTestComponent component =
				DaggerBackupContainerTestComponent.builder().build();
		component.inject(this);
		// The cheapest cost the format allows keeps the tests quick
		writer = new BackupContainerWriter(crypto, streamWriterFactory,
				bdfWriterFactory, MIN_BACKUP_LOG_COST);
		reader = new BackupContainerReader(crypto, streamReaderFactory,
				bdfReaderFactory);
	}

	@Test
	public void testRoundTrip() throws Exception {
		byte[] container = write(manifest());
		ByteArrayOutputStream dbOut = new ByteArrayOutputStream();
		BackupManifest m = reader.read(new ByteArrayInputStream(container),
				CODE, dbOut, progress);
		assertArrayEquals(db, dbOut.toByteArray());
		assertEquals(BACKUP_FORMAT_VERSION, m.getFormatVersion());
		assertEquals(1234, m.getAppVersionCode());
		assertEquals("1.6.0", m.getAppVersionName());
		assertEquals(CODE_SCHEMA_VERSION, m.getSchemaVersion());
		assertEquals(1755000000000L, m.getCreated());
		assertArrayEquals(dbKey.getBytes(), m.getDbKey().getBytes());
		assertEquals(BACKUP_DB_FILE_NAME, m.getDbFileName());
		assertEquals(DB_BYTES, m.getDbLength());
		assertArrayEquals(sha256(db), m.getDbSha256());
		progress.assertFinished(DB_BYTES);
	}

	@Test
	public void testEachBackupUsesAFreshSalt() throws Exception {
		byte[] first = write(manifest()), second = write(manifest());
		byte[] firstSalt = Arrays.copyOfRange(first, BACKUP_SALT_OFFSET,
				BACKUP_HEADER_BYTES);
		byte[] secondSalt = Arrays.copyOfRange(second, BACKUP_SALT_OFFSET,
				BACKUP_HEADER_BYTES);
		assertNotEquals(Arrays.toString(firstSalt),
				Arrays.toString(secondSalt));
	}

	@Test
	public void testWrongCodeIsRejected() throws Exception {
		assertRejected(write(manifest()), WRONG_CODE, WRONG_CODE_OR_DAMAGED);
	}

	@Test
	public void testFileWithoutTheMagicNumberIsRejected() throws Exception {
		assertRejected(flipBit(write(manifest()), 0), NOT_A_BACKUP);
	}

	@Test
	public void testUnknownFormatVersionIsRejected() throws Exception {
		byte[] container = write(manifest());
		container[BACKUP_FORMAT_VERSION_OFFSET] = BACKUP_FORMAT_VERSION + 1;
		assertRejected(container, UNSUPPORTED_FORMAT);
	}

	@Test
	public void testUnknownKdfIsRejected() throws Exception {
		byte[] container = write(manifest());
		container[BACKUP_KDF_OFFSET] = 1;
		assertRejected(container, UNSUPPORTED_FORMAT);
	}

	/**
	 * The test would not finish if a header claiming 2^30 were acted on, so
	 * passing is the evidence that nothing is allocated for it.
	 */
	@Test
	public void testCostParameterOutsideTheAllowedRangeIsRejected()
			throws Exception {
		byte[] container = write(manifest());
		container[BACKUP_LOG_COST_OFFSET] = MIN_BACKUP_LOG_COST - 1;
		assertRejected(container, UNSUPPORTED_FORMAT);
		container[BACKUP_LOG_COST_OFFSET] = MAX_BACKUP_LOG_COST + 1;
		assertRejected(container, UNSUPPORTED_FORMAT);
		container[BACKUP_LOG_COST_OFFSET] = 30;
		assertRejected(container, UNSUPPORTED_FORMAT);
	}

	/**
	 * The cost is read before anything is authenticated, so the most
	 * expensive one the format allows has to be affordable.
	 */
	@Test
	public void testHighestAllowedCostParameterCanBeRead() throws Exception {
		BackupContainerWriter expensive = new BackupContainerWriter(crypto,
				streamWriterFactory, bdfWriterFactory, MAX_BACKUP_LOG_COST);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		expensive.write(out, CODE, manifest(), new ByteArrayInputStream(db),
				progress);
		ByteArrayOutputStream dbOut = new ByteArrayOutputStream();
		reader.read(new ByteArrayInputStream(out.toByteArray()), CODE, dbOut,
				progress);
		assertArrayEquals(db, dbOut.toByteArray());
	}

	@Test
	public void testCodeIsNormalisedBeforeUse() throws Exception {
		byte[] container = write(manifest());
		ByteArrayOutputStream dbOut = new ByteArrayOutputStream();
		// The app shows the code in groups, so this is what a user types back
		reader.read(new ByteArrayInputStream(container),
				RecoveryCode.format(CODE), dbOut, progress);
		assertArrayEquals(db, dbOut.toByteArray());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCodeThatIsNotARecoveryCodeIsRejected() throws Exception {
		reader.read(new ByteArrayInputStream(write(manifest())), "nonsense",
				new ByteArrayOutputStream(), progress);
	}

	@Test
	public void testDatabaseFileNameFromTheManifestIsNotTrusted()
			throws Exception {
		BackupManifest m = new BackupManifest(BACKUP_FORMAT_VERSION, 1234,
				"1.6.0", CODE_SCHEMA_VERSION, 1755000000000L, dbKey,
				"../../shared_prefs/evil.xml", DB_BYTES, sha256(db));
		assertRejected(write(m), CORRUPT);
	}

	@Test
	public void testAlteredSaltIsRejected() throws Exception {
		assertRejected(flipBit(write(manifest()), BACKUP_SALT_OFFSET),
				WRONG_CODE_OR_DAMAGED);
	}

	@Test
	public void testAlteredStreamHeaderIsRejected() throws Exception {
		assertRejected(flipBit(write(manifest()), BACKUP_HEADER_BYTES + 30),
				WRONG_CODE_OR_DAMAGED);
	}

	@Test
	public void testAlteredFirstFrameIsRejected() throws Exception {
		byte[] container = write(manifest());
		int firstFrame = BACKUP_HEADER_BYTES + STREAM_HEADER_LENGTH;
		assertRejected(flipBit(container, firstFrame + 30), CORRUPT);
	}

	@Test
	public void testAlteredMiddleFrameIsRejected() throws Exception {
		byte[] container = write(manifest());
		int secondFrame =
				BACKUP_HEADER_BYTES + STREAM_HEADER_LENGTH + MAX_FRAME_LENGTH;
		assertTrue(container.length > secondFrame + MAX_FRAME_LENGTH);
		assertRejected(flipBit(container, secondFrame + 100), CORRUPT);
	}

	@Test
	public void testAlteredFinalFrameIsRejected() throws Exception {
		byte[] container = write(manifest());
		assertRejected(flipBit(container, container.length - 1), CORRUPT);
	}

	@Test
	public void testTruncationIsDetectedAtEveryBoundary() throws Exception {
		byte[] container = write(manifest());
		int streamHeaderEnd = BACKUP_HEADER_BYTES + STREAM_HEADER_LENGTH;
		// Too short to hold a header, so it is not recognisable as a backup
		for (int length : new int[] {0, 1, BACKUP_HEADER_BYTES - 1}) {
			assertRejected(Arrays.copyOf(container, length), NOT_A_BACKUP);
		}
		int[] boundaries = {
				BACKUP_HEADER_BYTES,
				BACKUP_HEADER_BYTES + 1,
				streamHeaderEnd - 1,
				streamHeaderEnd,
				streamHeaderEnd + 1,
				streamHeaderEnd + MAX_FRAME_LENGTH - 1,
				streamHeaderEnd + MAX_FRAME_LENGTH,
				streamHeaderEnd + MAX_FRAME_LENGTH + 1,
				container.length - 1
		};
		for (int length : boundaries) {
			assertTrue(length < container.length);
			assertRejected(Arrays.copyOf(container, length), TRUNCATED);
		}
	}

	@Test
	public void testSchemaVersionFromANewerAppIsRefusedBeforeWriting()
			throws Exception {
		byte[] container = write(manifest(CODE_SCHEMA_VERSION + 1));
		ByteArrayOutputStream dbOut = new ByteArrayOutputStream();
		try {
			reader.read(new ByteArrayInputStream(container), CODE, dbOut,
					progress);
			fail();
		} catch (InvalidBackupException e) {
			assertEquals(DATA_TOO_NEW, e.getError());
		}
		assertEquals(0, dbOut.size());
	}

	@Test
	public void testSchemaVersionThatCannotBeMigratedIsRejected()
			throws Exception {
		assertRejected(write(manifest(MIN_SCHEMA_VERSION - 1)), DATA_TOO_OLD);
	}

	@Test
	public void testWriterRefusesAHashThatDoesNotMatch() throws Exception {
		BackupManifest m = new BackupManifest(BACKUP_FORMAT_VERSION, 1234,
				"1.6.0", CODE_SCHEMA_VERSION, 1755000000000L, dbKey,
				BACKUP_DB_FILE_NAME, DB_BYTES, sha256(new byte[DB_BYTES]));
		try {
			write(m);
			fail();
		} catch (IOException expected) {
			// Expected
		}
	}

	@Test
	public void testWriterRefusesALengthThatDoesNotMatch() throws Exception {
		BackupManifest tooShort = new BackupManifest(BACKUP_FORMAT_VERSION,
				1234, "1.6.0", CODE_SCHEMA_VERSION, 1755000000000L, dbKey,
				BACKUP_DB_FILE_NAME, DB_BYTES - 1, sha256(db));
		try {
			write(tooShort);
			fail();
		} catch (IOException expected) {
			// Expected
		}
		BackupManifest tooLong = new BackupManifest(BACKUP_FORMAT_VERSION,
				1234, "1.6.0", CODE_SCHEMA_VERSION, 1755000000000L, dbKey,
				BACKUP_DB_FILE_NAME, DB_BYTES + 1, sha256(db));
		try {
			write(tooLong);
			fail();
		} catch (IOException expected) {
			// Expected
		}
	}

	private BackupManifest manifest() {
		return manifest(CODE_SCHEMA_VERSION);
	}

	private BackupManifest manifest(int schemaVersion) {
		return new BackupManifest(BACKUP_FORMAT_VERSION, 1234, "1.6.0",
				schemaVersion, 1755000000000L, dbKey, BACKUP_DB_FILE_NAME,
				DB_BYTES, sha256(db));
	}

	private byte[] write(BackupManifest m) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		writer.write(out, CODE, m, new ByteArrayInputStream(db), progress);
		return out.toByteArray();
	}

	private void assertRejected(byte[] container, BackupError expected)
			throws Exception {
		assertRejected(container, CODE, expected);
	}

	private void assertRejected(byte[] container, String code,
			BackupError expected) throws Exception {
		try {
			reader.read(new ByteArrayInputStream(container), code,
					new ByteArrayOutputStream(), progress);
			fail();
		} catch (InvalidBackupException e) {
			assertEquals(expected, e.getError());
		}
	}

	private byte[] flipBit(byte[] b, int offset) {
		byte[] copy = b.clone();
		copy[offset] ^= 1;
		return copy;
	}

	/**
	 * Records progress and fails as soon as it is reported out of order, so
	 * every test that copies a database checks it.
	 */
	private static class RecordingProgressListener
			implements BackupProgressListener {

		private long lastDone = -1, lastTotal = -1;

		@Override
		public void onBackupProgress(long done, long total) {
			assertTrue(done >= 0 && done <= total);
			if (done == 0) {
				// Every copy opens with a report of zero
				lastTotal = total;
			} else {
				assertEquals(lastTotal, total);
				assertTrue(done >= lastDone);
			}
			lastDone = done;
		}

		void assertFinished(long total) {
			assertEquals(total, lastTotal);
			assertEquals(total, lastDone);
		}
	}

	private byte[] sha256(byte[] b) {
		SHA256Digest digest = new SHA256Digest();
		digest.update(b, 0, b.length);
		byte[] hash = new byte[digest.getDigestSize()];
		digest.doFinal(hash, 0);
		return hash;
	}
}

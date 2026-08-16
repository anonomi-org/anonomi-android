package org.anonchatsecure.bramble.account;

import org.anonchatsecure.bramble.api.account.InvalidBackupException;
import org.anonchatsecure.bramble.api.account.RecoveryCode;
import org.anonchatsecure.bramble.api.crypto.CryptoComponent;
import org.anonchatsecure.bramble.api.crypto.SecretKey;
import org.anonchatsecure.bramble.util.ByteUtils;
import org.briarproject.nullsafety.NotNullByDefault;

import java.security.SecureRandom;

import javax.annotation.concurrent.Immutable;

import static java.lang.System.arraycopy;
import static org.anonchatsecure.bramble.api.account.BackupConstants.BACKUP_FORMAT_VERSION;
import static org.anonchatsecure.bramble.api.account.BackupConstants.BACKUP_FORMAT_VERSION_OFFSET;
import static org.anonchatsecure.bramble.api.account.BackupConstants.BACKUP_HEADER_BYTES;
import static org.anonchatsecure.bramble.api.account.BackupConstants.BACKUP_HEADER_LABEL;
import static org.anonchatsecure.bramble.api.account.BackupConstants.BACKUP_KDF_OFFSET;
import static org.anonchatsecure.bramble.api.account.BackupConstants.BACKUP_LOG_COST_OFFSET;
import static org.anonchatsecure.bramble.api.account.BackupConstants.BACKUP_MAGIC;
import static org.anonchatsecure.bramble.api.account.BackupConstants.BACKUP_SALT_BYTES;
import static org.anonchatsecure.bramble.api.account.BackupConstants.BACKUP_SALT_OFFSET;
import static org.anonchatsecure.bramble.api.account.BackupConstants.KDF_BLOCK_SIZE;
import static org.anonchatsecure.bramble.api.account.BackupConstants.KDF_SCRYPT;
import static org.anonchatsecure.bramble.api.account.BackupConstants.MAX_BACKUP_LOG_COST;
import static org.anonchatsecure.bramble.api.account.BackupConstants.MIN_BACKUP_LOG_COST;
import static org.anonchatsecure.bramble.api.account.BackupError.NOT_A_BACKUP;
import static org.anonchatsecure.bramble.api.account.BackupError.UNSUPPORTED_FORMAT;

/**
 * The plaintext header at the start of a backup file.
 */
@Immutable
@NotNullByDefault
class BackupHeader {

	private final byte[] bytes;
	private final int logCost;

	private BackupHeader(byte[] bytes, int logCost) {
		this.bytes = bytes;
		this.logCost = logCost;
	}

	static BackupHeader create(SecureRandom random, int logCost) {
		if (logCost < MIN_BACKUP_LOG_COST || logCost > MAX_BACKUP_LOG_COST) {
			throw new IllegalArgumentException();
		}
		byte[] bytes = new byte[BACKUP_HEADER_BYTES];
		ByteUtils.writeUint64(BACKUP_MAGIC, bytes, 0);
		bytes[BACKUP_FORMAT_VERSION_OFFSET] = (byte) BACKUP_FORMAT_VERSION;
		bytes[BACKUP_KDF_OFFSET] = (byte) KDF_SCRYPT;
		bytes[BACKUP_LOG_COST_OFFSET] = (byte) logCost;
		byte[] salt = new byte[BACKUP_SALT_BYTES];
		random.nextBytes(salt);
		arraycopy(salt, 0, bytes, BACKUP_SALT_OFFSET, BACKUP_SALT_BYTES);
		return new BackupHeader(bytes, logCost);
	}

	/**
	 * Parses the given bytes, rejecting anything this code cannot read
	 * before any work is done on the rest of the file.
	 */
	static BackupHeader parse(byte[] bytes) throws InvalidBackupException {
		if (bytes.length != BACKUP_HEADER_BYTES) {
			throw new InvalidBackupException(NOT_A_BACKUP);
		}
		if (ByteUtils.readUint64(bytes, 0) != BACKUP_MAGIC) {
			throw new InvalidBackupException(NOT_A_BACKUP);
		}
		if ((bytes[BACKUP_FORMAT_VERSION_OFFSET] & 0xFF)
				!= BACKUP_FORMAT_VERSION) {
			throw new InvalidBackupException(UNSUPPORTED_FORMAT);
		}
		if ((bytes[BACKUP_KDF_OFFSET] & 0xFF) != KDF_SCRYPT) {
			throw new InvalidBackupException(UNSUPPORTED_FORMAT);
		}
		int logCost = bytes[BACKUP_LOG_COST_OFFSET] & 0xFF;
		if (logCost < MIN_BACKUP_LOG_COST || logCost > MAX_BACKUP_LOG_COST) {
			throw new InvalidBackupException(UNSUPPORTED_FORMAT);
		}
		// The cost is read before anything has been authenticated, so also
		// refuse one this device cannot afford. Same bound as ScryptKdf.
		long maxCost = Runtime.getRuntime().maxMemory() / KDF_BLOCK_SIZE / 256;
		if ((1L << logCost) > maxCost) {
			throw new InvalidBackupException(UNSUPPORTED_FORMAT);
		}
		return new BackupHeader(bytes.clone(), logCost);
	}

	byte[] getBytes() {
		return bytes.clone();
	}

	/**
	 * @throws IllegalArgumentException if the code is not a recovery code.
	 * Callers take the code from the user and must check it with
	 * {@link RecoveryCode#normalise(String)} first.
	 */
	SecretKey deriveKey(CryptoComponent crypto, String recoveryCode) {
		String normalised = RecoveryCode.normalise(recoveryCode);
		if (normalised == null) throw new IllegalArgumentException();
		byte[] salt = new byte[BACKUP_SALT_BYTES];
		arraycopy(bytes, BACKUP_SALT_OFFSET, salt, 0, BACKUP_SALT_BYTES);
		SecretKey derived =
				crypto.deriveKeyFromPassword(normalised, salt, 1 << logCost);
		return crypto.deriveKey(BACKUP_HEADER_LABEL, derived);
	}
}

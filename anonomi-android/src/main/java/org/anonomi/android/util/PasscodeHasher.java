package org.anonomi.android.util;

import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

/**
 * Checks a calculator passcode without being able to read it back. Argon2id
 * rather than a plain digest because the secret is a short expression typed on
 * a keypad, so the only thing that makes a guess expensive is the work factor.
 */
public final class PasscodeHasher {

	private static final String ALGORITHM = "argon2id";
	private static final char FIELD = '$';

	/**
	 * Kept well inside what a low-end phone can allocate: being locked out of
	 * your own app because the check ran out of memory is worse than a
	 * cheaper guess.
	 */
	private static final int MEMORY_KIB = 8192;
	private static final int ITERATIONS = 4;
	private static final int PARALLELISM = 1;
	private static final int SALT_BYTES = 16;
	private static final int HASH_BYTES = 32;

	// Room to raise the cost later, but not to be handed an allocation that
	// kills the process on every attempt.
	private static final int MAX_MEMORY_KIB = 32768;
	private static final int MAX_ITERATIONS = 64;
	private static final int MAX_PARALLELISM = 4;
	private static final int MAX_HASH_BYTES = 64;

	private PasscodeHasher() {
	}

	/**
	 * Passcodes are digits and operators, so a value stored as one can never
	 * be mistaken for a record.
	 */
	public static boolean isHashed(String stored) {
		return stored.startsWith(ALGORITHM + FIELD);
	}

	public static String hash(String passcode) {
		byte[] salt = new byte[SALT_BYTES];
		new SecureRandom().nextBytes(salt);
		return hash(passcode, salt, MEMORY_KIB, ITERATIONS, PARALLELISM);
	}

	static String hash(String passcode, byte[] salt, int memoryKib,
			int iterations, int parallelism) {
		byte[] hash = derive(PasscodePolicy.normalise(passcode), salt,
				memoryKib, iterations, parallelism, HASH_BYTES);
		return ALGORITHM + FIELD + memoryKib + FIELD + iterations + FIELD
				+ parallelism + FIELD + toHex(salt) + FIELD + toHex(hash);
	}

	/**
	 * Whether this value should be replaced next time the passcode is known to
	 * be correct: it holds the expression itself, or a cost we have moved away
	 * from. Verifying uses the parameters in the record rather than the current
	 * ones, so the cost can be raised without invalidating anyone's passcode.
	 */
	public static boolean needsRehash(String stored) {
		if (!isHashed(stored)) return true;
		Record record = parse(stored);
		return record == null || record.memoryKib != MEMORY_KIB
				|| record.iterations != ITERATIONS
				|| record.parallelism != PARALLELISM
				|| record.salt.length != SALT_BYTES
				|| record.hash.length != HASH_BYTES;
	}

	/**
	 * @return false for anything that is not this passcode, including a stored
	 * value that makes no sense. Never throws: the caller must not react
	 * differently to a broken passcode than to a wrong one.
	 */
	public static boolean verify(String passcode, String stored) {
		String candidate = PasscodePolicy.normalise(passcode);
		if (!isHashed(stored)) {
			// Set before passcodes were hashed: the expression itself.
			byte[] typed = candidate.getBytes(StandardCharsets.UTF_8);
			byte[] expected = PasscodePolicy.normalise(stored)
					.getBytes(StandardCharsets.UTF_8);
			return expected.length > 0
					&& MessageDigest.isEqual(typed, expected);
		}
		Record record = parse(stored);
		if (record == null) return false;
		byte[] actual = derive(candidate, record.salt, record.memoryKib,
				record.iterations, record.parallelism, record.hash.length);
		return MessageDigest.isEqual(actual, record.hash);
	}

	private static byte[] derive(String passcode, byte[] salt, int memoryKib,
			int iterations, int parallelism, int length) {
		Argon2Parameters parameters =
				new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
						.withVersion(Argon2Parameters.ARGON2_VERSION_13)
						.withSalt(salt)
						.withMemoryAsKB(memoryKib)
						.withIterations(iterations)
						.withParallelism(parallelism)
						.build();
		Argon2BytesGenerator generator = new Argon2BytesGenerator();
		generator.init(parameters);
		byte[] out = new byte[length];
		generator.generateBytes(passcode.getBytes(StandardCharsets.UTF_8), out);
		return out;
	}

	private static final class Record {

		private final int memoryKib, iterations, parallelism;
		private final byte[] salt, hash;

		Record(int memoryKib, int iterations, int parallelism, byte[] salt,
				byte[] hash) {
			this.memoryKib = memoryKib;
			this.iterations = iterations;
			this.parallelism = parallelism;
			this.salt = salt;
			this.hash = hash;
		}
	}

	/**
	 * @return null if this is not a record we would have written. The bounds
	 * are checked because whoever can plant a value chooses the parameters,
	 * and those decide how much work reading it costs.
	 */
	private static Record parse(String stored) {
		String[] parts = stored.split("\\" + FIELD, -1);
		if (parts.length != 6) return null;
		try {
			int memoryKib = Integer.parseInt(parts[1]);
			int iterations = Integer.parseInt(parts[2]);
			int parallelism = Integer.parseInt(parts[3]);
			byte[] salt = fromHex(parts[4]);
			byte[] hash = fromHex(parts[5]);
			if (memoryKib < 1 || memoryKib > MAX_MEMORY_KIB) return null;
			if (iterations < 1 || iterations > MAX_ITERATIONS) return null;
			if (parallelism < 1 || parallelism > MAX_PARALLELISM) return null;
			// Argon2 needs at least eight blocks per lane and a salt of eight
			// bytes; below either, the generator throws rather than returns.
			if (memoryKib < parallelism * 8) return null;
			if (salt.length < 8) return null;
			if (hash.length < 4 || hash.length > MAX_HASH_BYTES) return null;
			return new Record(memoryKib, iterations, parallelism, salt, hash);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static String toHex(byte[] bytes) {
		StringBuilder hex = new StringBuilder(bytes.length * 2);
		for (byte b : bytes) {
			hex.append(Character.forDigit((b >> 4) & 0xF, 16));
			hex.append(Character.forDigit(b & 0xF, 16));
		}
		return hex.toString();
	}

	/**
	 * @throws NumberFormatException if this is not an even-length run of hex
	 * digits, which {@link #parse} turns into "not a record".
	 */
	private static byte[] fromHex(String hex) {
		int length = hex.length();
		if (length == 0 || length % 2 != 0) {
			throw new NumberFormatException("not a hex string");
		}
		byte[] bytes = new byte[length / 2];
		for (int i = 0; i < bytes.length; i++) {
			int high = Character.digit(hex.charAt(i * 2), 16);
			int low = Character.digit(hex.charAt(i * 2 + 1), 16);
			if (high < 0 || low < 0) {
				throw new NumberFormatException("not a hex string");
			}
			bytes[i] = (byte) ((high << 4) | low);
		}
		return bytes;
	}
}

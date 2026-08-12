package org.anonomi.android.util;

import androidx.annotation.Nullable;

/**
 * The outcome of reading one entry from {@link SecurePrefsManager}.
 * <p>
 * Reading encrypted storage has three outcomes and they are not
 * interchangeable. A key that was never written belongs to someone who has
 * configured nothing; a key whose value will not decrypt belongs to someone
 * whose configuration exists but is currently unavailable to us. Collapsing
 * the second into the first makes every caller read "I cannot read your
 * security settings" as "you have no security settings", which is how a
 * configured panic action used to degrade into the weakest one.
 * <p>
 * Callers must decide for themselves what {@link State#UNREADABLE} means, and
 * the type gives them no way round it. There is no accessor that returns the
 * value or {@code null}, and none that supplies a default, because either one
 * would let "could not be read" quietly collapse back into "not set" - the
 * ambiguity this type exists to remove. Reading means asking which of the
 * three states you are in.
 */
public final class SecureValue {

	public enum State {
		/** The stored value was decrypted successfully. */
		PRESENT,
		/** The key holds nothing; it has never been written. */
		ABSENT,
		/**
		 * The key holds something that could not be turned back into a
		 * value: the Keystore key is gone or invalidated, the GCM tag did
		 * not verify, or the record is not in the expected format.
		 */
		UNREADABLE
	}

	private static final SecureValue ABSENT =
			new SecureValue(State.ABSENT, null, null);

	private final State state;
	@Nullable
	private final String value;
	@Nullable
	private final String reason;

	private SecureValue(State state, @Nullable String value,
			@Nullable String reason) {
		this.state = state;
		this.value = value;
		this.reason = reason;
	}

	public static SecureValue present(String value) {
		if (value == null) throw new NullPointerException("value");
		return new SecureValue(State.PRESENT, value, null);
	}

	public static SecureValue absent() {
		return ABSENT;
	}

	/**
	 * @param reason why the value could not be read. Must never contain any
	 * part of the stored value: it is surfaced in the UI and in logs.
	 */
	public static SecureValue unreadable(String reason) {
		return new SecureValue(State.UNREADABLE, null, reason);
	}

	public State getState() {
		return state;
	}

	public boolean isPresent() {
		return state == State.PRESENT;
	}

	public boolean isAbsent() {
		return state == State.ABSENT;
	}

	public boolean isUnreadable() {
		return state == State.UNREADABLE;
	}

	/**
	 * @throws IllegalStateException unless this value is
	 * {@link State#PRESENT}. Check {@link #isPresent()} first.
	 */
	public String get() {
		if (state != State.PRESENT) {
			throw new IllegalStateException(
					"No value to read; state is " + state +
							(reason == null ? "" : ": " + reason));
		}
		//noinspection ConstantConditions - PRESENT always carries a value
		return value;
	}

	/**
	 * Never includes the value: these carry passcodes and private keys.
	 */
	@Override
	public String toString() {
		return state == State.UNREADABLE
				? "SecureValue{UNREADABLE: " + reason + "}"
				: "SecureValue{" + state + "}";
	}
}

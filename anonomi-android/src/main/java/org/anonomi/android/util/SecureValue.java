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
 * Callers must decide for themselves what {@link State#UNREADABLE} means, so
 * {@link #orIfAbsent(String)} deliberately refuses to supply a default for it.
 * There is no accessor that returns the value or {@code null}: that would
 * restore the ambiguity this type exists to remove.
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
	 * {@link State#PRESENT}. Check {@link #isPresent()} first, or use
	 * {@link #orIfAbsent(String)}.
	 */
	public String get() {
		if (state != State.PRESENT) {
			throw new IllegalStateException(
					"No value to read; state is " + state);
		}
		//noinspection ConstantConditions - PRESENT always carries a value
		return value;
	}

	/**
	 * Returns the stored value if it is {@link State#PRESENT}, or
	 * {@code fallback} if nothing has ever been stored.
	 *
	 * @throws IllegalStateException if the value is
	 * {@link State#UNREADABLE}. An unreadable value is not an unset one, and
	 * the caller has to say what it wants to happen instead.
	 */
	public String orIfAbsent(String fallback) {
		switch (state) {
			case PRESENT:
				//noinspection ConstantConditions
				return value;
			case ABSENT:
				return fallback;
			default:
				throw new IllegalStateException(
						"Value is unreadable, not absent: " + reason);
		}
	}

	/**
	 * Why the value could not be read, or null if it could.
	 */
	@Nullable
	public String getReason() {
		return reason;
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

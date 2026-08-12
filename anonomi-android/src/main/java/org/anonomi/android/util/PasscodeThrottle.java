package org.anonomi.android.util;

import androidx.annotation.Nullable;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * How long the calculator ignores a passcode after a run of wrong ones. The
 * count is what does the work: it goes back to zero only on a correct
 * passcode, so the delay keeps growing however the attempts are spaced.
 * <p>
 * The deadline is kept against both clocks and the longer wait wins, because
 * neither clock survives on its own. Whoever holds the device can move the
 * wall clock forward from Settings, which would otherwise retire a deadline
 * early; the monotonic clock cannot be moved but restarts at every boot, which
 * would otherwise make a stored deadline meaningless. Each is also capped at
 * the delay it was set for, so a clock that has moved can shorten neither wait
 * and lengthen neither.
 */
public final class PasscodeThrottle {

	/**
	 * Somewhere to keep the count across the calculator being killed from
	 * recents, which is the cheap way to reset one held in memory.
	 */
	public interface Store {

		SecureValue read();

		void write(String state);
	}

	/**
	 * Deliberately not bramble's {@code Clock}: that exposes only wall-clock
	 * time, which is the half of this an attacker can set.
	 */
	public interface Clock {

		long wallClockMillis();

		/** @see android.os.SystemClock#elapsedRealtime() */
		long elapsedRealtimeMillis();
	}

	static final int FREE_ATTEMPTS = 5;
	static final long FIRST_DELAY_MS = 30_000L;
	static final long MAX_DELAY_MS = 60 * 60_000L;

	/**
	 * Where the delay stops growing, and where an unreadable count is pinned
	 * so that destroying it costs the longest wait rather than clearing it.
	 */
	static final int MAX_FAILURES = FREE_ATTEMPTS + 32;

	/**
	 * A state written before the monotonic deadline was recorded. Such a state
	 * is honoured against the wall clock alone, which is what it was written
	 * for; the next failure rewrites it with both.
	 */
	private static final long NO_DEADLINE = Long.MIN_VALUE;

	private final Store store;
	private final Clock clock;

	public PasscodeThrottle(Store store, Clock clock) {
		this.store = store;
		this.clock = clock;
	}

	static long delayFor(int failures) {
		if (failures <= FREE_ATTEMPTS) return 0;
		int doublings = failures - FREE_ATTEMPTS - 1;
		if (doublings >= 32) return MAX_DELAY_MS;
		return min(FIRST_DELAY_MS << doublings, MAX_DELAY_MS);
	}

	/**
	 * @return how much longer this passcode will be ignored, in milliseconds,
	 * or zero if it will be checked now.
	 */
	public long remainingLockMillis() {
		State state = load();
		if (state == null) {
			// Unreadable or malformed. Fail closed, but write a state that
			// expires: the owner of a device whose Keystore has been reset
			// must not be shut out of their own app for good.
			store.write(stateFor(MAX_FAILURES).format());
			return MAX_DELAY_MS;
		}
		long delay = delayFor(state.failures);
		if (delay == 0) return 0;

		long wallNow = clock.wallClockMillis();
		long elapsedNow = clock.elapsedRealtimeMillis();
		long wallRemaining = remainingAgainst(state.wallDeadline, wallNow,
				delay);
		long elapsedRemaining = remainingAgainst(state.elapsedDeadline,
				elapsedNow, delay);

		// A deadline further away than its own delay means the clock it was
		// written against has moved: the wall clock set backwards, or the
		// monotonic one restarted at a boot. Re-anchor it, or it would hold
		// the lock for as long as the machine had been up.
		if (wallRemaining > delay || elapsedRemaining > delay) {
			if (wallRemaining > delay) wallRemaining = delay;
			if (elapsedRemaining > delay) elapsedRemaining = delay;
			store.write(new State(state.failures, wallNow + wallRemaining,
					elapsedNow + elapsedRemaining).format());
		}
		return max(wallRemaining, elapsedRemaining);
	}

	/**
	 * @return the raw distance to the deadline, which the caller compares
	 * against the delay to decide whether the clock has moved under it. A
	 * deadline that is merely absent has expired as far as this is concerned.
	 */
	private static long remainingAgainst(long deadline, long now, long delay) {
		if (deadline == NO_DEADLINE) return 0;
		long remaining = deadline - now;
		return remaining <= 0 ? 0 : remaining;
	}

	public boolean isLocked() {
		return remainingLockMillis() > 0;
	}

	public void recordFailure() {
		State state = load();
		int failures =
				min((state == null ? MAX_FAILURES : state.failures) + 1,
						MAX_FAILURES);
		store.write(stateFor(failures).format());
	}

	/** Forgets the run of failures. */
	public void recordSuccess() {
		store.write(new State(0, 0, 0).format());
	}

	private State stateFor(int failures) {
		long delay = delayFor(failures);
		return new State(failures, clock.wallClockMillis() + delay,
				clock.elapsedRealtimeMillis() + delay);
	}

	/**
	 * @return null if the stored state cannot be trusted, which the caller
	 * treats as the longest delay rather than the shortest.
	 */
	@Nullable
	private State load() {
		SecureValue stored = store.read();
		if (stored.isAbsent()) return new State(0, 0, 0);
		if (!stored.isPresent()) return null;
		String[] parts = stored.get().split(":", -1);
		if (parts.length != 2 && parts.length != 3) return null;
		int failures;
		long wallDeadline, elapsedDeadline;
		try {
			failures = Integer.parseInt(parts[0]);
			wallDeadline = Long.parseLong(parts[1]);
			elapsedDeadline = parts.length == 3
					? Long.parseLong(parts[2])
					: NO_DEADLINE;
		} catch (NumberFormatException e) {
			return null;
		}
		if (failures < 0 || wallDeadline < 0) return null;
		if (elapsedDeadline < 0 && elapsedDeadline != NO_DEADLINE) return null;
		return new State(min(failures, MAX_FAILURES), wallDeadline,
				elapsedDeadline);
	}

	private static final class State {

		private final int failures;
		private final long wallDeadline, elapsedDeadline;

		State(int failures, long wallDeadline, long elapsedDeadline) {
			this.failures = failures;
			this.wallDeadline = wallDeadline;
			this.elapsedDeadline = elapsedDeadline;
		}

		String format() {
			return failures + ":" + wallDeadline + ":" + elapsedDeadline;
		}
	}
}

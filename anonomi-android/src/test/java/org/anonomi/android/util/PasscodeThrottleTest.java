package org.anonomi.android.util;

import org.junit.Test;

import static org.anonomi.android.util.PasscodeThrottle.FIRST_DELAY_MS;
import static org.anonomi.android.util.PasscodeThrottle.FREE_ATTEMPTS;
import static org.anonomi.android.util.PasscodeThrottle.MAX_DELAY_MS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Guessing a short arithmetic expression is cheap, so the answer has to get
 * expensive. What matters here is that the cost cannot be sidestepped: not by
 * killing the calculator from recents, not from Settings, and not by turning
 * the phone off and on again.
 */
public class PasscodeThrottleTest {

	private static final long START = 1_700_000_000_000L;

	/** Long enough that a deadline held against it would be a real lockout. */
	private static final long UPTIME = 5L * 24 * 60 * 60 * 1000;

	private final FakeStore store = new FakeStore();
	private final FakeClock clock = new FakeClock();

	private PasscodeThrottle throttle() {
		return new PasscodeThrottle(store, clock);
	}

	private void failUntilLocked() {
		for (int i = 0; i < FREE_ATTEMPTS + 1; i++) throttle().recordFailure();
		assertEquals(FIRST_DELAY_MS, throttle().remainingLockMillis());
	}

	@Test
	public void nothingIsLockedBeforeAnythingHasBeenTyped() {
		assertFalse(throttle().isLocked());
		assertEquals(0, throttle().remainingLockMillis());
	}

	/**
	 * Someone who mistypes their own passcode is the common case, and they get
	 * a few tries before any of this is felt.
	 */
	@Test
	public void theFirstFewWrongAnswersAreNotDelayed() {
		for (int i = 0; i < FREE_ATTEMPTS; i++) {
			throttle().recordFailure();
			assertFalse("Locked after " + (i + 1) + " wrong answers",
					throttle().isLocked());
		}
	}

	@Test
	public void theDelayDoublesWithEachFurtherWrongAnswer() {
		for (int i = 0; i < FREE_ATTEMPTS; i++) throttle().recordFailure();

		throttle().recordFailure();
		assertEquals(FIRST_DELAY_MS, throttle().remainingLockMillis());

		throttle().recordFailure();
		assertEquals(FIRST_DELAY_MS * 2, throttle().remainingLockMillis());

		throttle().recordFailure();
		assertEquals(FIRST_DELAY_MS * 4, throttle().remainingLockMillis());
	}

	@Test
	public void theDelayStopsGrowingAtItsCap() {
		for (int i = 0; i < 200; i++) throttle().recordFailure();
		assertEquals(MAX_DELAY_MS, throttle().remainingLockMillis());
	}

	/**
	 * The counter is the whole defence, so it has to outlive the process. Each
	 * call above already builds a new throttle; this says why.
	 */
	@Test
	public void theDelaySurvivesTheCalculatorBeingKilled() {
		failUntilLocked();
		PasscodeThrottle afterRestart = new PasscodeThrottle(store, clock);
		assertTrue("Killing the calculator cleared the delay",
				afterRestart.isLocked());
		assertEquals(FIRST_DELAY_MS, afterRestart.remainingLockMillis());
	}

	@Test
	public void theDelayEndsWhenItHasBeenWaitedOut() {
		failUntilLocked();

		clock.advance(FIRST_DELAY_MS - 1);
		assertTrue(throttle().isLocked());

		clock.advance(1);
		assertFalse(throttle().isLocked());
	}

	/**
	 * Having waited it out does not buy a fresh set of free attempts: the next
	 * wrong answer costs twice what the last one did.
	 */
	@Test
	public void waitingOutADelayDoesNotResetTheCount() {
		failUntilLocked();
		clock.advance(FIRST_DELAY_MS);
		assertFalse(throttle().isLocked());

		throttle().recordFailure();
		assertEquals(FIRST_DELAY_MS * 2, throttle().remainingLockMillis());
	}

	@Test
	public void theCorrectPasscodeClearsTheDelay() {
		for (int i = 0; i < FREE_ATTEMPTS + 3; i++) throttle().recordFailure();
		assertTrue(throttle().isLocked());

		throttle().recordSuccess();
		assertFalse(throttle().isLocked());

		// and the count starts again from nothing
		throttle().recordFailure();
		assertFalse(throttle().isLocked());
	}

	/**
	 * Whoever is guessing also holds the device, and the wall clock is two taps
	 * away in Settings. Moving it forward past the deadline must not retire it.
	 */
	@Test
	public void windingTheClockForwardDoesNotSkipTheWait() {
		failUntilLocked();

		clock.wall += MAX_DELAY_MS;
		assertTrue("Setting the clock forward cleared the delay",
				throttle().isLocked());
		assertEquals(FIRST_DELAY_MS, throttle().remainingLockMillis());

		// and it still ends when the time has actually passed
		clock.advance(FIRST_DELAY_MS);
		assertFalse(throttle().isLocked());
	}

	@Test
	public void windingTheClockBackDoesNotSkipTheWait() {
		failUntilLocked();

		clock.wall -= 365L * 24 * 60 * 60 * 1000;
		assertTrue("Setting the clock back cleared the delay",
				throttle().isLocked());
		assertEquals(FIRST_DELAY_MS, throttle().remainingLockMillis());

		clock.advance(FIRST_DELAY_MS);
		assertFalse(throttle().isLocked());
	}

	/**
	 * The monotonic clock restarts at every boot, so a deadline held against it
	 * would otherwise outlast the wait by however long the phone had been up.
	 */
	@Test
	public void aRebootDoesNotSkipTheWaitOrOutlastIt() {
		failUntilLocked();

		clock.reboot();
		assertTrue("Rebooting cleared the delay", throttle().isLocked());
		assertEquals("Rebooting held the lock for longer than the delay",
				FIRST_DELAY_MS, throttle().remainingLockMillis());

		clock.advance(FIRST_DELAY_MS);
		assertFalse("Rebooting locked the owner out beyond the delay",
				throttle().isLocked());
	}

	/**
	 * Neither clock alone survives both moves, which is why the longer of the
	 * two wins.
	 */
	@Test
	public void rebootingAndThenChangingTheClockDoesNotSkipTheWait() {
		failUntilLocked();

		clock.reboot();
		clock.wall += MAX_DELAY_MS;
		assertTrue("A reboot plus a clock change cleared the delay",
				throttle().isLocked());
		assertEquals(FIRST_DELAY_MS, throttle().remainingLockMillis());

		clock.advance(FIRST_DELAY_MS);
		assertFalse(throttle().isLocked());
	}

	/**
	 * States written before the monotonic deadline was recorded hold only a
	 * wall-clock one. They are honoured as written rather than treated as
	 * damaged, which would hand an hour's wait to someone who had done nothing
	 * wrong.
	 */
	@Test
	public void aStateWrittenWithoutAMonotonicDeadlineIsStillHonoured() {
		store.value = SecureValue.present("0:0");
		assertFalse("An existing cleared count was read as tampering",
				throttle().isLocked());

		store.value = SecureValue.present(
				(FREE_ATTEMPTS + 1) + ":" + (START + FIRST_DELAY_MS));
		assertEquals(FIRST_DELAY_MS, throttle().remainingLockMillis());

		clock.advance(FIRST_DELAY_MS);
		assertFalse(throttle().isLocked());
	}

	/**
	 * Destroying the count must be worse than leaving it alone, or destroying
	 * it becomes the attack.
	 */
	@Test
	public void anUnreadableCountCostsTheLongestDelay() {
		store.value = SecureValue.unreadable("decryption failed");
		assertEquals(MAX_DELAY_MS, throttle().remainingLockMillis());
	}

	@Test
	public void aMalformedCountCostsTheLongestDelay() {
		for (String malformed : new String[] {"", "3", "3:4:5:6", "x:4", "3:x",
				"3:4:x", "-1:4", "3:-4", "3:4:-5"}) {
			store.value = SecureValue.present(malformed);
			assertEquals("Accepted a malformed count: " + malformed,
					MAX_DELAY_MS, throttle().remainingLockMillis());
		}
	}

	/**
	 * ...but it must not be permanent. Someone whose own Keystore key was
	 * dropped has to be able to get back in.
	 */
	@Test
	public void anUnreadableCountDoesNotLockTheOwnerOutForGood() {
		store.value = SecureValue.unreadable("decryption failed");
		assertTrue(throttle().isLocked());

		clock.advance(MAX_DELAY_MS);
		assertFalse("An unreadable count locked the app permanently",
				throttle().isLocked());
	}

	private static class FakeStore implements PasscodeThrottle.Store {

		private SecureValue value = SecureValue.absent();

		@Override
		public SecureValue read() {
			return value;
		}

		@Override
		public void write(String state) {
			value = SecureValue.present(state);
		}
	}

	private static class FakeClock implements PasscodeThrottle.Clock {

		private long wall = START;
		private long elapsed = UPTIME;

		@Override
		public long wallClockMillis() {
			return wall;
		}

		@Override
		public long elapsedRealtimeMillis() {
			return elapsed;
		}

		/** Time actually passing moves both. */
		void advance(long ms) {
			wall += ms;
			elapsed += ms;
		}

		/** Only the monotonic clock restarts. */
		void reboot() {
			elapsed = 0;
		}
	}
}

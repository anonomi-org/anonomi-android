package org.anonomi.android.panic;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * A wipe is triggered by someone who is about to lose the device, so the
 * process running it can be stopped at any moment. These cover what has to
 * have happened by the time each step returns, rather than what happens if
 * everything is allowed to run to the end.
 */
public class PanicWipeTest {

	private static final String DESTROY_KEY = "destroyKey";
	private static final String NOTIFY = "notifyPanicContacts";
	private static final String WAIT = "waitForDelivery";
	private static final String DELETE = "deleteRemainingData";

	private final RecordingSteps steps = new RecordingSteps();
	private final RecordingMarker marker = new RecordingMarker();

	/**
	 * Runs the work it is given, like a thread that is allowed to finish.
	 */
	private static final Executor RUNS = Runnable::run;

	/**
	 * Drops the work it is given, standing in for a process that is killed
	 * before the work gets to run.
	 */
	private static final Executor NEVER_RUNS = task -> {
	};

	/**
	 * The property the whole design rests on. Once the trigger has been
	 * handled, the account has to be beyond recovery even if nothing else
	 * ever runs - not scheduled to become so.
	 */
	@Test
	public void theAccountIsUnrecoverableOnceTheTriggerHasBeenHandled() {
		new PanicWipe(steps, marker, NEVER_RUNS).begin(() -> {
		});
		assertTrue("The key was still on disk after the panic trigger was " +
						"handled, so a process killed at that moment would " +
						"have left the account intact",
				steps.calls.contains(DESTROY_KEY));
	}

	/**
	 * Telling contacts writes to the database. On a loaded device that is
	 * exactly when an ANR would cost someone the wipe.
	 */
	@Test
	public void contactsAreNotNotifiedOnTheCallingThread() {
		new PanicWipe(steps, marker, NEVER_RUNS).begin(() -> {
		});
		assertFalse("Contacts were notified on the thread that handled the " +
						"trigger, which is the UI thread",
				steps.calls.contains(NOTIFY));
	}

	/**
	 * Notifying is best effort and delivery depends on the network long after
	 * this returns, so it must not come first and delay the one step that
	 * cannot be interrupted.
	 */
	@Test
	public void theKeyIsDestroyedBeforeContactsAreNotified() {
		new PanicWipe(steps, marker, RUNS).begin(() -> {
		});
		assertTrue(steps.calls.contains(DESTROY_KEY));
		assertTrue(steps.calls.contains(NOTIFY));
		assertTrue("Contacts were notified before the key was destroyed, so " +
						"the wipe waited on the notification",
				steps.calls.indexOf(DESTROY_KEY) <
						steps.calls.indexOf(NOTIFY));
	}

	/**
	 * The wipe cannot be contingent on the notification working - a failure
	 * to reach contacts is not a reason to leave the data on the device.
	 */
	@Test
	public void aFailureToNotifyContactsDoesNotStopTheWipe() {
		steps.failOnNotify = true;
		new PanicWipe(steps, marker, RUNS).begin(() -> {
		});
		assertTrue(steps.calls.contains(DESTROY_KEY));
		assertTrue(steps.calls.contains(DELETE));
	}

	/**
	 * A wipe that is killed part way through has to be recognisable
	 * afterwards, or it leaves a half-deleted account behind for good.
	 */
	@Test
	public void anInterruptedWipeIsFinishedOnResume() {
		new PanicWipe(steps, marker, NEVER_RUNS).begin(() -> {
		});

		// Whatever else was lost, the next start has to be able to tell that
		// a wipe was under way.
		RecordingSteps afterRestart = new RecordingSteps();
		assertTrue("An interrupted wipe left nothing behind to say it had " +
						"started, so it could never be finished",
				new PanicWipe(afterRestart, marker, RUNS)
						.resumeIfInterrupted());
		assertTrue(afterRestart.calls.contains(DELETE));
	}

	/**
	 * The interruption may have come before the key was destroyed, so
	 * resuming cannot assume that part is already done.
	 */
	@Test
	public void resumingDestroysTheKeyAgain() {
		marker.set = true;
		new PanicWipe(steps, marker, RUNS).resumeIfInterrupted();
		assertTrue(steps.calls.contains(DESTROY_KEY));
	}

	/**
	 * A finished wipe must not leave anything that makes the next start
	 * delete a newly created account.
	 */
	@Test
	public void aCompletedWipeLeavesNothingToResume() {
		new PanicWipe(steps, marker, RUNS).begin(() -> {
		});
		assertFalse(marker.set);
		assertFalse(new PanicWipe(new RecordingSteps(), marker, RUNS)
				.resumeIfInterrupted());
	}

	@Test
	public void anOrdinaryStartDoesNothing() {
		assertFalse(new PanicWipe(steps, marker, RUNS).resumeIfInterrupted());
		assertTrue(steps.calls.isEmpty());
	}

	/**
	 * Contacts were told when the trigger was used, or the chance has gone.
	 * Resuming must not send a second round to whoever is left.
	 */
	@Test
	public void resumingDoesNotNotifyContactsAgain() {
		marker.set = true;
		new PanicWipe(steps, marker, RUNS).resumeIfInterrupted();
		assertFalse(steps.calls.contains(NOTIFY));
	}

	/**
	 * Queueing a message only puts it in the database. If that database is
	 * deleted in the next breath the message never reaches anyone, so it is
	 * given a moment first - which is affordable only because the key has
	 * already gone by then.
	 */
	@Test
	public void queuedMessagesAreGivenTimeBeforeTheDataIsDeleted() {
		new PanicWipe(steps, marker, RUNS).begin(() -> {
		});
		assertTrue("Nothing was given a chance to be delivered before the " +
				"database holding it was deleted", steps.calls.contains(WAIT));
		assertTrue(steps.calls.indexOf(DESTROY_KEY) <
				steps.calls.indexOf(WAIT));
		assertTrue(steps.calls.indexOf(NOTIFY) < steps.calls.indexOf(WAIT));
		assertTrue(steps.calls.indexOf(WAIT) < steps.calls.indexOf(DELETE));
	}

	/**
	 * Most people have no panic contacts. Waiting for a delivery that was
	 * never queued would only keep their data on the device for longer.
	 */
	@Test
	public void nothingQueuedMeansNothingIsWaitedFor() {
		steps.anythingQueued = false;
		new PanicWipe(steps, marker, RUNS).begin(() -> {
		});
		assertFalse(steps.calls.contains(WAIT));
		assertTrue(steps.calls.contains(DELETE));
	}

	@Test
	public void aFailureToNotifyIsNotWaitedFor() {
		steps.failOnNotify = true;
		new PanicWipe(steps, marker, RUNS).begin(() -> {
		});
		assertFalse(steps.calls.contains(WAIT));
	}

	@Test
	public void completionIsReportedOnlyOnceNothingIsLeft() {
		List<String> calls = steps.calls;
		new PanicWipe(steps, marker, RUNS).begin(() -> calls.add("onComplete"));
		assertEquals(DELETE, calls.get(calls.size() - 2));
		assertEquals("onComplete", calls.get(calls.size() - 1));
	}

	private static class RecordingSteps implements PanicWipe.Steps {

		private final List<String> calls = new ArrayList<>();
		private boolean failOnNotify = false;
		private boolean anythingQueued = true;

		@Override
		public void destroyKey() {
			calls.add(DESTROY_KEY);
		}

		@Override
		public boolean notifyPanicContacts() {
			calls.add(NOTIFY);
			if (failOnNotify) throw new RuntimeException("no contacts");
			return anythingQueued;
		}

		@Override
		public void waitForDelivery() {
			calls.add(WAIT);
		}

		@Override
		public void deleteRemainingData() {
			calls.add(DELETE);
		}
	}

	private static class RecordingMarker implements PanicWipe.Marker {

		private boolean set = false;

		@Override
		public void set() {
			set = true;
		}

		@Override
		public boolean isSet() {
			return set;
		}

		@Override
		public void clear() {
			set = false;
		}
	}
}

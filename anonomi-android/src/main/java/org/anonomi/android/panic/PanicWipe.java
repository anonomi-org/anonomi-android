package org.anonomi.android.panic;

import java.util.concurrent.Executor;
import java.util.logging.Logger;

import static java.util.logging.Level.WARNING;
import static java.util.logging.Logger.getLogger;
import static org.anonchatsecure.bramble.util.LogUtils.logException;

/**
 * Runs an account wipe in an order that survives being interrupted.
 * <p>
 * A wipe is triggered by someone who is about to lose control of the device,
 * so the process running it can be backgrounded, force-stopped or reclaimed at
 * any moment, and no amount of care here can prevent that. What can be chosen
 * is the order. Destroying the key the account is encrypted with is a couple
 * of small file deletions, so it runs first and on the calling thread; once it
 * has returned, being killed costs an untidy data directory rather than the
 * account.
 * <p>
 * Everything slow happens after that point, where an interruption is
 * survivable: telling the chosen contacts, shutting the services down, and
 * deleting what is left on disk. A marker written before the first step lets
 * {@link #resumeIfInterrupted()} finish the job at the next start.
 */
public class PanicWipe {

	private static final Logger LOG = getLogger(PanicWipe.class.getName());

	/**
	 * The individual things a wipe does, kept behind an interface so their
	 * order can be tested without a device.
	 */
	public interface Steps {

		/**
		 * Deletes the key the account's data is encrypted with. Cheap, and
		 * the point after which the account cannot be recovered. Safe to call
		 * more than once.
		 */
		void destroyKey();

		/**
		 * Tells the contacts chosen for it that the trigger was used.
		 *
		 * @return true if anything was queued that could still be delivered.
		 */
		boolean notifyPanicContacts();

		/**
		 * Gives whatever was queued its chance to leave the device before the
		 * database holding it is deleted. Only worth doing once the key has
		 * gone, which is what makes the wait affordable: an interruption
		 * during it still leaves an account that cannot be recovered.
		 */
		void waitForDelivery();

		/**
		 * Shuts the services down and deletes the data the key protected.
		 * Blocks until it is done, and is safe to call again after an
		 * interrupted attempt.
		 */
		void deleteRemainingData();
	}

	/**
	 * Records that a wipe has started, so one that did not finish can be
	 * recognised at the next start.
	 */
	public interface Marker {

		void set();

		boolean isSet();

		void clear();
	}

	private final Steps steps;
	private final Marker marker;
	private final Executor executor;

	public PanicWipe(Steps steps, Marker marker, Executor executor) {
		this.steps = steps;
		this.marker = marker;
		this.executor = executor;
	}

	/**
	 * Starts a wipe. The caller is on the UI thread, so only the part that
	 * has to happen immediately runs there.
	 *
	 * @param onComplete run once nothing of the account is left.
	 */
	public void begin(Runnable onComplete) {
		marker.set();
		steps.destroyKey();
		executor.execute(() -> {
			// Nothing queued means nothing to wait for, and the sooner the
			// rest of it is gone the better.
			if (notifyQuietly()) steps.waitForDelivery();
			finish(onComplete);
		});
	}

	/**
	 * Finishes a wipe that did not run to completion, if there was one.
	 * <p>
	 * Contacts are not told again: they were told, or the chance to tell them
	 * has gone.
	 *
	 * @return true if an unfinished wipe was found.
	 */
	public boolean resumeIfInterrupted() {
		if (!marker.isSet()) return false;
		// The interruption may have come before the key was destroyed.
		steps.destroyKey();
		finish(() -> {
		});
		return true;
	}

	private boolean notifyQuietly() {
		try {
			return steps.notifyPanicContacts();
		} catch (RuntimeException e) {
			// Contacts are told as a courtesy. Failing to tell them is not a
			// reason to leave the data on the device.
			logException(LOG, WARNING, e);
			return false;
		}
	}

	private void finish(Runnable onComplete) {
		steps.deleteRemainingData();
		marker.clear();
		onComplete.run();
	}
}

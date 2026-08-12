package org.anonomi.android.panic;

import android.content.Context;

import org.anonchatsecure.bramble.api.account.AccountManager;

/**
 * Finishes a wipe that an earlier run of the app did not get to the end of.
 */
public class InterruptedPanicWipe {

	/**
	 * Deletes what is left of an account whose wipe was interrupted, if there
	 * is one.
	 * <p>
	 * This runs during startup, on the thread that is starting the app, and
	 * deliberately so: it has to finish before anything can offer to create
	 * an account, or it could delete the new one instead of the old one.
	 * There is nothing to do on an ordinary start beyond reading a
	 * preference.
	 */
	public static void finish(Context ctx, AccountManager accountManager) {
		new PanicWipe(new ResumeSteps(accountManager),
				new PanicWipeMarker(ctx), Runnable::run)
				.resumeIfInterrupted();
	}

	private static class ResumeSteps implements PanicWipe.Steps {

		private final AccountManager accountManager;

		ResumeSteps(AccountManager accountManager) {
			this.accountManager = accountManager;
		}

		@Override
		public void destroyKey() {
			accountManager.deleteDatabaseKey();
		}

		@Override
		public boolean notifyPanicContacts() {
			// Not reached when resuming: contacts were told when the trigger
			// was used, or the chance to tell them has gone.
			return false;
		}

		@Override
		public void waitForDelivery() {
			// Nothing was queued, so there is nothing to wait for.
		}

		@Override
		public void deleteRemainingData() {
			// Nothing has been started yet this early in the app's life, so
			// there is nothing to shut down first.
			accountManager.deleteAccount();
		}
	}
}

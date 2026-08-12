package org.anonomi.android.panic;

import org.anonomi.android.util.SecureValue;

import androidx.annotation.Nullable;

import static org.anonomi.android.panic.PanicSequenceDetector.ACTION_DELETE_ACCOUNT;
import static org.anonomi.android.panic.PanicSequenceDetector.ACTION_SHOW_DIALOG;
import static org.anonomi.android.panic.PanicSequenceDetector.ACTION_SIGN_OUT;

/**
 * Decides which panic action to run.
 * <p>
 * There are two entry points into panic - {@link PanicDialogHelper}, which
 * every in-app trigger goes through, and {@link PanicResponderActivity},
 * which reads the setting again when it is started without an explicit
 * choice. They used to make this decision separately, and both got it wrong
 * in the same way. It lives here once, and it holds no Android state so it
 * can be tested directly.
 */
public final class PanicActionPolicy {

	/**
	 * Used when nothing has ever been configured. Someone who has not chosen
	 * an action has not asked for their account to be deleted.
	 */
	static final String DEFAULT_ACTION = ACTION_SIGN_OUT;

	/**
	 * Used when an action is configured but cannot be read.
	 * <p>
	 * We know the user configured something and we know we cannot tell what.
	 * Of the two ways to be wrong, resolving to sign-out leaves an intact
	 * account on a device whose owner has just triggered panic, while
	 * resolving to deletion costs an account to someone who wanted a
	 * sign-out. Panic only ever runs because someone deliberately performed
	 * the trigger, so this is not a failure mode that fires on its own, and
	 * for this app the first way of being wrong is the more dangerous one.
	 * <p>
	 * The other half of that bargain is that an unreadable action has to be
	 * visible before it is acted on - see
	 * {@link PanicPreferencesFragment}, which flags it in settings.
	 */
	static final String UNREADABLE_ACTION = ACTION_DELETE_ACCOUNT;

	private PanicActionPolicy() {
	}

	/**
	 * Resolves the action to run.
	 *
	 * @param intentOverride an action chosen explicitly for this trigger, for
	 * example by the panic dialog. Wins over stored configuration.
	 * @param stored the configured action as read from secure storage.
	 */
	public static String resolve(@Nullable String intentOverride,
			SecureValue stored) {
		if (intentOverride != null) {
			return isKnownAction(intentOverride)
					? intentOverride : UNREADABLE_ACTION;
		}
		// Absent is the only outcome that means "not configured".
		if (stored.isAbsent()) return DEFAULT_ACTION;
		if (stored.isPresent() && isKnownAction(stored.get())) {
			return stored.get();
		}
		// Either the value would not decrypt, or it decrypted to something
		// that is not an action we recognise. Both mean a configured setting
		// we cannot honour, and neither means the setting was never made.
		return UNREADABLE_ACTION;
	}

	public static boolean isKnownAction(@Nullable String action) {
		return ACTION_SIGN_OUT.equals(action) ||
				ACTION_DELETE_ACCOUNT.equals(action) ||
				ACTION_SHOW_DIALOG.equals(action);
	}
}

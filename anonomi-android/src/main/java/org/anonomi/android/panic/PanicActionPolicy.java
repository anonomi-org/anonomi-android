package org.anonomi.android.panic;

import org.anonomi.android.util.SecureValue;

import androidx.annotation.Nullable;

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
		if (intentOverride != null) return intentOverride;
		if (stored.isPresent()) return stored.get();
		// TODO: an unreadable action is not an unconfigured one, but it is
		// currently treated as one. Preserved here so that moving the
		// decision into this class changes no behaviour; fixed in the
		// following commit.
		return DEFAULT_ACTION;
	}
}

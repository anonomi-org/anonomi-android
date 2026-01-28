package org.anonomi.android.login;

import android.content.Context;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.anonomi.R;
import org.briarproject.nullsafety.NotNullByDefault;

import androidx.appcompat.app.AlertDialog;

import static org.anonomi.android.util.UiUtils.getDialogIcon;

@NotNullByDefault
class LoginUtils {

	static AlertDialog createKeyStrengthenerErrorDialog(Context ctx) {
		MaterialAlertDialogBuilder builder =
				new MaterialAlertDialogBuilder(ctx, R.style.AnonDialogTheme);
		builder.setIcon(getDialogIcon(ctx, R.drawable.alerts_and_states_error));
		builder.setTitle(R.string.dialog_title_cannot_check_password);
		builder.setMessage(R.string.dialog_message_cannot_check_password);
		builder.setPositiveButton(R.string.ok, null);
		return builder.create();
	}
}

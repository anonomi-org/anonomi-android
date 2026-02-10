package org.anonomi.android.panic;

import android.app.Activity;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.anonomi.R;
import org.anonomi.android.util.SecurePrefsManager;

import static org.anonomi.android.panic.PanicSequenceDetector.ACTION_DELETE_ACCOUNT;
import static org.anonomi.android.panic.PanicSequenceDetector.ACTION_SHOW_DIALOG;
import static org.anonomi.android.panic.PanicSequenceDetector.PREF_KEY_PANIC_ACTION;

public class PanicDialogHelper {

	public static void onPanicTriggered(Activity activity) {
		SecurePrefsManager securePrefs = new SecurePrefsManager(activity);
		String action = securePrefs.getDecrypted(PREF_KEY_PANIC_ACTION);
		if (action == null) action = PanicSequenceDetector.ACTION_SIGN_OUT;

		if (ACTION_SHOW_DIALOG.equals(action)) {
			showPanicDialog(activity);
		} else {
			showStatusAndLaunch(activity, action);
		}
	}

	private static void showStatusAndLaunch(Activity activity, String action) {
		int messageRes = ACTION_DELETE_ACCOUNT.equals(action)
				? R.string.panic_status_deleting
				: R.string.panic_status_signing_out;

		new MaterialAlertDialogBuilder(activity, R.style.AnonDialogTheme)
				.setTitle(R.string.panic_dialog_title)
				.setMessage(messageRes)
				.setCancelable(false)
				.show();

		launchPanicResponder(activity, action);
	}

	private static void launchPanicResponder(Activity activity,
			String actionOverride) {
		Intent i = new Intent(activity, PanicResponderActivity.class);
		i.setAction(PanicResponderActivity.ACTION_INTERNAL_PANIC);
		if (actionOverride != null) {
			i.putExtra(PanicResponderActivity.EXTRA_PANIC_ACTION,
					actionOverride);
		}
		i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		activity.startActivity(i);
	}

	private static void showPanicDialog(Activity activity) {
		String[] items = {
				activity.getString(R.string.panic_option_sign_out),
				activity.getString(R.string.panic_option_delete_account),
				activity.getString(R.string.cancel)
		};

		// Resolve theme-aware colors
		TypedValue typedValue = new TypedValue();
		activity.getTheme().resolveAttribute(
				android.R.attr.colorPrimary, typedValue, true);
		int primary = typedValue.data;

		// textColorPrimary is a ColorStateList, resolve via TypedArray
		TypedArray ta = activity.obtainStyledAttributes(
				new int[]{android.R.attr.textColorPrimary});
		int textColor = ta.getColor(0, Color.WHITE);
		ta.recycle();

		int red = ContextCompat.getColor(activity,
				R.color.briar_button_text_negative);

		float density = activity.getResources()
				.getDisplayMetrics().density;

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(
				activity, android.R.layout.simple_list_item_1, items) {
			@Override
			public android.view.View getView(int position,
					android.view.View convertView,
					android.view.ViewGroup parent) {
				TextView tv = (TextView) super.getView(
						position, convertView, parent);
				tv.setTextSize(20);
				int hPad = (int) (24 * density);
				int vPad = (int) (16 * density);
				tv.setPadding(hPad, vPad, hPad, vPad);
				if (position == 0) {
					tv.setTextColor(primary);
				} else if (position == 1) {
					tv.setTextColor(red);
				} else if (position == 2) {
					tv.setTextColor(textColor);
				}
				return tv;
			}
		};

		// Custom title: larger and bold so it's clearly distinct from options
		TextView titleView = new TextView(activity);
		titleView.setText(R.string.panic_dialog_title);
		titleView.setTextSize(24);
		titleView.setTypeface(null, Typeface.BOLD);
		titleView.setTextColor(textColor);
		int titleHPad = (int) (24 * density);
		int titleTopPad = (int) (24 * density);
		int titleBottomPad = (int) (8 * density);
		titleView.setPadding(titleHPad, titleTopPad, titleHPad, titleBottomPad);

		MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(
				activity, R.style.AnonDialogTheme);
		builder.setCustomTitle(titleView);
		builder.setAdapter(adapter, (dialog, which) -> {
			switch (which) {
				case 0:
					showStatusAndLaunch(activity,
							PanicSequenceDetector.ACTION_SIGN_OUT);
					break;
				case 1:
					showStatusAndLaunch(activity,
							ACTION_DELETE_ACCOUNT);
					break;
				default:
					break;
			}
		});
		builder.setOnCancelListener(dialog -> {});
		builder.show();
	}
}

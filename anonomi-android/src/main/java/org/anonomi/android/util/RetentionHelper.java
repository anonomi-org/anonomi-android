package org.anonomi.android.util;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.anonomi.R;

import static java.util.concurrent.TimeUnit.DAYS;

public class RetentionHelper {

	private static final long[] DURATIONS = {
			DAYS.toMillis(7),
			DAYS.toMillis(30),
			DAYS.toMillis(90),
			DAYS.toMillis(180),
			-1
	};

	private static final int[] LABELS = {
			R.string.retention_7_days,
			R.string.retention_30_days,
			R.string.retention_90_days,
			R.string.retention_180_days,
			R.string.retention_keep_forever
	};

	public interface RetentionCallback {
		void onRetentionSelected(long durationMs);
	}

	public static void showRetentionDialog(Context context,
			long currentDuration, RetentionCallback callback) {
		String[] labels = new String[LABELS.length];
		int selected = LABELS.length - 1; // default: keep forever
		for (int i = 0; i < LABELS.length; i++) {
			labels[i] = context.getString(LABELS[i]);
			if (DURATIONS[i] == currentDuration) {
				selected = i;
			}
		}

		// Build a custom title view with title + info subtitle
		View titleView = LayoutInflater.from(context)
				.inflate(R.layout.dialog_retention_title, null);
		TextView titleText = titleView.findViewById(R.id.retentionTitle);
		TextView infoText = titleView.findViewById(R.id.retentionInfo);
		titleText.setText(R.string.message_retention_title);
		infoText.setText(R.string.message_retention_info);

		new MaterialAlertDialogBuilder(context, R.style.AnonDialogTheme)
				.setCustomTitle(titleView)
				.setSingleChoiceItems(labels, selected, (dialog, which) -> {
					callback.onRetentionSelected(DURATIONS[which]);
					dialog.dismiss();
				})
				.setNegativeButton(R.string.cancel, null)
				.show();
	}
}

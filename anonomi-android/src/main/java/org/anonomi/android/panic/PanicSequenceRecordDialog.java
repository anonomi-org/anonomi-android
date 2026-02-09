package org.anonomi.android.panic;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.anonomi.R;

import java.util.ArrayList;
import java.util.List;

public class PanicSequenceRecordDialog extends DialogFragment {

	private static final long SHORT_PRESS_MAX_MS = 500;
	private static final int MIN_STEPS = 3;

	private final List<PanicSequenceDetector.Step> recordedSteps =
			new ArrayList<>();
	private TextView stepsDisplay;
	private long keyDownTime = 0;
	private int keyDownCode = 0;

	private RecordCallback callback;

	public interface RecordCallback {
		void onSequenceRecorded(List<PanicSequenceDetector.Step> steps);
	}

	public void setCallback(RecordCallback callback) {
		this.callback = callback;
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
		View view = LayoutInflater.from(requireContext())
				.inflate(R.layout.dialog_record_panic_sequence, null);

		stepsDisplay = view.findViewById(R.id.steps_display);
		MaterialButton clearButton = view.findViewById(R.id.button_clear);
		MaterialButton cancelButton = view.findViewById(R.id.button_cancel);
		MaterialButton saveButton = view.findViewById(R.id.button_save);

		clearButton.setOnClickListener(v -> {
			recordedSteps.clear();
			updateDisplay();
		});

		cancelButton.setOnClickListener(v -> dismiss());

		saveButton.setOnClickListener(v -> {
			if (recordedSteps.size() < MIN_STEPS) {
				Toast.makeText(requireContext(),
						R.string.panic_sequence_too_short, Toast.LENGTH_SHORT)
						.show();
				return;
			}
			if (callback != null) {
				callback.onSequenceRecorded(
						new ArrayList<>(recordedSteps));
			}
			dismiss();
		});

		updateDisplay();

		MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(
				requireContext(), R.style.AnonDialogTheme);
		builder.setTitle(R.string.panic_record_dialog_title);
		builder.setView(view);

		Dialog dialog = builder.create();

		// Disable the panic detector while recording
		PanicSequenceDetector.getInstance().setEnabled(false);

		dialog.setOnKeyListener((d, keyCode, event) -> {
			if (keyCode != KeyEvent.KEYCODE_VOLUME_UP &&
					keyCode != KeyEvent.KEYCODE_VOLUME_DOWN) {
				return false;
			}

			if (event.getAction() == KeyEvent.ACTION_DOWN &&
					event.getRepeatCount() == 0) {
				keyDownTime = event.getEventTime();
				keyDownCode = keyCode;
				return true;
			}

			if (event.getAction() == KeyEvent.ACTION_UP) {
				if (keyCode != keyDownCode) return true;

				long pressDuration = event.getEventTime() - keyDownTime;
				boolean isLong = pressDuration >= SHORT_PRESS_MAX_MS;

				recordedSteps.add(new PanicSequenceDetector.Step(
						keyCode,
						isLong ? PanicSequenceDetector.PressType.LONG
								: PanicSequenceDetector.PressType.SHORT
				));
				updateDisplay();
				return true;
			}

			return true;
		});

		return dialog;
	}

	@Override
	public void onDismiss(@NonNull DialogInterface dialog) {
		super.onDismiss(dialog);
		// Re-enable the panic detector
		if (getContext() != null) {
			PanicSequenceDetector.getInstance().loadSequence(getContext());
		}
	}

	private void updateDisplay() {
		if (recordedSteps.isEmpty()) {
			stepsDisplay.setText(R.string.panic_sequence_not_set);
		} else {
			stepsDisplay.setText(
					PanicSequenceDetector.toDisplayString(recordedSteps));
		}
	}
}

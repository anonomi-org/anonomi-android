package org.anonomi.android.panic;

import android.content.Context;
import android.view.KeyEvent;

import org.anonomi.android.util.SecurePrefsManager;

import java.util.ArrayList;
import java.util.List;

public class PanicSequenceDetector {

	public static final String PREF_KEY_PANIC_SEQUENCE = "pref_key_panic_sequence";
	public static final String PREF_KEY_PANIC_ACTION = "pref_key_panic_action";

	public static final String ACTION_SIGN_OUT = "sign_out";
	public static final String ACTION_DELETE_ACCOUNT = "delete_account";
	public static final String ACTION_SHOW_DIALOG = "show_dialog";

	private static final long SHORT_PRESS_MAX_MS = 500;
	private static final long STEP_TIMEOUT_MS = 3000;

	private static final PanicSequenceDetector INSTANCE =
			new PanicSequenceDetector();

	private List<Step> sequence = new ArrayList<>();
	private int currentStepIndex = 0;
	private long keyDownTime = 0;
	private int keyDownCode = 0;
	private boolean tracking = false;
	private boolean enabled = false;
	private PanicTriggerListener listener;

	private PanicSequenceDetector() {
	}

	public static PanicSequenceDetector getInstance() {
		return INSTANCE;
	}

	public void loadSequence(Context context) {
		try {
			SecurePrefsManager securePrefs = new SecurePrefsManager(context);
			String raw = securePrefs.getDecrypted(PREF_KEY_PANIC_SEQUENCE);
			if (raw != null && !raw.isEmpty()) {
				sequence = deserializeSequence(raw);
				enabled = sequence.size() >= 3;
			} else {
				sequence = new ArrayList<>();
				enabled = false;
			}
		} catch (Exception e) {
			sequence = new ArrayList<>();
			enabled = false;
		}
		reset();
	}

	public void setListener(PanicTriggerListener listener) {
		this.listener = listener;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	/**
	 * Process a key event. Returns true if the event was consumed
	 * (i.e. the detector is actively tracking a sequence).
	 */
	public boolean onKeyEvent(KeyEvent event) {
		if (!enabled || sequence.isEmpty()) return false;

		int keyCode = event.getKeyCode();
		if (keyCode != KeyEvent.KEYCODE_VOLUME_UP &&
				keyCode != KeyEvent.KEYCODE_VOLUME_DOWN) {
			reset();
			return false;
		}

		if (event.getAction() == KeyEvent.ACTION_DOWN &&
				event.getRepeatCount() == 0) {
			// Check timeout from last completed step
			if (tracking && currentStepIndex > 0) {
				long now = event.getEventTime();
				if (now - keyDownTime > STEP_TIMEOUT_MS) {
					reset();
				}
			}

			keyDownTime = event.getEventTime();
			keyDownCode = keyCode;

			// Check if this key matches the expected step's button
			Step expected = sequence.get(currentStepIndex);
			if (expected.button == keyCode) {
				tracking = true;
				return true;
			} else {
				reset();
				// Check if it matches the first step (restart detection)
				Step first = sequence.get(0);
				if (first.button == keyCode) {
					tracking = true;
					keyDownTime = event.getEventTime();
					keyDownCode = keyCode;
					return true;
				}
				return false;
			}
		}

		if (event.getAction() == KeyEvent.ACTION_UP && tracking) {
			if (keyCode != keyDownCode) {
				reset();
				return false;
			}

			long pressDuration = event.getEventTime() - keyDownTime;
			boolean isLong = pressDuration >= SHORT_PRESS_MAX_MS;
			Step expected = sequence.get(currentStepIndex);

			if (expected.button == keyCode && expected.pressType == (isLong ? PressType.LONG : PressType.SHORT)) {
				currentStepIndex++;
				if (currentStepIndex >= sequence.size()) {
					// Sequence complete
					PanicTriggerListener l = listener;
					reset();
					if (l != null) {
						l.onPanicTriggered();
					}
				}
				return true;
			} else {
				// Mismatch - reset and check if this could be start of new sequence
				reset();
				return true; // consume since we were tracking
			}
		}

		return tracking;
	}

	private void reset() {
		currentStepIndex = 0;
		tracking = false;
		keyDownTime = 0;
		keyDownCode = 0;
	}

	public static String serializeSequence(List<Step> steps) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < steps.size(); i++) {
			if (i > 0) sb.append(",");
			Step s = steps.get(i);
			sb.append(s.button == KeyEvent.KEYCODE_VOLUME_UP ? "U" : "D");
			sb.append(":");
			sb.append(s.pressType == PressType.SHORT ? "S" : "L");
		}
		return sb.toString();
	}

	public static List<Step> deserializeSequence(String raw) {
		List<Step> steps = new ArrayList<>();
		if (raw == null || raw.isEmpty()) return steps;
		String[] parts = raw.split(",");
		for (String part : parts) {
			String[] tokens = part.split(":");
			if (tokens.length != 2) continue;
			int button = tokens[0].equals("U") ?
					KeyEvent.KEYCODE_VOLUME_UP : KeyEvent.KEYCODE_VOLUME_DOWN;
			PressType pressType = tokens[1].equals("L") ?
					PressType.LONG : PressType.SHORT;
			steps.add(new Step(button, pressType));
		}
		return steps;
	}

	/**
	 * Convert a sequence to a human-readable display string.
	 */
	public static String toDisplayString(List<Step> steps) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < steps.size(); i++) {
			if (i > 0) sb.append("  ");
			Step s = steps.get(i);
			sb.append(s.button == KeyEvent.KEYCODE_VOLUME_UP ? "\u2191" : "\u2193");
			sb.append(s.pressType == PressType.SHORT ? "(short)" : "(long)");
		}
		return sb.toString();
	}

	public enum PressType {
		SHORT, LONG
	}

	public static class Step {
		public final int button;
		public final PressType pressType;

		public Step(int button, PressType pressType) {
			this.button = button;
			this.pressType = pressType;
		}
	}

	public interface PanicTriggerListener {
		void onPanicTriggered();
	}
}

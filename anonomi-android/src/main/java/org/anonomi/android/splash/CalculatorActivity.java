package org.anonomi.android.splash;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.anonomi.R;
import org.anonomi.android.util.Disguise;
import org.anonomi.android.panic.PanicDialogHelper;
import org.anonomi.android.panic.PanicResponderActivity;
import org.anonomi.android.panic.PanicSequenceDetector;
import org.anonomi.android.settings.SecurityFragment;
import org.anonomi.android.util.AndroidPasscodeClock;
import org.anonomi.android.util.PasscodeAttemptStore;
import org.anonomi.android.util.PasscodeHasher;
import org.anonomi.android.util.PasscodeThrottle;
import org.anonomi.android.util.SecurePrefsManager;
import org.anonomi.android.util.SecureValue;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static android.os.Build.VERSION.SDK_INT;
import static android.view.WindowManager.LayoutParams.FLAG_SECURE;
import static org.anonomi.android.TestingConstants.PREVENT_SCREENSHOTS;

public class CalculatorActivity extends AppCompatActivity {

	private StringBuilder rawExpression = new StringBuilder();
	private String currentDisplay = "";
	private TextView display;
	private double val1 = Double.NaN;
	private double val2;
	private char ACTION;

	/**
	 * Checking a passcode is deliberately slow, and one at a time is fast
	 * enough for someone pressing keys.
	 */
	private ExecutorService passcodeExecutor;

	private final char ADDITION = '+';
	private final char SUBTRACTION = '-';
	private final char MULTIPLICATION = '*';
	private final char DIVISION = '/';
	private final char MODULUS = '%';
	private final char NEGATE = '@';
	private final char EQUALS = '=';

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// The one screen that does not inherit these from BaseActivity, and
		// the one where a passcode is typed.
		if (PREVENT_SCREENSHOTS) getWindow().addFlags(FLAG_SECURE);
		if (SDK_INT >= 31) getWindow().setHideOverlayWindows(true);

		setContentView(R.layout.activity_calculator);
		Disguise.applyTaskDescription(this);

		passcodeExecutor = Executors.newSingleThreadExecutor();
		display = findViewById(R.id.input);

		int[] numberIds = {
				R.id.button0, R.id.button1, R.id.button2, R.id.button3, R.id.button4,
				R.id.button5, R.id.button6, R.id.button7, R.id.button8, R.id.button9
		};

		for (int id : numberIds) {
			findViewById(id).setOnClickListener(v -> {
				Button b = (Button) v;
				currentDisplay += b.getText();
				rawExpression.append(b.getText());
				display.setText(currentDisplay);
			});
		}

		findViewById(R.id.button_add).setOnClickListener(v -> onOperator(ADDITION));
		findViewById(R.id.button_sub).setOnClickListener(v -> onOperator(SUBTRACTION));
		findViewById(R.id.button_multi).setOnClickListener(v -> onOperator(MULTIPLICATION));
		findViewById(R.id.button_divide).setOnClickListener(v -> onOperator(DIVISION));
		findViewById(R.id.button_para1).setOnClickListener(v -> onOperator(MODULUS));
		findViewById(R.id.button_para2).setOnClickListener(v -> onOperator(NEGATE));

		findViewById(R.id.button_dot).setOnClickListener(v -> {
			currentDisplay += ".";
			rawExpression.append(".");
			display.setText(currentDisplay);
		});

		findViewById(R.id.button_clear).setOnClickListener(v -> clearEntry());

		Button equalButton = findViewById(R.id.button_equal);

		equalButton.setOnClickListener(v -> {
			try {
				operation();
				ACTION = EQUALS;
				if (val1 == (long) val1) {
					display.setText(String.format("%d", (long) val1));
				} else {
					display.setText(String.valueOf(val1));
				}
				currentDisplay = String.valueOf(val1);
			} catch (Exception e) {
				display.setText(getString(R.string.error));
			}
		});

		// Require long-press to check the passcode
		equalButton.setOnLongClickListener(v -> {
			if (rawExpression.length() > 0) {
				checkPasscode(rawExpression.toString());
			}
			return true;
		});
	}

	private void clearEntry() {
		currentDisplay = "";
		rawExpression.setLength(0);
		val1 = Double.NaN;
		val2 = Double.NaN;
		ACTION = ' ';
		display.setText("0");
	}

	private void onOperator(char op) {
		if (!currentDisplay.isEmpty()) {
			// Append operator to expression for passcode checking UX
			rawExpression.append(op);

			operation();
			ACTION = op;

			if (val1 == (long) val1) {
				display.setText(String.format("%d", (long) val1));
			} else {
				display.setText(String.valueOf(val1));
			}

			currentDisplay = "";
		} else {
			display.setText(getString(R.string.error));
		}
	}

	private void operation() {
		if (!Double.isNaN(val1)) {
			val2 = Double.parseDouble(currentDisplay);
			switch (ACTION) {
				case ADDITION:
					val1 = val1 + val2;
					break;
				case SUBTRACTION:
					val1 = val1 - val2;
					break;
				case MULTIPLICATION:
					val1 = val1 * val2;
					break;
				case DIVISION:
					val1 = val1 / val2;
					break;
				case MODULUS:
					val1 = val1 % val2;
					break;
				case NEGATE:
					val1 = -val1;
					break;
				case EQUALS:
					break;
			}
		} else {
			val1 = Double.parseDouble(currentDisplay);
		}
	}

	/**
	 * A wrong passcode, an unreadable one and one ignored after too many
	 * attempts all have to look the same from the outside, so nothing here
	 * reports anything.
	 */
	private void checkPasscode(String userExpression) {
		Context appContext = getApplicationContext();
		passcodeExecutor.execute(() -> {
			if (!accepts(appContext, userExpression)) return;
			runOnUiThread(() -> {
				if (!isFinishing() && !isDestroyed()) unlockApp();
			});
		});
	}

	private static boolean accepts(Context context, String userExpression) {
		SecurePrefsManager securePrefs = new SecurePrefsManager(context);
		PasscodeThrottle throttle = new PasscodeThrottle(
				new PasscodeAttemptStore(securePrefs,
						SecurityFragment.PREF_KEY_CALCULATOR_ATTEMPTS),
				new AndroidPasscodeClock());
		if (throttle.isLocked()) return false;

		SecurePrefsManager disguise = SecurePrefsManager.forDisguise(context);
		SecureValue stored =
				disguise.read(SecurityFragment.PREF_KEY_CALCULATOR_PASSCODE);

		// An unreadable passcode must not unlock, and must not say so. It is
		// not counted as a wrong answer either, because no answer would be
		// right.
		if (!stored.isPresent()) return false;
		String savedExpression = stored.get();
		if (savedExpression.isEmpty()) return false;

		if (!PasscodeHasher.verify(userExpression, savedExpression)) {
			throttle.recordFailure();
			return false;
		}
		if (PasscodeHasher.needsRehash(savedExpression)) {
			// The only moment an older stored form can be replaced without
			// asking for the passcode again.
			disguise.putEncrypted(
					SecurityFragment.PREF_KEY_CALCULATOR_PASSCODE,
					PasscodeHasher.hash(userExpression));
		}
		throttle.recordSuccess();
		return true;
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		if (PanicSequenceDetector.getInstance().onKeyEvent(event)) {
			return true;
		}
		return super.dispatchKeyEvent(event);
	}

	@Override
	protected void onResume() {
		super.onResume();
		PanicSequenceDetector.getInstance().loadSequence(this);
		PanicSequenceDetector.getInstance().setListener(() ->
				PanicDialogHelper.onPanicTriggered(CalculatorActivity.this));
	}

	@Override
	protected void onPause() {
		super.onPause();
		PanicSequenceDetector.getInstance().setListener(null);
		// A half-typed passcode must not be left on screen for whoever picks
		// the phone up next, nor in the recents thumbnail.
		clearEntry();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		passcodeExecutor.shutdownNow();
	}

	private void unlockApp() {
		Intent intent = new Intent(this, org.anonomi.android.navdrawer.NavDrawerActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		startActivity(intent);
		finish();
	}
}
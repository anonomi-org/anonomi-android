package org.anonomi.android.splash;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.anonomi.R;
import org.anonomi.android.panic.PanicResponderActivity;
import org.anonomi.android.panic.PanicSequenceDetector;
import org.anonomi.android.settings.SecurityFragment;
import org.anonomi.android.util.SecurePrefsManager;

public class CalculatorActivity extends AppCompatActivity {

	private StringBuilder rawExpression = new StringBuilder();
	private String currentDisplay = "";
	private TextView display;
	private double val1 = Double.NaN;
	private double val2;
	private char ACTION;

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
		setContentView(R.layout.activity_calculator);

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

		findViewById(R.id.button_clear).setOnClickListener(v -> {
			currentDisplay = "";
			rawExpression.setLength(0);
			val1 = Double.NaN;
			val2 = Double.NaN;
			ACTION = ' ';
			display.setText("0");
		});

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

	private void checkPasscode(String userExpression) {
		SecurePrefsManager securePrefs = new SecurePrefsManager(this);
		String savedExpression = securePrefs.getDecrypted(SecurityFragment.PREF_KEY_CALCULATOR_PASSCODE);

		if (savedExpression == null || savedExpression.isEmpty()) {
			return;
		}

		// Clean up both expressions: remove spaces
		String cleanedUserExpr = userExpression.replaceAll("\\s+", "");
		String cleanedSavedExpr = savedExpression.replaceAll("\\s+", "");

		if (cleanedSavedExpr.equals(cleanedUserExpr)) {
			unlockApp();
		}
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
		PanicSequenceDetector.getInstance().setListener(() -> {
			Intent i = new Intent(CalculatorActivity.this,
					PanicResponderActivity.class);
			i.setAction(PanicResponderActivity.ACTION_INTERNAL_PANIC);
			i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(i);
		});
	}

	@Override
	protected void onPause() {
		super.onPause();
		PanicSequenceDetector.getInstance().setListener(null);
	}

	private void unlockApp() {
		Toast.makeText(this, getString(R.string.unlocking_anonchat), Toast.LENGTH_SHORT).show();

		Intent intent = new Intent(this, org.anonomi.android.navdrawer.NavDrawerActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		startActivity(intent);
		finish();
	}
}
package org.anonomi.android.conversation;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.MenuItem;

import androidx.appcompat.app.AlertDialog;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.RadioButton;

import android.net.Uri;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;

import org.anonomi.R;
import org.anonomi.android.activity.ActivityComponent;
import org.anonomi.android.activity.BriarActivity;
import org.anonchatsecure.bramble.api.contact.ContactId;
import org.anonchatsecure.bramble.api.sync.GroupId;
import org.anonchatsecure.anonchat.api.messaging.MessagingManager;
import org.anonchatsecure.anonchat.api.messaging.PrivateMessage;
import org.anonchatsecure.anonchat.api.messaging.PrivateMessageFactory;
import org.anonchatsecure.anonchat.api.attachment.AttachmentHeader;

import org.anonomi.android.util.SecurePrefsManager;
import org.anonomi.android.util.SecureValue;

import androidx.annotation.StringRes;

import static org.anonomi.android.settings.MoneroSettingsFragment.PREF_KEY_MINOR_INDEX;
import static org.anonomi.android.settings.MoneroSettingsFragment.PREF_KEY_PRIMARY_ADDRESS;
import static org.anonomi.android.settings.MoneroSettingsFragment.PREF_KEY_PRIVATE_VIEW_KEY;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Collections;

import javax.inject.Inject;

import android.util.Log;

import org.anonomi.android.xmr.AnonMoneroUtils;
import org.anonomi.android.xmr.MoneroDecodedAddress;

import org.anonchatsecure.anonchat.api.autodelete.AutoDeleteManager;
import org.anonchatsecure.bramble.api.db.TransactionManager;

public class RequestXmrActivity extends BriarActivity {

	private static final int MIN_MINOR_INDEX = 1;
	private static final int MAX_MINOR_INDEX = 1_000_000;

	@Inject MessagingManager messagingManager;
	@Inject PrivateMessageFactory privateMessageFactory;

	private ProgressBar progressSpinner;
	private EditText amountEditText;
	private EditText messageEditText;
	private EditText rateEditText;
	private TextView conversionTextView;
	private Button generateButton;
	private Button sendButton;
	private ImageView qrImageView;
	private ContactId contactId;

	private RadioGroup modeRadioGroup;
	private RadioButton radioSequential, radioManual;
	private EditText manualIndexEditText;

	private boolean isUpdatingAmount = false;
	private boolean isUpdatingFiat = false;

	private EditText optionalMessageEditText;
	private Bitmap qrBitmap;
	private String lastGeneratedSubaddress;


	private double initialRate = 0.0;

	private int currentMinorIndex = 1;
	private TextView minorIndexTextView;

	@Inject AutoDeleteManager autoDeleteManager;
	@Inject TransactionManager transactionManager;

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (progressSpinner != null) {
			progressSpinner.setVisibility(View.GONE);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			finish();  // Close this activity and go back
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_request_xmr);

		if (getSupportActionBar() != null) {
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
			getSupportActionBar().setDisplayShowHomeEnabled(true);
		}

		progressSpinner    = findViewById(R.id.progressSpinner);
		amountEditText     = findViewById(R.id.editAmount);
		messageEditText    = findViewById(R.id.editMessage);
		rateEditText       = findViewById(R.id.editRate);
		conversionTextView = findViewById(R.id.conversionTextView);
		generateButton     = findViewById(R.id.buttonGenerate);
		minorIndexTextView = findViewById(R.id.minorIndexTextView);
		sendButton         = findViewById(R.id.buttonSend);
		modeRadioGroup = findViewById(R.id.modeRadioGroup);
		radioSequential = findViewById(R.id.radioSequential);
		radioManual = findViewById(R.id.radioManual);
		manualIndexEditText = findViewById(R.id.editManualIndex);

		sendButton.setEnabled(false);


		qrImageView        = findViewById(R.id.qrImageView);
		optionalMessageEditText = findViewById(R.id.editMessage);

		int id = getIntent().getIntExtra("CONTACT_ID", -1);
		if (id == -1) {
			Toast.makeText(this, R.string.missing_contact_id,
					Toast.LENGTH_SHORT).show();
			finish();
			return;
		}
		contactId = new ContactId(id);

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		String rateString = prefs.getString("pref_key_monero_rate", "0");
		try {
			initialRate = Double.parseDouble(rateString);
		} catch (NumberFormatException ignored) {}
		rateEditText.setText(String.valueOf(initialRate));

		amountEditText.addTextChangedListener(new SimpleTextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (!isUpdatingFiat) {
					isUpdatingAmount = true;
					updateConversionFromAmount();
					isUpdatingAmount = false;
				}
			}
		});

		conversionTextView.addTextChangedListener(new SimpleTextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (!isUpdatingAmount) {
					isUpdatingFiat = true;
					updateConversionFromFiat();
					isUpdatingFiat = false;
				}
			}
		});

		rateEditText.addTextChangedListener(new SimpleTextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				String amountStr = amountEditText.getText().toString();
				String fiatStr = conversionTextView.getText().toString();
				if (!amountStr.isEmpty()) {
					updateConversionFromAmount();
				} else if (!fiatStr.isEmpty()) {
					updateConversionFromFiat();
				}
			}
		});

		generateButton.setOnClickListener(v -> generateQrCode());
		sendButton.setOnClickListener(v -> sendRequestMessage());

		qrImageView.setOnClickListener(v -> hideKeyboard());

		modeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
			if (checkedId == R.id.radioManual) {
				manualIndexEditText.setVisibility(View.VISIBLE);
			} else {
				manualIndexEditText.setVisibility(View.GONE);
			}
		});

		manualIndexEditText.addTextChangedListener(new SimpleTextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (radioManual.isChecked()) {
					generateButton.setEnabled(!s.toString().trim().isEmpty());
				}
			}
		});

	}

	private String formatDecimal(double value, int decimals) {
		if (decimals == 2 && value == (long) value) {
			// If fiat value is a whole number (e.g., 83.00), show as integer
			return String.format(java.util.Locale.US, "%d", (long) value);
		} else {
			return String.format(java.util.Locale.US, "%." + decimals + "f", value);
		}
	}

	private void updateConversionFromAmount() {
		String amountStr = amountEditText.getText().toString().replace(',', '.');
		String rateStr = rateEditText.getText().toString().replace(',', '.');
		if (!amountStr.isEmpty() && !rateStr.isEmpty()) {
			try {
				double amount = Double.parseDouble(amountStr);
				double rate = Double.parseDouble(rateStr);
				double converted = amount * rate;
				conversionTextView.setText(formatDecimal(converted, 2)); // 2 decimals for fiat
			} catch (NumberFormatException ignored) {
				conversionTextView.setText("");
			}
		} else {
			conversionTextView.setText("");
		}
	}

	private void updateConversionFromFiat() {
		String fiatStr = conversionTextView.getText().toString().replace(',', '.');
		String rateStr = rateEditText.getText().toString().replace(',', '.');
		if (!fiatStr.isEmpty() && !rateStr.isEmpty()) {
			try {
				double fiat = Double.parseDouble(fiatStr);
				double rate = Double.parseDouble(rateStr);
				if (rate != 0) {
					double amount = fiat / rate;
					amountEditText.setText(formatDecimal(amount, 6)); // 6 decimals for XMR
				} else {
					amountEditText.setText("");
				}
			} catch (NumberFormatException ignored) {
				// Don't blank while typing
			}
		}
	}

	private void updateConversion() {
		String amountStr = amountEditText.getText().toString();
		String rateStr = rateEditText.getText().toString();
		if (!amountStr.isEmpty() && !rateStr.isEmpty()) {
			try {
				double amount = Double.parseDouble(amountStr.replace(',', '.'));
				double rate = Double.parseDouble(rateStr.replace(',', '.'));
				double converted = amount * rate;
				conversionTextView.setText(String.valueOf(converted));
			} catch (NumberFormatException ignored) {
				conversionTextView.setText("");
			}
		} else {
			conversionTextView.setText("");
		}
	}

	private void generateQrCode() {
		hideKeyboard();
		progressSpinner.setVisibility(View.VISIBLE);
		generateButton.setEnabled(false);
		sendButton.setEnabled(false);

		SecurePrefsManager securePrefs = new SecurePrefsManager(this);

		// ✅ 1️⃣ Load primary address and private view key
		SecureValue addressValue =
				securePrefs.read(PREF_KEY_PRIMARY_ADDRESS);
		SecureValue viewKeyValue =
				securePrefs.read(PREF_KEY_PRIVATE_VIEW_KEY);
		String primaryAddress =
				addressValue.isPresent() ? addressValue.get() : null;
		String privateViewKeyHex =
				viewKeyValue.isPresent() ? viewKeyValue.get() : null;

		// ✅ 2️⃣ Early checks
		// Settings we could not read are not settings the user never entered,
		// and telling them to enter an address they already entered would send
		// them round in circles.
		if (addressValue.isUnreadable() || viewKeyValue.isUnreadable()) {
			abortGenerate(R.string.monero_settings_unreadable);
			return;
		}

		if (primaryAddress == null || primaryAddress.isEmpty() || privateViewKeyHex == null || privateViewKeyHex.isEmpty()) {
			abortGenerate(R.string.missing_monero_addressor_view_key);
			return;
		}

		if (!AnonMoneroUtils.isValidMoneroPrivateKey(privateViewKeyHex)) {
			abortGenerate(R.string.invalid_monero_private_view_key);
			return;
		}

		if (!AnonMoneroUtils.isValidMoneroAddress(primaryAddress)) {
			abortGenerate(R.string.invalid_monero_address);
			return;
		}

		// ✅ 3️⃣ Load and parse minor index
		int minor;

// Check if manual mode is selected
		if (radioManual.isChecked()) {
			String manualStr = manualIndexEditText.getText().toString().trim();
			if (manualStr.isEmpty()) {
				Toast.makeText(this, R.string.minor_index_required,
						Toast.LENGTH_SHORT).show();
				progressSpinner.setVisibility(View.GONE);
				generateButton.setEnabled(true);
				return;
			}
			try {
				minor = Integer.parseInt(manualStr);
				if (minor < MIN_MINOR_INDEX || minor > MAX_MINOR_INDEX) {
					Toast.makeText(this, getString(
									R.string.minor_index_out_of_range,
									MIN_MINOR_INDEX, MAX_MINOR_INDEX),
							Toast.LENGTH_SHORT).show();
					progressSpinner.setVisibility(View.GONE);
					generateButton.setEnabled(true);
					return;
				}
			} catch (NumberFormatException e) {
				Toast.makeText(this, R.string.minor_index_invalid,
						Toast.LENGTH_SHORT).show();
				progressSpinner.setVisibility(View.GONE);
				generateButton.setEnabled(true);
				return;
			}
			currentMinorIndex = minor;  // For display
		} else {
			// Sequential mode: load from secure prefs
			SecureValue minorValue = securePrefs.read(PREF_KEY_MINOR_INDEX);
			// Falling back to 1 here would hand out a subaddress that has
			// already been issued, which is exactly what persistMinorIndex()
			// exists to prevent: two contacts receiving the same subaddress
			// can link their payments to the same wallet. Refuse instead.
			if (minorValue.isUnreadable()) {
				abortGenerate(R.string.monero_settings_unreadable);
				return;
			}
			String minorStr =
					minorValue.isPresent() ? minorValue.get() : null;
			minor = 1;
			if (minorStr != null && !minorStr.isEmpty()) {
				try {
					minor = Integer.parseInt(minorStr);
				} catch (NumberFormatException e) {
					Log.w("QRDebug", "Error parsing minor index: " + minorStr, e);
				}
			}
			minor++;  // increment for sequential
			currentMinorIndex = minor;
		}
		updateMinorIndexTextView();

		try {
			// ✅ 4️⃣ Decode primary address and derive the subaddress
			MoneroDecodedAddress decoded = AnonMoneroUtils.decodeAddress(primaryAddress);
			byte[] privateViewKey = AnonMoneroUtils.hexToBytes(privateViewKeyHex);

			String subaddress = AnonMoneroUtils.buildSubaddress(
					decoded.getPublicSpendKey(), privateViewKey, 0,
					currentMinorIndex);

			// The index is consumed as soon as it produces a subaddress, so
			// abandoning this screen cannot hand the same one out twice.
			persistMinorIndex();

			lastGeneratedSubaddress = subaddress;

			// ✅ 11️⃣ Build URI
			String amount = amountEditText.getText().toString().trim();
			String description = messageEditText.getText().toString().trim();

// ✅ Enforce max length of 255 chars
			if (description.length() > 255) {
				description = description.substring(0, 255);  // trim to 255 chars
			}

			StringBuilder uri = new StringBuilder("monero:" + subaddress);

// Add parameters if they exist
			boolean hasQuery = false;

			if (!amount.isEmpty()) {
				uri.append("?tx_amount=").append(amount);
				hasQuery = true;
			}
			if (!description.isEmpty()) {
				uri.append(hasQuery ? "&" : "?");
				uri.append("tx_description=").append(Uri.encode(description));
			}

			// ✅ 12️⃣ Save new rate if changed
			String newRateStr = rateEditText.getText().toString();
			try {
				double newRate = Double.parseDouble(newRateStr);
				if (newRate != initialRate) {
					askToSaveNewRate(newRate);
				}
			} catch (NumberFormatException ignored) {
				Log.d("QRDebug", "Rate parse error or empty input.");
			}

			// ✅ 13️⃣ Generate QR
			try {
				QRCodeWriter writer = new QRCodeWriter();
				int size = 400;
				Bitmap bmp = toBitmap(writer.encode(uri.toString(), BarcodeFormat.QR_CODE, size, size));
				qrBitmap = bmp;
				qrImageView.setImageBitmap(bmp);
				progressSpinner.setVisibility(View.GONE);
				generateButton.setEnabled(false);
				sendButton.setEnabled(true);
				qrImageView.setAlpha(0f);
				qrImageView.setScaleX(0.7f);
				qrImageView.setScaleY(0.7f);
				qrImageView.animate()
						.alpha(1f)
						.scaleX(1f)
						.scaleY(1f)
						.setDuration(500)
						.setInterpolator(new OvershootInterpolator(1.2f))
						.start();

			} catch (WriterException e) {
				sendButton.setEnabled(false);
				Toast.makeText(this, R.string.error_generating_qr, Toast.LENGTH_SHORT).show();
				progressSpinner.setVisibility(View.GONE);
				generateButton.setEnabled(true);
			}


		} catch (Exception e) {
			Log.e("QRDebug", "Exception during QR generation.", e);
			Toast.makeText(this, R.string.error_generating_qr, Toast.LENGTH_SHORT).show();
			progressSpinner.setVisibility(View.GONE);
			generateButton.setEnabled(true);
		}

	}


	private void updateMinorIndexTextView() {
		if (minorIndexTextView != null) {
			minorIndexTextView.setText(
					getString(R.string.minor_index_label, currentMinorIndex));
			minorIndexTextView.setVisibility(View.VISIBLE);
		}
	}

	/**
	 * Reports why QR generation stopped and puts the controls back. The
	 * checks at the top of {@link #generateQrCode()} used to return without
	 * this, leaving the spinner up and the generate button disabled.
	 */
	private void abortGenerate(@StringRes int messageRes) {
		Toast.makeText(this, messageRes, Toast.LENGTH_SHORT).show();
		progressSpinner.setVisibility(View.GONE);
		generateButton.setEnabled(true);
	}

	/**
	 * Records the highest minor index handed out, so the next sequential
	 * request starts after it. Called when a subaddress is generated rather
	 * than when it is sent: a subaddress the user saw but did not send must
	 * still not be reissued, or two contacts could receive the same one and
	 * link their payments to the same wallet.
	 */
	private void persistMinorIndex() {
		if (!radioSequential.isChecked()) return;
		SecurePrefsManager securePrefs = new SecurePrefsManager(this);
		securePrefs.putEncrypted(PREF_KEY_MINOR_INDEX,
				String.valueOf(currentMinorIndex));
	}



	private void askToSaveNewRate(double newRate) {
		new AlertDialog.Builder(this)
				.setTitle(R.string.update_rate_title)
				.setMessage(R.string.update_rate_message)
				.setPositiveButton(R.string.yes, (dialog, which) -> {
					SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
					prefs.edit().putString("pref_key_monero_rate", String.valueOf(newRate)).apply();
					initialRate = newRate;
					Toast.makeText(this, R.string.rate_updated, Toast.LENGTH_SHORT).show();
				})
				.setNegativeButton(R.string.no, null)
				.show();
	}

	private void sendRequestMessage() {
		try {
			if (qrBitmap == null) {
				Toast.makeText(this, R.string.no_qr_code_generated,
						Toast.LENGTH_SHORT).show();
				return;
			}

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos); // Change to PNG
			byte[] qrBytes = baos.toByteArray();

			long timestamp = System.currentTimeMillis();
			GroupId groupId = messagingManager.getConversationId(contactId);

			AttachmentHeader attachmentHeader = messagingManager.addLocalAttachment(
					groupId, timestamp, "image/png", new ByteArrayInputStream(qrBytes) // NOTE: change MIME type!
			);

			// Build the message

			String amount = amountEditText.getText().toString();
			String rateStr = rateEditText.getText().toString();
			String optionalMessage = optionalMessageEditText.getText().toString().trim();

			// Safety: limit optional message to 100 characters
			if (optionalMessage.length() > 100) {
				optionalMessage = optionalMessage.substring(0, 100);
			}

			StringBuilder message = new StringBuilder();
			message.append("🪙 Monero Request:\n")
					.append("Address: ").append(shortenAddress(lastGeneratedSubaddress));

			if (!amount.isEmpty()) {
				message.append("\nAmount: ").append(amount).append(" XMR");
			}

			if (!rateStr.isEmpty()) {
				message.append("\nRate: ").append(rateStr);
			}

			if (!amount.isEmpty() && !rateStr.isEmpty()) {
				try {
					double amountValue = Double.parseDouble(amount);
					double rateValue = Double.parseDouble(rateStr);
					double fiatValue = amountValue * rateValue;
					message.append("\nFiat: ").append(String.format("%.2f", fiatValue));
				} catch (NumberFormatException ignored) {}
			}

			if (!optionalMessage.isEmpty()) {
				message.append("\n").append(optionalMessage);
			}

			// Create and send the message
			long autoDeleteTimer = 0;
			try {
				autoDeleteTimer = transactionManager.transactionWithResult(true, txn ->
						autoDeleteManager.getAutoDeleteTimer(txn, contactId)
				);
			} catch (Exception e) {
				e.printStackTrace(); // fallback to zero
			}

			PrivateMessage pm = privateMessageFactory.createPrivateMessage(
					groupId, timestamp, message.toString(), Collections.singletonList(attachmentHeader), autoDeleteTimer
			);

			messagingManager.addLocalMessage(pm);

			// The index was already persisted when the subaddress was generated

			Toast.makeText(this, R.string.request_sent, Toast.LENGTH_SHORT).show();
			finish();

		} catch (Exception e) {
			e.printStackTrace();
			Toast.makeText(this, R.string.error_creating_message, Toast.LENGTH_SHORT).show();
		}
	}

	private String shortenAddress(String address) {
		if (address == null || address.length() < 12) return address; // Very short addresses, don't shorten
		String start = address.substring(0, 8); // First 8 chars
		String end = address.substring(address.length() - 8); // Last 8 chars
		return start + "..." + end;
	}

	private Bitmap toBitmap(com.google.zxing.common.BitMatrix matrix) {
		int width = matrix.getWidth();
		int height = matrix.getHeight();
		Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				bmp.setPixel(x, y, matrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
			}
		}
		return bmp;
	}

	private void hideKeyboard() {
		View focus = getCurrentFocus();
		if (focus != null) {
			InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
		}
	}

	@Override
	public void injectActivity(ActivityComponent component) {
		component.inject(this);
	}
	private abstract class SimpleTextWatcher implements android.text.TextWatcher {
		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

		@Override
		public void afterTextChanged(android.text.Editable s) {}
	}
}

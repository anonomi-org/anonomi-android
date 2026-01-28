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
import org.anonomi.android.xmr.CryptoUtils;
import org.anonomi.android.xmr.SubaddressGenerator;
import org.anonchatsecure.bramble.api.contact.ContactId;
import org.anonchatsecure.bramble.api.sync.GroupId;
import org.anonchatsecure.anonchat.api.messaging.MessagingManager;
import org.anonchatsecure.anonchat.api.messaging.PrivateMessage;
import org.anonchatsecure.anonchat.api.messaging.PrivateMessageFactory;
import org.anonchatsecure.anonchat.api.attachment.AttachmentHeader;

import org.anonomi.android.util.SecurePrefsManager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Collections;

import javax.inject.Inject;

import android.util.Log;

import org.anonomi.android.xmr.AnonMoneroUtils;
import org.anonomi.android.xmr.MoneroBase58;
import org.anonomi.android.xmr.MoneroDecodedAddress;

import org.anonchatsecure.anonchat.api.autodelete.AutoDeleteManager;
import org.anonchatsecure.bramble.api.db.TransactionManager;

public class RequestXmrActivity extends BriarActivity {

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
			Toast.makeText(this, "Missing contact ID", Toast.LENGTH_SHORT).show();
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

	private byte[] hexStringToByteArray(String s) {
		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
					+ Character.digit(s.charAt(i+1), 16));
		}
		return data;
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
		String primaryAddress = securePrefs.getDecrypted("pref_key_primary_address");
		String privateViewKeyHex = securePrefs.getDecrypted("pref_key_private_view_key");

		// ✅ 2️⃣ Early checks
		if (primaryAddress == null || primaryAddress.isEmpty() || privateViewKeyHex == null || privateViewKeyHex.isEmpty()) {
			Toast.makeText(this, R.string.missing_monero_addressor_view_key, Toast.LENGTH_SHORT).show();
			return;
		}

		if (!AnonMoneroUtils.isValidMoneroPrivateKey(privateViewKeyHex)) {
			Toast.makeText(this, R.string.invalid_monero_private_view_key, Toast.LENGTH_SHORT).show();
			return;
		}

		if (!AnonMoneroUtils.isValidMoneroAddress(primaryAddress)) {
			Toast.makeText(this, R.string.invalid_monero_address, Toast.LENGTH_SHORT).show();
			return;
		}

		// ✅ 3️⃣ Load and parse minor index
		int minor;

// Check if manual mode is selected
		if (radioManual.isChecked()) {
			String manualStr = manualIndexEditText.getText().toString().trim();
			if (manualStr.isEmpty()) {
				Toast.makeText(this, "Please enter a minor index.", Toast.LENGTH_SHORT).show();
				progressSpinner.setVisibility(View.GONE);
				generateButton.setEnabled(true);
				return;
			}
			try {
				minor = Integer.parseInt(manualStr);
				if (minor < 1 || minor > 1000000) {
					Toast.makeText(this, "Minor index must be between 1 and 1,000,000.", Toast.LENGTH_SHORT).show();
					progressSpinner.setVisibility(View.GONE);
					generateButton.setEnabled(true);
					return;
				}
			} catch (NumberFormatException e) {
				Toast.makeText(this, "Invalid minor index.", Toast.LENGTH_SHORT).show();
				progressSpinner.setVisibility(View.GONE);
				generateButton.setEnabled(true);
				return;
			}
			currentMinorIndex = minor;  // For display
		} else {
			// Sequential mode: load from secure prefs
			String minorStr = securePrefs.getDecrypted("pref_key_minor_index_key");
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
			// ✅ 4️⃣ Decode primary address
			MoneroDecodedAddress decoded = AnonMoneroUtils.decodeAddress(primaryAddress);
			byte[] publicSpendKey = decoded.getPublicSpendKey(); // 32 bytes
			byte[] publicViewKey = decoded.getPublicViewKey();   // 32 bytes

			// ✅ 5️⃣ Convert private view key
			byte[] privateViewKey = hexStringToByteArray(privateViewKeyHex);

			// ✅ 6️⃣ Generate subaddress keys
			byte[] subPubSpendKey = SubaddressGenerator.generateSubaddressPublicSpendKey(
					publicSpendKey, privateViewKey, 0, currentMinorIndex
			);

			byte[] subPubViewKey = SubaddressGenerator.generateSubaddressPublicViewKey(subPubSpendKey, privateViewKey);

			// ✅ 7️⃣ Build subaddress data (hex string)
			String hex = "2a" +
					CryptoUtils.bytesToHex(subPubSpendKey) +
					CryptoUtils.bytesToHex(subPubViewKey);

			// ✅ 8️⃣ Hash for checksum (matches Node.js behavior)
			byte[] hexBytes = hexStringToByteArray(hex);
			byte[] checksum = CryptoUtils.keccak256(hexBytes, 0, hexBytes.length);
			String checksumHex = CryptoUtils.bytesToHex(checksum).substring(0, 8);  // 4 bytes = 8 hex chars

			// ✅ 9️⃣ Combine into full hex string
			String fullHex = hex + checksumHex;

			// ✅ 🔟 Encode Base58
			String subaddress = MoneroBase58.encode(hexStringToByteArray(fullHex));

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
			minorIndexTextView.setText("# " + currentMinorIndex);
			minorIndexTextView.setVisibility(View.VISIBLE);
		}
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
				Toast.makeText(this, "No QR code generated!", Toast.LENGTH_SHORT).show();
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

			if (radioSequential.isChecked()) {
				SecurePrefsManager securePrefs = new SecurePrefsManager(this);
				securePrefs.putEncrypted("pref_key_minor_index_key", String.valueOf(currentMinorIndex));
			}

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

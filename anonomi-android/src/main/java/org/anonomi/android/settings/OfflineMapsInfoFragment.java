package org.anonomi.android.settings;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import org.anonomi.R;

public class OfflineMapsInfoFragment extends Fragment {

	private static final String TOR_BROWSER_PACKAGE = "org.torproject.torbrowser";

	public static OfflineMapsInfoFragment newInstance() {
		return new OfflineMapsInfoFragment();
	}

	@Nullable
	@Override
	public View onCreateView(
			LayoutInflater inflater,
			@Nullable ViewGroup container,
			@Nullable Bundle savedInstanceState
	) {
		View v = inflater.inflate(R.layout.fragment_offline_maps_info, container, false);

		TextView linkTextView = v.findViewById(R.id.onion_link);

		// Onion URL (updated in strings.xml)
		String onionUrl = getString(R.string.offline_maps_onion_url);
		linkTextView.setText(onionUrl);

		// Clearnet URL (add your clearnet endpoint in strings.xml)
		String clearnetUrl = getString(R.string.offline_maps_clearnet_url);

		// Copy Onion URL button
		Button copyButton = v.findViewById(R.id.copy_url_button);
		copyButton.setOnClickListener(view -> copyToClipboard("Onion URL", onionUrl));

		// Open in Tor Browser button
		Button openTorButton = v.findViewById(R.id.open_tor_browser_button);
		openTorButton.setOnClickListener(
				view -> openInTorBrowser(onionUrl, clearnetUrl)
		);

		// Copy Clearnet URL button
		Button copyClearnetButton = v.findViewById(R.id.copy_clearnet_url_button);
		copyClearnetButton.setOnClickListener(view -> copyToClipboard("Clearnet URL", clearnetUrl));

		return v;
	}

	private void copyToClipboard(String label, String value) {
		ClipboardManager clipboard =
				(ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
		ClipData clip = ClipData.newPlainText(label, value);
		clipboard.setPrimaryClip(clip);
		Toast.makeText(requireContext(),
				R.string.url_copied_to_clipboard,
				Toast.LENGTH_SHORT).show();
	}

	private void openInTorBrowser(String onionUrl, String clearnetUrl) {
		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(onionUrl));
		intent.setPackage(TOR_BROWSER_PACKAGE);

		try {
			startActivity(intent);
		} catch (Exception e) {
			showTorBrowserRequiredDialog(clearnetUrl);
		}
	}

	private void showTorBrowserRequiredDialog(String clearnetUrl) {
		new AlertDialog.Builder(requireContext())
				.setTitle(R.string.tor_browser_required_title)
				.setMessage(R.string.tor_browser_required_message)
				.setPositiveButton(R.string.install_tor_browser, (d, w) -> {
					try {
						startActivity(new Intent(Intent.ACTION_VIEW,
								Uri.parse("https://www.torproject.org/download/")));
					} catch (Exception ignored) {}
				})
				.setNeutralButton(R.string.open_clearnet_warning, (d, w) -> {
					openClearnetUrl(clearnetUrl);
				})
				.setNegativeButton(android.R.string.cancel, null)
				.show();
	}

	private void openClearnetUrl(String clearnetUrl) {
		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(clearnetUrl));
		startActivity(intent);
	}
}
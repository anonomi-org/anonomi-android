package org.anonomi.android.account;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.widget.Toast;

import org.anonomi.R;

public class SetupInfoFragment extends Fragment {

	public static SetupInfoFragment newInstance() {
		return new SetupInfoFragment();
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_setup_info, container, false);

		// Setup the toolbar
		androidx.appcompat.widget.Toolbar toolbar = v.findViewById(R.id.toolbar);
		toolbar.setTitle(R.string.more_info_title);
		toolbar.setNavigationIcon(R.drawable.ic_arrow_back);

		// Handle back button
		toolbar.setNavigationOnClickListener(view -> requireActivity().getSupportFragmentManager().popBackStack());

		// Find the Onion link TextView
		TextView linkTextView = v.findViewById(R.id.onion_link);

		// Set up the Copy URL button
		Button copyButton = v.findViewById(R.id.copy_url_button);
		copyButton.setOnClickListener(view -> {
			String url = linkTextView.getText().toString();
			ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
			ClipData clip = ClipData.newPlainText("Onion URL", url);
			clipboard.setPrimaryClip(clip);
			Toast.makeText(requireContext(), R.string.url_copied_to_clipboard, Toast.LENGTH_SHORT).show();
		});

		return v;
	}
}
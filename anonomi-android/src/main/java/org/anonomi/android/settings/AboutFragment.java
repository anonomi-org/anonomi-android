package org.anonomi.android.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.anonomi.BuildConfig;
import org.anonomi.R;
import org.briarproject.nullsafety.MethodsNotNullByDefault;
import org.briarproject.nullsafety.ParametersNotNullByDefault;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.widget.Button;

@MethodsNotNullByDefault
@ParametersNotNullByDefault
public class AboutFragment extends Fragment {

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater,
			@Nullable ViewGroup container,
			@Nullable Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_about, container, false);
	}

	@Override
	public void onStart() {
		super.onStart();
		requireActivity().setTitle(R.string.about_title);

		TextView anonChatVersion = requireActivity().findViewById(R.id.anonomiVersion);
		TextView torVersion = requireActivity().findViewById(R.id.TorVersion);

		anonChatVersion.setText(
				getString(R.string.anonomi_version, BuildConfig.VERSION_NAME)
		);

		torVersion.setText(
				getString(R.string.tor_version, BuildConfig.TorVersion)
		);
	}
}
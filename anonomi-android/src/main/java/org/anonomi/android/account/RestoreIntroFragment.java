package org.anonomi.android.account;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import org.anonomi.R;
import org.anonomi.android.util.ActivityLaunchers.GetContentAdvanced;
import org.anonomi.android.util.ActivityLaunchers.OpenDocumentAdvanced;
import org.briarproject.nullsafety.MethodsNotNullByDefault;
import org.briarproject.nullsafety.ParametersNotNullByDefault;

import javax.annotation.Nullable;

import androidx.activity.result.ActivityResultLauncher;

import static org.anonomi.android.util.UiUtils.launchActivityToOpenFile;

/**
 * Says what restoring an account costs the phone the backup came from, then
 * asks for the file.
 */
@MethodsNotNullByDefault
@ParametersNotNullByDefault
public class RestoreIntroFragment extends RestoreFragment {

	private static final String TAG = RestoreIntroFragment.class.getName();

	private final ActivityResultLauncher<String[]> docLauncher =
			registerForActivityResult(new OpenDocumentAdvanced(),
					this::onDocumentChosen);
	private final ActivityResultLauncher<String> contentLauncher =
			registerForActivityResult(new GetContentAdvanced(),
					this::onDocumentChosen);

	public static RestoreIntroFragment newInstance() {
		return new RestoreIntroFragment();
	}

	@Override
	public String getUniqueTag() {
		return TAG;
	}

	@Override
	protected String getHelpText() {
		return getString(R.string.restore_intro_text);
	}

	@Override
	public View onCreateView(LayoutInflater inflater,
			@Nullable ViewGroup container,
			@Nullable Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_restore_intro, container,
				false);
		Button chooseFile = v.findViewById(R.id.chooseFileButton);
		chooseFile.setOnClickListener(this);
		return v;
	}

	@Override
	public void onClick(View view) {
		// A backup carries no type of its own on purpose, so anything the
		// user can reach has to be offered
		launchActivityToOpenFile(requireContext(), docLauncher, contentLauncher,
				new String[] {"*/*"});
	}

	private void onDocumentChosen(@Nullable Uri uri) {
		if (uri != null) viewModel.onBackupFileChosen(uri);
	}
}

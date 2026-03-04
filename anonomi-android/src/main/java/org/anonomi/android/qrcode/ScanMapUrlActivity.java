package org.anonomi.android.qrcode;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.anonomi.R;

/**
 * Hosts ScanMapUrlFragment. Returns RESULT_OK with extra {@link #EXTRA_URL} on success.
 */
public class ScanMapUrlActivity extends AppCompatActivity {

	public static final String EXTRA_URL = "url";

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_fragment_container);

		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction()
					.replace(R.id.fragmentContainer, new ScanMapUrlFragment())
					.commit();
		}
	}
}

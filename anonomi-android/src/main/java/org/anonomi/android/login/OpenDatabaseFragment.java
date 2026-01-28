package org.anonomi.android.login;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.anonomi.R;
import org.anonomi.android.activity.ActivityComponent;
import org.anonomi.android.fragment.BaseFragment;
import org.briarproject.nullsafety.MethodsNotNullByDefault;
import org.briarproject.nullsafety.ParametersNotNullByDefault;

import javax.inject.Inject;

import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;

import static org.anonomi.android.login.StartupViewModel.State;
import static org.anonomi.android.login.StartupViewModel.State.COMPACTING;
import static org.anonomi.android.login.StartupViewModel.State.MIGRATING;

@MethodsNotNullByDefault
@ParametersNotNullByDefault
public class OpenDatabaseFragment extends BaseFragment {

	final static String TAG = OpenDatabaseFragment.class.getName();

	@Inject
	ViewModelProvider.Factory viewModelFactory;

	private StartupViewModel viewModel;

	private TextView textView;
	private ImageView imageView;

	@Override
	public void injectFragment(ActivityComponent component) {
		component.inject(this);
		viewModel = new ViewModelProvider(requireActivity(),
				viewModelFactory).get(StartupViewModel.class);
	}

	@Override
	public View onCreateView(LayoutInflater inflater,
			@Nullable ViewGroup container,
			@Nullable Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_open_database, container,
				false);

		textView = v.findViewById(R.id.textView);
		imageView = v.findViewById(R.id.imageView);

		LifecycleOwner owner = getViewLifecycleOwner();
		viewModel.getState().observe(owner, this::onStateChanged);

		return v;
	}

	private void onStateChanged(State state) {
		if (state == MIGRATING) showMigration();
		else if (state == COMPACTING) showCompaction();
	}

	private void showMigration() {
		textView.setText(R.string.startup_migrate_database);
		imageView.setImageResource(R.drawable.startup_migration);
	}

	private void showCompaction() {
		textView.setText(R.string.startup_compact_database);
		imageView.setImageResource(R.drawable.startup_migration);
	}

	@Override
	public String getUniqueTag() {
		return TAG;
	}

}

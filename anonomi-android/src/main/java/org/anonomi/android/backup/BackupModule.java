package org.anonomi.android.backup;

import org.anonomi.android.viewmodel.ViewModelKey;

import androidx.lifecycle.ViewModel;
import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoMap;

@Module
public interface BackupModule {

	@Binds
	@IntoMap
	@ViewModelKey(BackupViewModel.class)
	ViewModel bindBackupViewModel(BackupViewModel backupViewModel);

}

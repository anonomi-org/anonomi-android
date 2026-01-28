package org.anonomi.android.mailbox;

import org.anonomi.android.viewmodel.ViewModelKey;

import androidx.lifecycle.ViewModel;
import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoMap;

@Module
public interface MailboxModule {

	@Binds
	@IntoMap
	@ViewModelKey(MailboxViewModel.class)
	ViewModel bindMailboxViewModel(MailboxViewModel mailboxViewModel);

}

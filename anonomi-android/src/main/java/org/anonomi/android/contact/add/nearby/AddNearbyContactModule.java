package org.anonomi.android.contact.add.nearby;

import org.anonomi.android.viewmodel.ViewModelKey;

import androidx.lifecycle.ViewModel;
import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoMap;

@Module
public abstract class AddNearbyContactModule {

	@Binds
	@IntoMap
	@ViewModelKey(AddNearbyContactViewModel.class)
	abstract ViewModel bindContactExchangeViewModel(
			AddNearbyContactViewModel addNearbyContactViewModel);

}

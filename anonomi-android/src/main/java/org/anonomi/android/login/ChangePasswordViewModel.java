package org.anonomi.android.login;

import org.anonchatsecure.bramble.api.account.AccountManager;
import org.anonchatsecure.bramble.api.crypto.DecryptionException;
import org.anonchatsecure.bramble.api.crypto.DecryptionResult;
import org.anonchatsecure.bramble.api.crypto.PasswordStrengthEstimator;
import org.anonchatsecure.bramble.api.lifecycle.IoExecutor;
import org.anonomi.android.viewmodel.LiveEvent;
import org.anonomi.android.viewmodel.MutableLiveEvent;
import org.briarproject.nullsafety.NotNullByDefault;

import java.util.concurrent.Executor;

import javax.inject.Inject;

import androidx.lifecycle.ViewModel;

import static org.anonchatsecure.bramble.api.crypto.DecryptionResult.SUCCESS;

@NotNullByDefault
public class ChangePasswordViewModel extends ViewModel {

	private final AccountManager accountManager;
	private final Executor ioExecutor;
	private final PasswordStrengthEstimator strengthEstimator;

	@Inject
	ChangePasswordViewModel(AccountManager accountManager,
			@IoExecutor Executor ioExecutor,
			PasswordStrengthEstimator strengthEstimator) {
		this.accountManager = accountManager;
		this.ioExecutor = ioExecutor;
		this.strengthEstimator = strengthEstimator;
	}

	float estimatePasswordStrength(String password) {
		return strengthEstimator.estimateStrength(password);
	}

	LiveEvent<DecryptionResult> changePassword(String oldPassword,
			String newPassword) {
		MutableLiveEvent<DecryptionResult> result = new MutableLiveEvent<>();
		ioExecutor.execute(() -> {
			try {
				accountManager.changePassword(oldPassword, newPassword);
				result.postEvent(SUCCESS);
			} catch (DecryptionException e) {
				result.postEvent(e.getDecryptionResult());
			}
		});
		return result;
	}
}

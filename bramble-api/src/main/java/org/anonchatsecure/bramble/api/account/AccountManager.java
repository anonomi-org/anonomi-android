package org.anonchatsecure.bramble.api.account;

import org.anonchatsecure.bramble.api.crypto.DecryptionException;
import org.anonchatsecure.bramble.api.crypto.SecretKey;
import org.anonchatsecure.bramble.api.identity.IdentityManager;
import org.briarproject.nullsafety.NotNullByDefault;

import java.io.File;

import javax.annotation.Nullable;

@NotNullByDefault
public interface AccountManager {

	/**
	 * Returns true if the manager has the database key. This will be false
	 * before {@link #createAccount(String, String)} or {@link #signIn(String)}
	 * has been called, and true after {@link #createAccount(String, String)}
	 * or {@link #signIn(String)} has returned true, until
	 * {@link #deleteAccount()} is called or the process exits.
	 */
	boolean hasDatabaseKey();

	/**
	 * Returns the database key if the manager has it. This will be null
	 * before {@link #createAccount(String, String)} or {@link #signIn(String)}
	 * has been called, and non-null after
	 * {@link #createAccount(String, String)} or {@link #signIn(String)} has
	 * returned true, until {@link #deleteAccount()} is called or the process
	 * exits.
	 */
	@Nullable
	SecretKey getDatabaseKey();

	/**
	 * Returns true if the encrypted database key can be loaded from disk.
	 */
	boolean accountExists();

	/**
	 * Creates an identity with the given name and registers it with the
	 * {@link IdentityManager}. Creates a database key, encrypts it with the
	 * given password and stores it on disk. {@link #accountExists()} will
	 * return true after this method returns true.
	 */
	boolean createAccount(String name, String password);

	/**
	 * Deletes the encrypted database key from disk, leaving the rest of the
	 * account's state where it is. The state is encrypted with the key, so it
	 * cannot be read back afterwards and {@link #accountExists()} will return
	 * false. Deleting the key is a small enough piece of work to be worth
	 * separating from {@link #deleteAccount()} where it has to happen
	 * promptly. Does nothing if the key is already gone.
	 */
	void deleteDatabaseKey();

	/**
	 * Deletes all account state from disk. {@link #accountExists()} will
	 * return false after this method returns.
	 */
	void deleteAccount();

	/**
	 * Loads the encrypted database key from disk and decrypts it with the
	 * given password.
	 *
	 * @throws DecryptionException If the database key could not be loaded and
	 * decrypted.
	 */
	void signIn(String password) throws DecryptionException;

	/**
	 * Loads the encrypted database key from disk, decrypts it with the old
	 * password, encrypts it with the new password, and stores it on disk,
	 * replacing the old key.
	 *
	 * @throws DecryptionException If the database key could not be loaded and
	 * decrypted.
	 */
	void changePassword(String oldPassword, String newPassword)
			throws DecryptionException;

	/**
	 * Checks that the given password decrypts the database key, without
	 * signing in. Used before handing out anything that would outlive the
	 * unlocked screen.
	 *
	 * @throws DecryptionException If the database key could not be loaded and
	 * decrypted.
	 */
	void verifyPassword(String password) throws DecryptionException;

	/**
	 * Moves the given database file into place and stores the given database
	 * key, encrypted with the given password, so the account can be signed
	 * into afterwards. No identity is registered: the restored database
	 * already has one.
	 * <p>
	 * The key is stored last. If the process dies in between there is a
	 * database and no key, which the next launch clears.
	 *
	 * @return true if the account was restored, false if it was not, in which
	 * case nothing of it is left on disk.
	 * @throws IllegalStateException if an account already exists.
	 */
	boolean restoreAccount(File dbFile, SecretKey dbKey, String password);
}

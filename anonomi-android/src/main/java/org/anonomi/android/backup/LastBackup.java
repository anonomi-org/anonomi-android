package org.anonomi.android.backup;

import android.content.Context;

import org.anonomi.android.util.SecurePrefsManager;
import org.anonomi.android.util.SecureValue;
import org.briarproject.nullsafety.NotNullByDefault;

import java.util.logging.Logger;

import javax.annotation.Nullable;

import static java.util.logging.Level.WARNING;
import static java.util.logging.Logger.getLogger;
import static org.anonchatsecure.bramble.util.LogUtils.logException;

/**
 * When the app last wrote a backup, so that settings can say how old it is
 * and an old backup is not mistaken for a current one.
 * <p>
 * Deliberately only the time. Where the backup went is not kept: nothing
 * reaches the file again once it has been written, and a note of its
 * whereabouts would answer the one question a search of the phone should not
 * be able to answer.
 * <p>
 * Encrypted, because even the fact that a backup exists is worth something to
 * someone searching the phone.
 */
@NotNullByDefault
public class LastBackup {

	private static final Logger LOG = getLogger(LastBackup.class.getName());

	/**
	 * Not shared with any {@code Preference} of the same name - see
	 * {@link SecurePrefsManager} for why a shared key would be overwritten
	 * with plaintext.
	 */
	public static final String PREF_KEY_LAST_BACKUP = "pref_key_last_backup";

	public final long created;

	private LastBackup(long created) {
		this.created = created;
	}

	public static void save(Context ctx, long created) {
		save(new SecurePrefsManager(ctx), created);
	}

	static void save(SecurePrefsManager prefs, long created) {
		prefs.putEncrypted(PREF_KEY_LAST_BACKUP, encode(created));
	}

	/**
	 * Returns the last backup, or null if there has not been one or the record
	 * cannot be read. A record that will not decrypt is treated as no record:
	 * what it is used for is cosmetic, and the alternative is telling the user
	 * about a backup we cannot date.
	 */
	@Nullable
	public static LastBackup load(Context ctx) {
		try {
			return load(new SecurePrefsManager(ctx));
		} catch (RuntimeException e) {
			// Opening the encrypted preferences throws when the Keystore
			// refuses - after the screen lock has changed, for instance. This
			// is read while a settings screen is being shown, and a date on
			// it is not worth taking the screen down for
			logException(LOG, WARNING, e);
			return null;
		}
	}

	@Nullable
	static LastBackup load(SecurePrefsManager prefs) {
		SecureValue stored = prefs.read(PREF_KEY_LAST_BACKUP);
		if (!stored.isPresent()) return null;
		return decode(stored.get());
	}

	static String encode(long created) {
		return String.valueOf(created);
	}

	@Nullable
	static LastBackup decode(String stored) {
		try {
			return new LastBackup(Long.parseLong(stored));
		} catch (NumberFormatException e) {
			return null;
		}
	}
}

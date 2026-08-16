package org.anonomi.android.backup;

import android.content.Context;
import android.net.Uri;

import org.anonomi.android.util.SecurePrefsManager;
import org.anonomi.android.util.SecureValue;
import org.briarproject.nullsafety.NotNullByDefault;

import javax.annotation.Nullable;

/**
 * What the app remembers about the backup it last wrote, so that it can be
 * named in settings and deleted by a panic wipe.
 * <p>
 * Encrypted, because where a backup is kept is worth as much to someone
 * searching the phone as the fact that one exists. Written as a single record
 * so that it cannot be found half updated.
 */
@NotNullByDefault
public class LastBackup {

	/**
	 * Not shared with any {@code Preference} of the same name - see
	 * {@link SecurePrefsManager} for why a shared key would be overwritten
	 * with plaintext.
	 */
	public static final String PREF_KEY_LAST_BACKUP = "pref_key_last_backup";

	private static final String SEPARATOR = "\n";

	public final Uri uri;
	public final long created;
	public final String displayName;

	private LastBackup(Uri uri, long created, String displayName) {
		this.uri = uri;
		this.created = created;
		this.displayName = displayName;
	}

	public static void save(Context ctx, Uri uri, long created,
			String displayName) {
		save(new SecurePrefsManager(ctx), uri, created, displayName);
	}

	static void save(SecurePrefsManager prefs, Uri uri, long created,
			String displayName) {
		prefs.putEncrypted(PREF_KEY_LAST_BACKUP,
				encode(uri, created, displayName));
	}

	/**
	 * Returns the last backup, or null if there has not been one or the record
	 * cannot be read. A record that will not decrypt is treated as no record:
	 * everything it is used for is best-effort, and the alternative is telling
	 * the user about a backup we cannot name or reach.
	 */
	@Nullable
	public static LastBackup load(Context ctx) {
		return load(new SecurePrefsManager(ctx));
	}

	@Nullable
	static LastBackup load(SecurePrefsManager prefs) {
		SecureValue stored = prefs.read(PREF_KEY_LAST_BACKUP);
		if (!stored.isPresent()) return null;
		return decode(stored.get());
	}

	static String encode(Uri uri, long created, String displayName) {
		// The name goes last because it is the only part that can hold a
		// separator
		return created + SEPARATOR + uri + SEPARATOR + displayName;
	}

	@Nullable
	static LastBackup decode(String stored) {
		String[] parts = stored.split(SEPARATOR, 3);
		if (parts.length != 3) return null;
		long created;
		try {
			created = Long.parseLong(parts[0]);
		} catch (NumberFormatException e) {
			return null;
		}
		Uri uri = Uri.parse(parts[1]);
		if (uri.getScheme() == null) return null;
		return new LastBackup(uri, created, parts[2]);
	}
}

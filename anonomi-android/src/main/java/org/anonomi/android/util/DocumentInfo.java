package org.anonomi.android.util;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import org.briarproject.nullsafety.NotNullByDefault;

import java.util.logging.Logger;

import static java.util.logging.Level.WARNING;
import static java.util.logging.Logger.getLogger;
import static org.anonchatsecure.bramble.util.LogUtils.logException;

/**
 * What a content provider will say about a document the user has chosen.
 */
@NotNullByDefault
public class DocumentInfo {

	private static final Logger LOG = getLogger(DocumentInfo.class.getName());

	/**
	 * Something to show the user for this document, falling back to part of
	 * the URI if the provider will not give a name.
	 */
	public final String displayName;

	/**
	 * The document's size in bytes, or -1 if the provider did not say.
	 */
	public final long size;

	private DocumentInfo(String displayName, long size) {
		this.displayName = displayName;
		this.size = size;
	}

	/**
	 * Asks the provider about the given document. A provider that answers
	 * neither question, or throws, gives -1 for the size, so a caller that
	 * needs a real size treats an unhelpful provider as an unknown one.
	 */
	public static DocumentInfo query(ContentResolver resolver, Uri uri) {
		String last = uri.getLastPathSegment();
		String displayName = last == null ? uri.toString() : last;
		long size = -1;
		String[] columns =
				{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE};
		try {
			Cursor c = resolver.query(uri, columns, null, null, null);
			if (c != null) {
				try {
					if (c.moveToFirst()) {
						int name = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
						if (name != -1 && !c.isNull(name)) {
							displayName = c.getString(name);
						}
						int length = c.getColumnIndex(OpenableColumns.SIZE);
						if (length != -1 && !c.isNull(length)) {
							size = c.getLong(length);
						}
					}
				} finally {
					c.close();
				}
			}
		} catch (Exception e) {
			logException(LOG, WARNING, e);
		}
		return new DocumentInfo(displayName, size);
	}
}

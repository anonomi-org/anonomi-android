package org.anonchatsecure.bramble.api.data;

import org.anonchatsecure.bramble.api.FormatException;
import org.anonchatsecure.bramble.api.db.Metadata;
import org.briarproject.nullsafety.NotNullByDefault;

@NotNullByDefault
public interface MetadataEncoder {

	Metadata encode(BdfDictionary d) throws FormatException;
}

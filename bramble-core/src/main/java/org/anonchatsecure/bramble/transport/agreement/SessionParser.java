package org.anonchatsecure.bramble.transport.agreement;

import org.anonchatsecure.bramble.api.FormatException;
import org.anonchatsecure.bramble.api.data.BdfDictionary;
import org.briarproject.nullsafety.NotNullByDefault;

@NotNullByDefault
interface SessionParser {

	Session parseSession(BdfDictionary meta) throws FormatException;
}

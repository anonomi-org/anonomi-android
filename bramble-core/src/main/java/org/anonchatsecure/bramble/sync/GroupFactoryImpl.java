package org.anonchatsecure.bramble.sync;

import org.anonchatsecure.bramble.api.crypto.CryptoComponent;
import org.anonchatsecure.bramble.api.sync.ClientId;
import org.anonchatsecure.bramble.api.sync.Group;
import org.anonchatsecure.bramble.api.sync.GroupFactory;
import org.anonchatsecure.bramble.api.sync.GroupId;
import org.anonchatsecure.bramble.util.ByteUtils;
import org.anonchatsecure.bramble.util.StringUtils;
import org.briarproject.nullsafety.NotNullByDefault;

import javax.annotation.concurrent.Immutable;
import javax.inject.Inject;

import static org.anonchatsecure.bramble.api.sync.Group.FORMAT_VERSION;
import static org.anonchatsecure.bramble.api.sync.GroupId.LABEL;
import static org.anonchatsecure.bramble.util.ByteUtils.INT_32_BYTES;

@Immutable
@NotNullByDefault
class GroupFactoryImpl implements GroupFactory {

	private static final byte[] FORMAT_VERSION_BYTES =
			new byte[] {FORMAT_VERSION};

	private final CryptoComponent crypto;

	@Inject
	GroupFactoryImpl(CryptoComponent crypto) {
		this.crypto = crypto;
	}

	@Override
	public Group createGroup(ClientId c, int majorVersion, byte[] descriptor) {
		byte[] majorVersionBytes = new byte[INT_32_BYTES];
		ByteUtils.writeUint32(majorVersion, majorVersionBytes, 0);
		byte[] hash = crypto.hash(LABEL, FORMAT_VERSION_BYTES,
				StringUtils.toUtf8(c.getString()), majorVersionBytes,
				descriptor);
		return new Group(new GroupId(hash), c, majorVersion, descriptor);
	}
}

package org.anonchatsecure.bramble.api.keyagreement;

import org.anonchatsecure.bramble.api.data.BdfList;
import org.anonchatsecure.bramble.api.plugin.TransportId;
import org.briarproject.nullsafety.NotNullByDefault;

import javax.annotation.concurrent.Immutable;

@Immutable
@NotNullByDefault
public class TransportDescriptor {

	private final TransportId id;
	private final BdfList descriptor;

	public TransportDescriptor(TransportId id, BdfList descriptor) {
		this.id = id;
		this.descriptor = descriptor;
	}

	public TransportId getId() {
		return id;
	}

	public BdfList getDescriptor() {
		return descriptor;
	}
}

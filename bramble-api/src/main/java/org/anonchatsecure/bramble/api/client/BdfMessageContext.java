package org.anonchatsecure.bramble.api.client;

import org.anonchatsecure.bramble.api.data.BdfDictionary;
import org.anonchatsecure.bramble.api.sync.MessageId;
import org.briarproject.nullsafety.NotNullByDefault;

import java.util.Collection;
import java.util.Collections;

import javax.annotation.concurrent.Immutable;

@Immutable
@NotNullByDefault
public class BdfMessageContext {

	private final BdfDictionary dictionary;
	private final Collection<MessageId> dependencies;

	public BdfMessageContext(BdfDictionary dictionary,
			Collection<MessageId> dependencies) {
		this.dictionary = dictionary;
		this.dependencies = dependencies;
	}

	public BdfMessageContext(BdfDictionary dictionary) {
		this(dictionary, Collections.emptyList());
	}

	public BdfDictionary getDictionary() {
		return dictionary;
	}

	public Collection<MessageId> getDependencies() {
		return dependencies;
	}
}

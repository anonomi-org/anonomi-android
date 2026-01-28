package org.anonomi.android.attachment;

import org.anonomi.android.attachment.media.MediaModule;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {
		MediaModule.class
})
interface AbstractAttachmentRetrieverComponent {

	void inject(AttachmentRetrieverIntegrationTest test);

}

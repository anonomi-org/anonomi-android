package org.anonomi.android.conversation.glide;

import android.content.Context;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.module.AppGlideModule;

import org.anonomi.android.AnonChatApplication;
import org.anonchatsecure.anonchat.api.attachment.AttachmentHeader;
import org.briarproject.nullsafety.NotNullByDefault;

import java.io.InputStream;

import static android.util.Log.DEBUG;
import static android.util.Log.WARN;
import static org.anonomi.android.TestingConstants.IS_DEBUG_BUILD;

@GlideModule
@NotNullByDefault
public final class BriarGlideModule extends AppGlideModule {

	@Override
	public void registerComponents(Context context, Glide glide,
			Registry registry) {
		AnonChatApplication app =
				(AnonChatApplication) context.getApplicationContext();
		BriarModelLoaderFactory factory = new BriarModelLoaderFactory(app);
		registry.prepend(AttachmentHeader.class, InputStream.class, factory);
	}

	@Override
	public void applyOptions(Context context, GlideBuilder builder) {
		builder.setLogLevel(IS_DEBUG_BUILD ? DEBUG : WARN);
	}

	@Override
	public boolean isManifestParsingEnabled() {
		return false;
	}

}
package org.anonomi.android.util;

import android.content.Context;
import android.content.Intent;

import org.briarproject.nullsafety.NotNullByDefault;

import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument;
import androidx.activity.result.contract.ActivityResultContracts.GetContent;
import androidx.activity.result.contract.ActivityResultContracts.GetMultipleContents;
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument;
import androidx.activity.result.contract.ActivityResultContracts.OpenMultipleDocuments;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.net.Uri;
import java.util.List;
import java.util.ArrayList;

import static android.app.Activity.RESULT_CANCELED;
import static android.bluetooth.BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE;
import static android.bluetooth.BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION;
import static android.content.Intent.EXTRA_MIME_TYPES;
import static android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION;
import static android.os.Build.VERSION.SDK_INT;
import static org.anonchatsecure.bramble.util.AndroidUtils.getSupportedImageContentTypes;

@NotNullByDefault
public class ActivityLaunchers {

	public static class CreateDocumentAdvanced extends CreateDocument {
		@NonNull
		@Override
		public Intent createIntent(Context context, String input) {
			Intent i = super.createIntent(context, input);
			putShowAdvancedExtra(i);
			return i;
		}
	}

	public static class GetContentAdvanced extends GetContent {
		@NonNull
		@Override
		public Intent createIntent(Context context, String input) {
			Intent i = super.createIntent(context, input);
			putShowAdvancedExtra(i);
			i.addFlags(FLAG_GRANT_READ_URI_PERMISSION);
			return i;
		}
	}

	public static class OpenDocumentAdvanced extends OpenDocument {
		@NonNull
		@Override
		public Intent createIntent(Context context, String[] input) {
			Intent i = super.createIntent(context, input);
			putShowAdvancedExtra(i);
			i.setType("/*");
			i.addFlags(FLAG_GRANT_READ_URI_PERMISSION);
			return i;
		}
	}

	public class GetImageAdvanced extends ActivityResultContract<String[], List<Uri>> {
		@Override
		public Intent createIntent(Context context, String[] input) {
			Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
			intent.setType("*/*");  // or input[0] if passed
			intent.putExtra(Intent.EXTRA_MIME_TYPES, input);
			intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
			return intent;
		}

		@Override
		public List<Uri> parseResult(int resultCode, @Nullable Intent intent) {
			List<Uri> uris = new ArrayList<>();
			if (intent == null) return uris;
			if (intent.getClipData() != null) {
				for (int i = 0; i < intent.getClipData().getItemCount(); i++) {
					uris.add(intent.getClipData().getItemAt(i).getUri());
				}
			} else if (intent.getData() != null) {
				uris.add(intent.getData());
			}
			return uris;
		}
	}

	public static class GetMultipleImagesAdvanced extends GetMultipleContents {
		@NonNull
		@Override
		public Intent createIntent(Context context, String input) {
			Intent i = super.createIntent(context, input);
			putShowAdvancedExtra(i);
			i.setType("image/*");
			i.addFlags(FLAG_GRANT_READ_URI_PERMISSION);
			i.putExtra(EXTRA_MIME_TYPES, getSupportedImageContentTypes());
			return i;
		}
	}

	public static class OpenImageDocumentAdvanced extends OpenDocument {
		@NonNull
		@Override
		public Intent createIntent(Context context, String[] input) {
			Intent i = super.createIntent(context, input);
			putShowAdvancedExtra(i);
			i.setType("image/*");
			i.addFlags(FLAG_GRANT_READ_URI_PERMISSION);
			i.putExtra(EXTRA_MIME_TYPES, getSupportedImageContentTypes());
			return i;
		}
	}

	public static class OpenMultipleImageDocumentsAdvanced
			extends OpenMultipleDocuments {
		@NonNull
		@Override
		public Intent createIntent(Context context, String[] input) {
			Intent i = super.createIntent(context, input);
			putShowAdvancedExtra(i);
			i.setType("image/*");
			i.addFlags(FLAG_GRANT_READ_URI_PERMISSION);
			i.putExtra(EXTRA_MIME_TYPES, getSupportedImageContentTypes());
			return i;
		}
	}

	public static class RequestBluetoothDiscoverable
			extends ActivityResultContract<Integer, Boolean> {
		@NonNull
		@Override
		public Intent createIntent(Context context, Integer duration) {
			Intent i = new Intent(ACTION_REQUEST_DISCOVERABLE);
			i.putExtra(EXTRA_DISCOVERABLE_DURATION, duration);
			return i;
		}

		@Override
		public Boolean parseResult(int resultCode, @Nullable Intent intent) {
			return resultCode != RESULT_CANCELED;
		}
	}

	private static void putShowAdvancedExtra(Intent i) {
		i.putExtra(SDK_INT <= 28 ? "android.content.extra.SHOW_ADVANCED" :
				"android.provider.extra.SHOW_ADVANCED", true);
	}

}

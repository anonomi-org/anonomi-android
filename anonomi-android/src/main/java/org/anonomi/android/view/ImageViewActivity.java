package org.anonomi.android.view;

import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class ImageViewActivity extends AppCompatActivity {

	private static final String EXTRA_IMAGE_DATA = "imageData";

	public static void start(Context context, byte[] imageData) {
		Intent intent = new Intent(context, ImageViewActivity.class);
		intent.putExtra(EXTRA_IMAGE_DATA, imageData);
		context.startActivity(intent);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		ImageView imageView = new ImageView(this);
		imageView.setBackgroundColor(0xFF000000);
		imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
		imageView.setOnClickListener(v -> finish());
		setContentView(imageView);

		byte[] data = getIntent().getByteArrayExtra(EXTRA_IMAGE_DATA);
		if (data != null) {
			android.graphics.Bitmap bitmap =
					BitmapFactory.decodeByteArray(data, 0, data.length);
			if (bitmap != null) {
				imageView.setImageBitmap(bitmap);
			} else {
				finish();
			}
		} else {
			finish();
		}
	}

}

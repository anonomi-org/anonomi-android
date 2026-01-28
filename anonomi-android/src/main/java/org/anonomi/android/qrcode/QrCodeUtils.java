package org.anonomi.android.qrcode;

import android.graphics.Bitmap;
import android.util.DisplayMetrics;

import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import org.briarproject.nullsafety.NotNullByDefault;

import java.util.logging.Logger;

import javax.annotation.Nullable;

import android.content.Context;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.LuminanceSource;

import static android.graphics.Bitmap.Config.ARGB_8888;
import static android.graphics.Color.BLACK;
import static android.graphics.Color.WHITE;
import static com.google.zxing.BarcodeFormat.QR_CODE;
import static java.util.logging.Level.WARNING;
import static java.util.logging.Logger.getLogger;
import static org.anonchatsecure.bramble.util.LogUtils.logException;

@NotNullByDefault
public class QrCodeUtils {
	public static final double HOTSPOT_QRCODE_FACTOR = 0.35;

	private static final Logger LOG = getLogger(QrCodeUtils.class.getName());

	@Nullable
	public static Bitmap createQrCode(DisplayMetrics dm, String input) {
		return createQrCode(Math.min(dm.widthPixels, dm.heightPixels), input);
	}

	@Nullable
	public static Bitmap createQrCode(int edgeLen, String input) {
		try {
			// Generate QR code
			BitMatrix encoded = new QRCodeWriter().encode(input, QR_CODE,
					edgeLen, edgeLen);
			return renderQrCode(encoded);
		} catch (WriterException e) {
			logException(LOG, WARNING, e);
			return null;
		}
	}

	@Nullable
	public static String decodeQrFromBitmap(Bitmap bitmap) {
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		int[] pixels = new int[width * height];
		bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

		LuminanceSource source = new RGBLuminanceSource(width, height, pixels);
		BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));

		try {
			Result result = new MultiFormatReader().decode(binaryBitmap);
			return result.getText();
		} catch (NotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Nullable
	public static String decodeQrFromImage(Uri uri, Context context) {
		try {
			Bitmap bitmap;
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
				ImageDecoder.Source source = ImageDecoder.createSource(context.getContentResolver(), uri);
				bitmap = ImageDecoder.decodeBitmap(source, (decoder, info, src) -> {
					decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
				});
			} else {
				bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
			}
			return decodeQrFromBitmap(bitmap);
		} catch (Exception e) {
			return null;
		}
	}

	private static Bitmap renderQrCode(BitMatrix matrix) {
		int width = matrix.getWidth();
		int height = matrix.getHeight();
		int[] pixels = new int[width * height];
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				pixels[y * width + x] = matrix.get(x, y) ? BLACK : WHITE;
			}
		}
		Bitmap qr = Bitmap.createBitmap(width, height, ARGB_8888);
		qr.setPixels(pixels, 0, width, 0, 0, width, height);
		return qr;
	}

}

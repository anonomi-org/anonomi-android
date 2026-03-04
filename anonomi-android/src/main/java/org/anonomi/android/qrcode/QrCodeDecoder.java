package org.anonomi.android.qrcode;

import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Reader;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import org.anonchatsecure.bramble.api.lifecycle.IoExecutor;
import org.anonchatsecure.bramble.api.system.AndroidExecutor;
import org.briarproject.nullsafety.MethodsNotNullByDefault;
import org.briarproject.nullsafety.NotNullByDefault;
import org.briarproject.nullsafety.ParametersNotNullByDefault;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

import androidx.annotation.UiThread;

import static com.google.zxing.DecodeHintType.CHARACTER_SET;
import static java.util.logging.Logger.getLogger;

@MethodsNotNullByDefault
@ParametersNotNullByDefault
public class QrCodeDecoder implements PreviewConsumer, PreviewCallback {

	private static final Logger LOG = getLogger(QrCodeDecoder.class.getName());

	private final AndroidExecutor androidExecutor;
	private final Executor ioExecutor;
	private final Reader reader = new QRCodeReader();
	private final ResultCallback callback;
	private final Map<DecodeHintType, Object> hints;     // TRY_HARDER, full frame
	private final Map<DecodeHintType, Object> fastHints; // no TRY_HARDER, center crop

	private Camera camera = null;

	public QrCodeDecoder(AndroidExecutor androidExecutor,
			@IoExecutor Executor ioExecutor, ResultCallback callback) {
		this.androidExecutor = androidExecutor;
		this.ioExecutor = ioExecutor;
		this.callback = callback;
		hints = new EnumMap<>(DecodeHintType.class);
		hints.put(CHARACTER_SET, "ISO8859_1");
		hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
		fastHints = new EnumMap<>(DecodeHintType.class);
		fastHints.put(CHARACTER_SET, "ISO8859_1");
	}

	@Override
	public void start(Camera camera, int cameraIndex) {
		this.camera = camera;
		askForPreviewFrame();
	}

	@Override
	public void stop() {
		camera = null;
	}

	@UiThread
	private void askForPreviewFrame() {
		if (camera != null) camera.setOneShotPreviewCallback(this);
	}

	@UiThread
	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {
		if (camera == this.camera) {
			try {
				Size size = camera.getParameters().getPreviewSize();
				// NV21 format: width * height bytes of Y + width * height / 2
				// bytes of interleaved U and V. Some devices pad rows to a
				// stride larger than width, so accept data.length >= minimum.
				int minSize = size.width * size.height * 3 / 2;
				if (data.length >= minSize) {
					int stride = (data.length * 2) / (size.height * 3);
					if (stride < size.width) stride = size.width;
					decode(data, size.width, size.height, stride);
				} else {
					askForPreviewFrame();
				}
			} catch (RuntimeException e) {
				// Error getting camera parameters; keep the loop alive
				askForPreviewFrame();
			}
		}
	}

	private void decode(byte[] data, int width, int height, int stride) {
		ioExecutor.execute(() -> {
			try {
				// --- Fast path: centre crop, no TRY_HARDER (~10–30 ms per frame) ---
				// Point camera at the QR code — it will be in the centre 70% of the frame.
				// Cropping makes the QR occupy a larger fraction of the analysed area so
				// ZXing's 1/3–2/3 row/column sampling reliably hits the finder patterns
				// without needing TRY_HARDER.
				int cropL = width / 6;   // 16.7 % from each side
				int cropT = height / 6;
				int cropW = width  * 2 / 3;
				int cropH = height * 2 / 3;
				LuminanceSource cropSrc = new PlanarYUVLuminanceSource(
						data, stride, height, cropL, cropT, cropW, cropH, false);

				// 1. Centre crop, HybridBinarizer (fast)
				try {
					Result r = reader.decode(
							new BinaryBitmap(new HybridBinarizer(cropSrc)), fastHints);
					callback.onQrCodeDecoded(r);
					return;
				} catch (ReaderException e) {
					reader.reset();
				}

				// 2. Centre crop, GlobalHistogramBinarizer (fast, better for low contrast)
				try {
					Result r = reader.decode(
							new BinaryBitmap(new GlobalHistogramBinarizer(cropSrc)), fastHints);
					callback.onQrCodeDecoded(r);
					return;
				} catch (ReaderException e) {
					reader.reset();
				}

				// --- Thorough path: full frame + TRY_HARDER (~150–300 ms per frame) ---
				// Fallback for QR codes that are small, off-centre, or partially obscured.
				LuminanceSource fullSrc = new PlanarYUVLuminanceSource(
						data, stride, height, 0, 0, width, height, false);

				// 3. Full frame, HybridBinarizer + TRY_HARDER
				try {
					Result r = reader.decode(
							new BinaryBitmap(new HybridBinarizer(fullSrc)), hints);
					callback.onQrCodeDecoded(r);
					return;
				} catch (ReaderException e) {
					reader.reset();
				}

				// 4. Full frame, GlobalHistogramBinarizer + TRY_HARDER
				try {
					Result r = reader.decode(
							new BinaryBitmap(new GlobalHistogramBinarizer(fullSrc)), hints);
					callback.onQrCodeDecoded(r);
					return;
				} catch (ReaderException e) {
					reader.reset();
				}

				// --- Inverted path: white QR on dark background (common on screens) ---
				// ZXing 3.3.3 has no invert(); negate Y plane bytes manually.
				byte[] inv = data.clone();
				int yPlaneEnd = stride * height;
				for (int i = 0; i < yPlaneEnd; i++) {
					inv[i] = (byte) (255 - (inv[i] & 0xFF));
				}
				LuminanceSource invSrc = new PlanarYUVLuminanceSource(
						inv, stride, height, 0, 0, width, height, false);

				// 5. Inverted, HybridBinarizer + TRY_HARDER
				try {
					Result r = reader.decode(
							new BinaryBitmap(new HybridBinarizer(invSrc)), hints);
					callback.onQrCodeDecoded(r);
					return;
				} catch (ReaderException e) {
					reader.reset();
				}

				// 6. Inverted, GlobalHistogramBinarizer + TRY_HARDER
				try {
					Result r = reader.decode(
							new BinaryBitmap(new GlobalHistogramBinarizer(invSrc)), hints);
					callback.onQrCodeDecoded(r);
				} catch (ReaderException e) {
					// No barcode found in this frame
				}
			} catch (Throwable e) {
				LOG.warning("Decode error: " + e);
			} finally {
				reader.reset();
				androidExecutor.runOnUiThread(this::askForPreviewFrame);
			}
		});
	}

	@NotNullByDefault
	public interface ResultCallback {
		@IoExecutor
		void onQrCodeDecoded(Result result);
	}
}

package org.anonomi.android.qrcode;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.Manifest;
import android.content.pm.PackageManager;

import com.google.zxing.Result;

import org.anonomi.R;
import org.anonchatsecure.bramble.api.system.AndroidExecutor;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

public class ScanMapUrlFragment extends Fragment implements QrCodeDecoder.ResultCallback {

	private static final int CAMERA_PERMISSION_REQUEST_CODE = 2001;

	private QrCodeDecoder decoder;
	private CameraView cameraView;

	private final Handler mainHandler = new Handler(Looper.getMainLooper());
	private final Executor ioExecutor = Executors.newSingleThreadExecutor();

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
			@Nullable Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_qr_scanner, container, false);
		cameraView = v.findViewById(R.id.camera_view);

		decoder = new QrCodeDecoder(new AndroidExecutor() {
			@Override
			public void runOnUiThread(Runnable r) {
				mainHandler.post(r);
			}

			@Override
			public void runOnBackgroundThread(Runnable r) {
				ioExecutor.execute(r);
			}

			@Override
			public <V> Future<V> runOnUiThread(Callable<V> callable) {
				FutureTask<V> task = new FutureTask<>(callable);
				mainHandler.post(task);
				return task;
			}

			@Override
			public <V> Future<V> runOnBackgroundThread(Callable<V> callable) {
				FutureTask<V> task = new FutureTask<>(callable);
				ioExecutor.execute(task);
				return task;
			}

			public void assertUiThread() {
			}
		}, ioExecutor, this);

		cameraView.setPreviewConsumer(decoder);
		return v;
	}

	@Override
	public void onStart() {
		super.onStart();
		if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
				== PackageManager.PERMISSION_GRANTED) {
			startCameraSafe();
		} else {
			ActivityCompat.requestPermissions(requireActivity(),
					new String[]{Manifest.permission.CAMERA},
					CAMERA_PERMISSION_REQUEST_CODE);
		}
	}

	@Override
	public void onStop() {
		super.onStop();
		if (cameraView != null) {
			try {
				cameraView.stop();
			} catch (CameraException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions,
			int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				startCameraSafe();
			} else {
				Toast.makeText(getActivity(), R.string.camera_permission_denied,
						Toast.LENGTH_LONG).show();
				requireActivity().finish();
			}
		}
	}

	private void startCameraSafe() {
		if (cameraView != null) {
			try {
				cameraView.start();
			} catch (CameraException e) {
				e.printStackTrace();
				Toast.makeText(getActivity(), R.string.camera_error, Toast.LENGTH_LONG).show();
				requireActivity().finish();
			}
		}
	}

	// Called on the IO thread (@IoExecutor)
	@Override
	public void onQrCodeDecoded(Result result) {
		String text = result.getText().trim();
		if (text.startsWith("http")) {
			Intent intent = new Intent();
			intent.putExtra(ScanMapUrlActivity.EXTRA_URL, text);
			mainHandler.post(() -> {
				Activity activity = getActivity();
				if (activity != null && !activity.isFinishing()) {
					activity.setResult(Activity.RESULT_OK, intent);
					activity.finish();
				}
			});
		} else {
			mainHandler.post(() -> {
				Activity activity = getActivity();
				if (activity != null) {
					Toast.makeText(activity, R.string.qr_not_a_url,
							Toast.LENGTH_SHORT).show();
				}
			});
		}
	}
}

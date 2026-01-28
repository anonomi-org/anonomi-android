package org.anonomi.android.qrcode;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.zxing.Result;

import org.anonomi.R;
import org.anonchatsecure.bramble.api.system.AndroidExecutor;

import android.Manifest;
import android.content.pm.PackageManager;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

public class ScanSignatureQrFragment extends Fragment implements QrCodeDecoder.ResultCallback {

	private QrCodeDecoder decoder;
	private CameraView cameraView;

	private boolean permissionRequested = false;


	private static final int CAMERA_PERMISSION_REQUEST_CODE = 1001;
	private final Executor ioExecutor = Executors.newSingleThreadExecutor();

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_qr_scanner, container, false);
		cameraView = v.findViewById(R.id.camera_view);

		decoder = new QrCodeDecoder(new AndroidExecutor() {
			@Override
			public void runOnUiThread(Runnable r) {
				requireActivity().runOnUiThread(r);
			}

			@Override
			public void runOnBackgroundThread(Runnable r) {
				ioExecutor.execute(r);
			}

			@Override
			public <V> Future<V> runOnUiThread(Callable<V> callable) {
				FutureTask<V> task = new FutureTask<>(callable);
				runOnUiThread(task);
				return task;
			}

			@Override
			public <V> Future<V> runOnBackgroundThread(Callable<V> callable) {
				FutureTask<V> task = new FutureTask<>(callable);
				ioExecutor.execute(task);
				return task;
			}

			public void assertUiThread() {
				// Optional UI thread check
			}
		}, ioExecutor, this);

		cameraView.setPreviewConsumer(decoder);
		return v;
	}

	@Override
	public void onStart() {
		super.onStart();
		if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
				!= PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(requireActivity(),
					new String[]{Manifest.permission.CAMERA},
					CAMERA_PERMISSION_REQUEST_CODE);
		} else {
			startCameraSafe();
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
	public void onResume() {
		super.onResume();
		if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
				== PackageManager.PERMISSION_GRANTED) {
			startCameraSafe();
		} else if (!permissionRequested) {
			permissionRequested = true;
			ActivityCompat.requestPermissions(requireActivity(),
					new String[]{Manifest.permission.CAMERA},
					CAMERA_PERMISSION_REQUEST_CODE);
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				// Run startCameraSafe() onResume after permission is granted
				// Let onResume() handle it when fragment resumes
			} else {
				Toast.makeText(getActivity(), R.string.camera_permission_denied, Toast.LENGTH_LONG).show();
				requireActivity().finish();
			}
		}
	}

	@Override
	public void onQrCodeDecoded(Result result) {
		String data = result.getText().trim();
		try {
			byte[] sig = Base64.decode(data, Base64.DEFAULT);
			if (sig.length == 64) {
				Intent i = new Intent();
				i.putExtra("signature", data);
				requireActivity().setResult(Activity.RESULT_OK, i);
				requireActivity().finish();
			} else {
				Toast.makeText(getActivity(), R.string.qr_code_invalid, Toast.LENGTH_LONG).show();
			}
		} catch (IllegalArgumentException e) {
			Toast.makeText(getActivity(), R.string.qr_code_invalid, Toast.LENGTH_LONG).show();
		}
	}
}
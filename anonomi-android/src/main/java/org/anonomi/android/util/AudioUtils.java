package org.anonomi.android.util;

import android.util.Log;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.SecureRandom;
import java.util.Arrays;

public class AudioUtils {

	private static final int FFT_SIZE = 512;
	private static final int HOP_SIZE = 128;

	public static byte[] distortPcm(byte[] pcmIn) {
		try {
			SecureRandom rng = new SecureRandom();

			short[] input = new short[pcmIn.length / 2];
			ByteBuffer.wrap(pcmIn).order(ByteOrder.LITTLE_ENDIAN)
					.asShortBuffer().get(input);

			// Always at least 5.5 semitones down; excludes values near 1.0
			float pitchFactor = 0.58f + rng.nextFloat() * 0.15f;

			// Shift formants up or down by at least 15%; bidirectional for unpredictability
			float formantWarp = rng.nextBoolean()
					? 0.75f + rng.nextFloat() * 0.10f
					: 1.15f + rng.nextFloat() * 0.13f;

			float[] signal = shortsToFloats(input);

			// Compensate so the two transforms stay independent after resampling
			float effectiveWarp = formantWarp / pitchFactor;
			signal = spectralWarp(signal, effectiveWarp);
			signal = resampleFloats(signal, pitchFactor);

			short[] output = floatsToShorts(signal);
			ByteBuffer buf = ByteBuffer.allocate(output.length * 2)
					.order(ByteOrder.LITTLE_ENDIAN);
			for (short s : output) buf.putShort(s);
			return buf.array();

		} catch (Exception e) {
			Log.e("AudioUtils", "Distortion failed", e);
			return null;
		}
	}

	/**
	 * STFT-based spectral magnitude warping.
	 *
	 * For each overlapping frame the magnitude spectrum is stretched ({@code warp > 1})
	 * or compressed ({@code warp < 1}) along the frequency axis via linear
	 * interpolation.  Phases are preserved.  Overlap-add with a Hann window
	 * (75 % overlap) reconstructs the signal.
	 */
	private static float[] spectralWarp(float[] samples, float warp) {
		float[] window = hannWindow(FFT_SIZE);
		int outLen = samples.length + FFT_SIZE;
		float[] output   = new float[outLen];
		float[] winAccum = new float[outLen];

		float[] real  = new float[FFT_SIZE];
		float[] imag  = new float[FFT_SIZE];
		int halfSize = FFT_SIZE / 2 + 1;
		float[] mag   = new float[halfSize];
		float[] phase = new float[halfSize];

		for (int hop = 0; hop < samples.length; hop += HOP_SIZE) {
			Arrays.fill(real, 0f);
			Arrays.fill(imag, 0f);
			for (int i = 0; i < FFT_SIZE; i++) {
				int idx = hop + i;
				real[i] = (idx < samples.length ? samples[idx] : 0f) * window[i];
			}

			fft(real, imag, false);

			for (int i = 0; i < halfSize; i++) {
				mag[i]   = (float) Math.sqrt(real[i] * real[i] + imag[i] * imag[i]);
				phase[i] = (float) Math.atan2(imag[i], real[i]);
			}

			float[] newMag = new float[halfSize];
			for (int i = 0; i < halfSize; i++) {
				float src = i / warp;
				int   b0  = (int) src;
				float frac = src - b0;
				if (b0 + 1 >= halfSize) {
					newMag[i] = 0f;
				} else {
					newMag[i] = (1f - frac) * mag[b0] + frac * mag[b0 + 1];
				}
			}

			for (int i = 0; i < halfSize; i++) {
				real[i] = newMag[i] * (float) Math.cos(phase[i]);
				imag[i] = newMag[i] * (float) Math.sin(phase[i]);
			}
			for (int i = halfSize; i < FFT_SIZE; i++) {
				real[i] =  real[FFT_SIZE - i];
				imag[i] = -imag[FFT_SIZE - i];
			}

			fft(real, imag, true);

			for (int i = 0; i < FFT_SIZE; i++) {
				int outIdx = hop + i;
				if (outIdx < outLen) {
					output[outIdx]   += real[i] * window[i];
					winAccum[outIdx] += window[i] * window[i];
				}
			}
		}

		for (int i = 0; i < samples.length; i++) {
			if (winAccum[i] > 1e-6f) output[i] /= winAccum[i];
		}
		return Arrays.copyOf(output, samples.length);
	}

	private static float[] resampleFloats(float[] samples, float factor) {
		int outLen = Math.max(1, (int) (samples.length / factor));
		float[] out = new float[outLen];
		for (int i = 0; i < outLen; i++) {
			float srcIdx = i * factor;
			int   i0    = (int) srcIdx;
			int   i1    = Math.min(i0 + 1, samples.length - 1);
			float frac  = srcIdx - i0;
			out[i] = (1f - frac) * samples[i0] + frac * samples[i1];
		}
		return out;
	}

	private static float[] hannWindow(int size) {
		float[] w = new float[size];
		for (int i = 0; i < size; i++)
			w[i] = 0.5f * (1f - (float) Math.cos(2 * Math.PI * i / (size - 1)));
		return w;
	}

	private static float[] shortsToFloats(short[] s) {
		float[] f = new float[s.length];
		for (int i = 0; i < s.length; i++) f[i] = s[i] / 32768f;
		return f;
	}

	private static short[] floatsToShorts(float[] f) {
		short[] s = new short[f.length];
		for (int i = 0; i < f.length; i++) {
			float v = f[i] * 32768f;
			s[i] = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, v));
		}
		return s;
	}

	private static void fft(float[] real, float[] imag, boolean inverse) {
		int n = real.length;

		for (int i = 1, j = 0; i < n; i++) {
			int bit = n >> 1;
			for (; (j & bit) != 0; bit >>= 1) j ^= bit;
			j ^= bit;
			if (i < j) {
				float t;
				t = real[i]; real[i] = real[j]; real[j] = t;
				t = imag[i]; imag[i] = imag[j]; imag[j] = t;
			}
		}

		for (int len = 2; len <= n; len <<= 1) {
			double ang = 2 * Math.PI / len * (inverse ? -1 : 1);
			float wRe = (float) Math.cos(ang);
			float wIm = (float) Math.sin(ang);
			for (int i = 0; i < n; i += len) {
				float curRe = 1f, curIm = 0f;
				for (int j = 0; j < len / 2; j++) {
					int a = i + j, b = i + j + len / 2;
					float uRe = real[a], uIm = imag[a];
					float vRe = real[b] * curRe - imag[b] * curIm;
					float vIm = real[b] * curIm + imag[b] * curRe;
					real[a] = uRe + vRe;  imag[a] = uIm + vIm;
					real[b] = uRe - vRe;  imag[b] = uIm - vIm;
					float nextRe = curRe * wRe - curIm * wIm;
					curIm = curRe * wIm + curIm * wRe;
					curRe = nextRe;
				}
			}
		}

		if (inverse) {
			for (int i = 0; i < n; i++) { real[i] /= n; imag[i] /= n; }
		}
	}

	private static short[] decodeWavToPCM(InputStream is, int[] sampleRate, int[] channels) throws IOException {
		is.skip(44);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] buf = new byte[1024];
		int len;
		while ((len = is.read(buf)) != -1) baos.write(buf, 0, len);
		byte[] pcmBytes = baos.toByteArray();

		short[] pcm = new short[pcmBytes.length / 2];
		ByteBuffer.wrap(pcmBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(pcm);

		sampleRate[0] = 16000;
		channels[0] = 1;
		return pcm;
	}

	public static void writeWavFile(File file, byte[] pcmData, int sampleRate, int channels) throws IOException {
		try (FileOutputStream fos = new FileOutputStream(file)) {
			int bitsPerSample = 16;
			int byteRate = sampleRate * channels * bitsPerSample / 8;
			int dataSize = pcmData.length;
			int fileSize = 44 + dataSize;

			ByteBuffer header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN);
			header.put("RIFF".getBytes());
			header.putInt(fileSize - 8);
			header.put("WAVE".getBytes());
			header.put("fmt ".getBytes());
			header.putInt(16);
			header.putShort((short) 1);
			header.putShort((short) channels);
			header.putInt(sampleRate);
			header.putInt(byteRate);
			header.putShort((short) (channels * bitsPerSample / 8));
			header.putShort((short) bitsPerSample);
			header.put("data".getBytes());
			header.putInt(dataSize);
			fos.write(header.array());

			fos.write(pcmData);
		}
	}
}

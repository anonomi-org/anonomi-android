package org.anonomi.android.util;

import android.util.Log;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class AudioUtils {

	public static byte[] distortPcm(byte[] pcmIn) {
		try {
			short[] samples = new short[pcmIn.length / 2];
			ByteBuffer.wrap(pcmIn).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(samples);

			// 🎲 Random pitch factor (irreversible)
			float pitchFactor = 0.63f + (float) Math.random() * 0.15f; // Range: 0.65 – 0.80

			int newLength = (int) (samples.length / pitchFactor);
			short[] output = new short[newLength];

			for (int i = 0; i < newLength; i++) {
				float srcIndex = i * pitchFactor;
				if (srcIndex >= samples.length - 1) break;

				int i0 = (int) srcIndex;
				int i1 = i0 + 1;
				float frac = srcIndex - i0;

				output[i] = (short) ((1 - frac) * samples[i0] + frac * samples[i1]);
			}

			ByteBuffer buffer = ByteBuffer.allocate(output.length * 2).order(ByteOrder.LITTLE_ENDIAN);
			for (short s : output) buffer.putShort(s);

			return buffer.array();

		} catch (Exception e) {
			Log.e("AudioUtils", "Distortion failed", e);
			return null;
		}
	}

	private static short[] decodeWavToPCM(InputStream is, int[] sampleRate, int[] channels) throws IOException {
		// Implement a WAV parser (or use minimal logic to skip header, extract PCM)
		// You may already have the header values if you wrote the WAV yourself.
		// Assume: skip 44 bytes, PCM 16-bit mono
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

			// WAV Header
			ByteBuffer header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN);
			header.put("RIFF".getBytes());
			header.putInt(fileSize - 8);               // ChunkSize
			header.put("WAVE".getBytes());
			header.put("fmt ".getBytes());
			header.putInt(16);                         // Subchunk1Size
			header.putShort((short) 1);                // PCM format
			header.putShort((short) channels);
			header.putInt(sampleRate);
			header.putInt(byteRate);
			header.putShort((short) (channels * bitsPerSample / 8));
			header.putShort((short) bitsPerSample);
			header.put("data".getBytes());
			header.putInt(dataSize);                   // Subchunk2Size
			fos.write(header.array());

			// PCM data
			fos.write(pcmData);
		}
	}
}
package org.anonchatsecure.bramble.transport;

import org.anonchatsecure.bramble.api.crypto.StreamEncrypter;
import org.anonchatsecure.bramble.util.ByteUtils;
import org.briarproject.nullsafety.NotNullByDefault;

import java.io.IOException;
import java.io.OutputStream;

import static org.anonchatsecure.bramble.api.transport.TransportConstants.FRAME_HEADER_LENGTH;
import static org.anonchatsecure.bramble.api.transport.TransportConstants.MAC_LENGTH;
import static org.anonchatsecure.bramble.api.transport.TransportConstants.STREAM_HEADER_LENGTH;
import static org.anonchatsecure.bramble.util.ByteUtils.INT_16_BYTES;

@NotNullByDefault
class TestStreamEncrypter implements StreamEncrypter {

	private final OutputStream out;
	private final byte[] tag;

	private boolean writeTagAndHeader = true;

	TestStreamEncrypter(OutputStream out, byte[] tag) {
		this.out = out;
		this.tag = tag;
	}

	@Override
	public void writeFrame(byte[] payload, int payloadLength,
			int paddingLength, boolean finalFrame) throws IOException {
		if (writeTagAndHeader) writeTagAndHeader();
		byte[] frameHeader = new byte[FRAME_HEADER_LENGTH];
		ByteUtils.writeUint16(payloadLength, frameHeader, 0);
		ByteUtils.writeUint16(paddingLength, frameHeader, INT_16_BYTES);
		if (finalFrame) frameHeader[0] |= 0x80;
		out.write(frameHeader);
		out.write(payload, 0, payloadLength);
		out.write(new byte[paddingLength]);
		out.write(new byte[MAC_LENGTH]);
	}

	@Override
	public void flush() throws IOException {
		if (writeTagAndHeader) writeTagAndHeader();
		out.flush();
	}

	private void writeTagAndHeader() throws IOException {
		out.write(tag);
		out.write(new byte[STREAM_HEADER_LENGTH]);
		writeTagAndHeader = false;
	}
}

/*
 * Briar Desktop
 * Copyright (C) 2025 The Briar Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.anonchatsecure.anonchat.attachment;

import org.briarproject.nullsafety.NotNullByDefault;

import java.io.IOException;
import java.io.InputStream;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * An {@link InputStream} that wraps another InputStream, counting the bytes
 * read and only allowing a limited number of bytes to be read.
 */
@NotThreadSafe
@NotNullByDefault
public class CountingInputStream extends InputStream {

	private final InputStream delegate;
	private final long maxBytesToRead;

	private long bytesRead = 0;

	public CountingInputStream(InputStream delegate, long maxBytesToRead) {
		this.delegate = delegate;
		this.maxBytesToRead = maxBytesToRead;
	}

	public long getBytesRead() {
		return bytesRead;
	}

	@Override
	public int available() throws IOException {
		return delegate.available();
	}

	@Override
	public void close() throws IOException {
		delegate.close();
	}

	@Override
	public int read() throws IOException {
		if (bytesRead == maxBytesToRead) return -1;
		int i = delegate.read();
		if (i != -1) bytesRead++;
		return i;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		if (len == 0) return 0;
		if (bytesRead == maxBytesToRead) return -1;
		if (bytesRead + len > maxBytesToRead)
			len = (int) (maxBytesToRead - bytesRead);
		int read = delegate.read(b, off, len);
		if (read != -1) bytesRead += read;
		return read;
	}
}

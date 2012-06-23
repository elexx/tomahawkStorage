package tomahawk.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

public class ByteBufferInputStream extends InputStream {

	private final ByteBuffer data;

	public ByteBufferInputStream(ByteBuffer data) {
		this.data = data;
	}

	@Override
	public int read() throws IOException {
		try {
			return data.get() & 0xFF;
		} catch (BufferUnderflowException e) {
			return -1;
		}
	}
}

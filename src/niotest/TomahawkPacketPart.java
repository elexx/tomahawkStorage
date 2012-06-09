package niotest;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class TomahawkPacketPart {
	public long length = 0;
	public Byte flags;
	public ByteBuffer data;

	public short bytesOfLengthAlreadyRead = 0;

	public NioServer server;
	public SocketChannel socketChannel;

	public TomahawkPacket generatePacket() {
		data.flip();
		return new TomahawkPacket(length, flags, data, server, socketChannel);
	}
}

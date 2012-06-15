package network;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class TomahawkPacket {
	public final long length;
	public final byte flags;
	public final ByteBuffer data;

	public final NioServer server;
	public final SocketChannel socketChannel;

	public TomahawkPacket(final long length, final byte flags, final ByteBuffer data, final NioServer server, final SocketChannel socketChannel) {
		this.length = length;
		this.flags = flags;
		this.data = data;
		this.server = server;
		this.socketChannel = socketChannel;
	}
}

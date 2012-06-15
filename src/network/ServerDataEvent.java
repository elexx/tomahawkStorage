package network;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

class ServerDataEvent {
	public final NioServer server;
	public final SocketChannel socketChannel;
	public final ByteBuffer data;

	public ServerDataEvent(final NioServer server, final SocketChannel socketChannel, final byte[] data) {
		this.server = server;
		this.socketChannel = socketChannel;
		this.data = ByteBuffer.wrap(data);
	}
}

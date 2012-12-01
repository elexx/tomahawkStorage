package tomahawk.network;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import network.Transmitter;

class TomahawkPacket {
	public final long length;
	public final byte flags;
	public final ByteBuffer data;

	public final Transmitter transmitter;
	public final SocketChannel socketChannel;

	public TomahawkPacket(final long length, final byte flags, final ByteBuffer data, final Transmitter transmitter, final SocketChannel socketChannel) {
		this.length = length;
		this.flags = flags;
		this.data = data;
		this.transmitter = transmitter;
		this.socketChannel = socketChannel;
	}
}

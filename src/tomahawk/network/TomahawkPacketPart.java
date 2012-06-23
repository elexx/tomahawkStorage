package tomahawk.network;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import network.Transmitter;

class TomahawkPacketPart {
	public long length = 0;
	public Byte flags;
	public ByteBuffer data;

	public short bytesOfLengthAlreadyRead = 0;

	public TomahawkPacket generatePacket(Transmitter transmitter, SocketChannel socketChannel) {
		data.flip();
		return new TomahawkPacket(length, flags, data, transmitter, socketChannel);
	}
}

package network;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public interface Transmitter {
	public void sendPacket(SocketChannel socketChannel, ByteBuffer data);
}

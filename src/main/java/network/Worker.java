package network;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public interface Worker {

	void processData(Transmitter transmitter, SocketChannel socketChannel, ByteBuffer dataBuffer);

	void disconnectEvent(SocketChannel socketChannel);

}

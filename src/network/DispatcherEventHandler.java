package network;

import java.nio.channels.SocketChannel;

public interface DispatcherEventHandler {

	public void connectEvent(Transmitter transmitter, SocketChannel socketChannel);

	public void disconnectEvent(SocketChannel socketChannel);

}

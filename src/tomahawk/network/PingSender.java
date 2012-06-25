package tomahawk.network;

import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.Set;

import network.DispatcherEventHandler;
import network.Transmitter;
import tomahawk.network.Protocol.Type;

public class PingSender implements DispatcherEventHandler, NewControlConnectionHandler, Runnable {

	private final Set<SocketChannel> channels = new HashSet<>();
	private Transmitter transmitter;

	@Override
	public void connectEvent(Transmitter transmitter, SocketChannel socketChannel) {
		this.transmitter = transmitter;
	}

	@Override
	public void newControlConnection(SocketChannel socketChannel) {
		channels.add(socketChannel);
	}

	@Override
	public void disconnectEvent(SocketChannel socketChannel) {
		channels.remove(socketChannel);
	}

	@Override
	public void run() {
		for (SocketChannel channel : channels) {
			transmitter.sendPacket(channel, Protocol.getPacket(Type.PING));
		}
	}

}

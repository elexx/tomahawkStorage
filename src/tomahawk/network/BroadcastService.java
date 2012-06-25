package tomahawk.network;

import java.nio.channels.SocketChannel;
import java.util.Collection;

import network.Transmitter;
import tomahawk.network.ClientConnection.ConnectionType;
import tomahawk.network.Protocol.Type;
import filesystem.NewFileCallback;

public class BroadcastService implements NewFileCallback {

	private final Collection<ClientConnection> channelClientMap;

	public BroadcastService(Collection<ClientConnection> channelClientMap) {
		this.channelClientMap = channelClientMap;
	}

	@Override
	public void newFilesAdded() {
		synchronized (channelClientMap) {
			for (ClientConnection clientConnection : channelClientMap) {
				Transmitter transmitter = clientConnection.transmitter;
				SocketChannel controlConnection = clientConnection.type2Channel.get(ConnectionType.CONTROL);
				transmitter.sendPacket(controlConnection, Protocol.getPacket(Type.METHOD_TRIGGER));
			}
		}
	}
}

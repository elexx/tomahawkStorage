package tomahawk.network;

import java.nio.channels.SocketChannel;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import network.Transmitter;

public class ClientConnection {

	public final Transmitter transmitter;
	public final Map<ConnectionType, SocketChannel> type2Channel = new EnumMap<>(ConnectionType.class);
	public final Map<SocketChannel, ConnectionType> channel2Type = new HashMap<>();
	public final UUID uuid;

	public long lastSeen;

	public int streamingId;

	public ClientConnection(UUID uuid, Transmitter transmitter) {
		this.uuid = uuid;
		this.transmitter = transmitter;
	}

	public void put(ConnectionType type, SocketChannel channel) {
		type2Channel.put(type, channel);
		channel2Type.put(channel, type);
	}

	public enum ConnectionType {
		CONTROL, DBSYNC, STREAMING
	}
}

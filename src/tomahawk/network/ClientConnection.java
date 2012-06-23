package tomahawk.network;

import java.nio.channels.SocketChannel;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClientConnection {

	public final Map<ConnectionType, SocketChannel> type2Channel = new EnumMap<>(ConnectionType.class);
	public final Map<SocketChannel, ConnectionType> channel2Type = new HashMap<>();
	public final UUID uuid;

	public ClientConnection(UUID uuid) {
		this.uuid = uuid;
	}

	public void put(ConnectionType type, SocketChannel channel) {
		type2Channel.put(type, channel);
		channel2Type.put(channel, type);
	}

	enum ConnectionType {
		CONTROL, DBSYNC
	}
}

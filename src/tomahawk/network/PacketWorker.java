package tomahawk.network;

import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.zip.InflaterInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tomahawk.network.ClientConnection.ConnectionType;
import tomahawk.network.Protocol.Type;
import tomahawk.util.ByteBufferInputStream;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

public class PacketWorker implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(PacketWorker.class);

	private final Queue<TomahawkPacket> queue = new ConcurrentLinkedQueue<>();
	private final Gson gson = new GsonBuilder().create();
	private final List<NewControlConnectionHandler> eventHandlers = new LinkedList<>();

	private final Map<SocketChannel, ClientConnection> channelClientMap;

	public PacketWorker(final Map<SocketChannel, ClientConnection> channelClientMap) {
		this.channelClientMap = channelClientMap;
	}

	public void processPacket(TomahawkPacket packet) {
		synchronized (queue) {
			queue.add(packet);
			queue.notify();
		}
	}

	public void addNewControlConnectionHandler(NewControlConnectionHandler eventHandler) {
		eventHandlers.add(eventHandler);
	}

	public void removeNewControlConnectionHandler(NewControlConnectionHandler eventHandler) {
		eventHandlers.remove(eventHandler);
	}

	@Override
	public void run() {
		Map<UUID, ClientConnection> activeKeys = new HashMap<>();

		while (true) {
			TomahawkPacket packet;
			synchronized (queue) {
				while (queue.isEmpty()) {
					try {
						queue.wait();
					} catch (InterruptedException e) {}
				}
				packet = queue.remove();
			}

			logger.info("processing packet - length: " + packet.length);

			if (Flag.isFlagSet(packet.flags, Flag.JSON)) {
				logger.info("JsonPacket");

				JsonReader reader;
				long realDataLength;
				if (Flag.isFlagSet(packet.flags, Flag.COMPRESSED)) {
					realDataLength = packet.data.getInt();
					reader = new JsonReader(new InputStreamReader(new InflaterInputStream(new ByteBufferInputStream(packet.data)), Charset.forName("UTF-8")));
				} else {
					realDataLength = packet.length;
					reader = new JsonReader(new InputStreamReader(new ByteBufferInputStream(packet.data), Charset.forName("UTF-8")));
				}

				JsonPacket jsonPacket = gson.fromJson(reader, JsonPacket.class);
				if ("whitelist".equals(jsonPacket.key)) {
					logger.info("new controlConn: " + jsonPacket.conntype);
					// control
					if ("accept-offer".equals(jsonPacket.conntype)) {
						ClientConnection client = new ClientConnection(UUID.fromString(jsonPacket.nodeid), packet.transmitter);
						client.put(ConnectionType.CONTROL, packet.socketChannel);

						channelClientMap.put(packet.socketChannel, client);
						// uuidClientMap.put(client.uuid, client);
						logger.info("UUID: " + client.uuid);

						packet.transmitter.sendPacket(packet.socketChannel, Protocol.getPacket(Type.VERSION));

						for (NewControlConnectionHandler handler : eventHandlers) {
							handler.newControlConnection(packet.socketChannel);
						}
					}
					// } else if (jsonPacket.key.startsWith("FILE_REQUEST_KEY:")) {
					// logger.info("new FILE_REQUEST_KEY");
					// stream
				} else {
					logger.info("new dbConn");
					// dbsync
					if ("accept-offer".equals(jsonPacket.conntype)) {
						ClientConnection client = activeKeys.get(UUID.fromString(jsonPacket.key));
						client.put(ConnectionType.DBSYNC, packet.socketChannel);

						channelClientMap.put(packet.socketChannel, client);

						logger.info("UUID: " + client.uuid);

						packet.transmitter.sendPacket(packet.socketChannel, Protocol.getPacket(Type.VERSION));

					} else if ("fetchops".equals(jsonPacket.method)) {
						packet.transmitter.sendPacket(packet.socketChannel, ByteBuffer.wrap(new byte[] { 0, 0, 0, 2, (1 << 4), 'o', 'k' }));

						if ("" == jsonPacket.lastop) {
							// replay all ops!

						} else {
							// get ops after lastop and replay

						}
					}
				}
			} else if (Flag.isFlagSet(packet.flags, Flag.SETUP)) {
				if (packet.length == 2 && packet.data.get() == 'o' && packet.data.get() == 'k') {
					ClientConnection client = channelClientMap.get(packet.socketChannel);
					ConnectionType type = client.channel2Type.get(packet.socketChannel);
					if (type != null && type == ConnectionType.CONTROL) {
						UUID key = UUID.randomUUID();
						activeKeys.put(key, client);
						packet.transmitter.sendPacket(packet.socketChannel, Protocol.getDbSyncPacket(key.toString()));
					} else if (type != null && type == ConnectionType.DBSYNC) {
						// nothing todo ... version check was successful
						// maybe the client will now send a "method" : "fetchops"
					}
					// if (channelClientMap.containsKey(packet.socketChannel)) {
					// // this is a control connection! - reply with syncoffer
					// packet.transmitter.sendPacket(packet.socketChannel, Protocol.getPacket(Type.DB_SYNCOFFER));
					// } else {
					//
					// }
				}
			} else if (Flag.isFlagSet(packet.flags, Flag.PING)) {
				channelClientMap.get(packet.socketChannel).lastSeen = System.currentTimeMillis();
			}

			// ClientConnection client;
			// if (clients.containsKey(packet.socketChannel)) {
			// client = clients.get(packet.socketChannel);
			// } else {
			//
			// }
			//
			//
			// ByteBuffer buffer = ByteBuffer.allocate(8 * 1024);
			// buffer.putInt(packet.data.limit());
			// buffer.put(packet.flags);
			// buffer.put(packet.data);
			// buffer.flip();
			//
			// packet.transmitter.sendPacket(packet.socketChannel, buffer);

		}
	}
	// private static void prettyprint(JsonReader reader, PrintStream writer) throws IOException {
	// while (true) {
	// JsonToken token = reader.peek();
	// switch (token) {
	// case BEGIN_ARRAY:
	// reader.beginArray();
	// writer.println("[");
	// break;
	// case END_ARRAY:
	// reader.endArray();
	// writer.println("]");
	// break;
	// case BEGIN_OBJECT:
	// reader.beginObject();
	// writer.println("{");
	// break;
	// case END_OBJECT:
	// reader.endObject();
	// writer.println("}");
	// break;
	// case NAME:
	// String name = reader.nextName();
	// writer.print(name + " : ");
	// break;
	// case STRING:
	// String s = reader.nextString();
	// writer.println(s);
	// break;
	// case NUMBER:
	// String n = reader.nextString();
	// writer.println(new BigDecimal(n));
	// break;
	// case BOOLEAN:
	// boolean b = reader.nextBoolean();
	// writer.println(b);
	// break;
	// case NULL:
	// reader.nextNull();
	// writer.println("NULL");
	// break;
	// case END_DOCUMENT:
	// return;
	// }
	// }
	// }

}

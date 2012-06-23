package tomahawk.network;

import java.io.InputStreamReader;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.zip.InflaterInputStream;

import network.DispatcherEventHandler;
import tomahawk.network.ClientConnection.ConnectionType;
import tomahawk.network.Protocol.Type;
import tomahawk.util.ByteBufferInputStream;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

public class PacketWorker implements Runnable {

	private final Queue<TomahawkPacket> queue = new ConcurrentLinkedQueue<>();
	private final Gson gson = new GsonBuilder().create();
	private DispatcherEventHandler eventHandler;

	public void processPacket(TomahawkPacket packet) {
		synchronized (queue) {
			queue.add(packet);
			queue.notify();
		}
	}

	public void setNewConnectionEventHandler(DispatcherEventHandler eventHandler) {
		this.eventHandler = eventHandler;
	}

	@Override
	public void run() {
		Map<SocketChannel, ClientConnection> channelClientMap = new HashMap<>();
		Map<UUID, ClientConnection> uuidClientMap = new HashMap<>();

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

			if (Flag.isFlagSet(packet.flags, Flag.JSON)) {
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
				if ("whitelist" == jsonPacket.key) {
					// control
					if ("accept-offer" == jsonPacket.conntype) {
						ClientConnection client = new ClientConnection(UUID.fromString(jsonPacket.nodeid));
						client.put(ConnectionType.CONTROL, packet.socketChannel);

						channelClientMap.put(packet.socketChannel, client);
						uuidClientMap.put(client.uuid, client);

						packet.transmitter.sendPacket(packet.socketChannel, Protocol.getPacket(Type.VERSION));
					}
				} else if (jsonPacket.key.startsWith("FILE_REQUEST_KEY:")) {
					// stream
				} else {
					// dbsync
					// currently ignoring the key!
					if ("accept-offer" == jsonPacket.conntype) {
						ClientConnection client = uuidClientMap.get(jsonPacket.controlid);
						client.put(ConnectionType.DBSYNC, packet.socketChannel);

						channelClientMap.put(packet.socketChannel, client);

						packet.transmitter.sendPacket(packet.socketChannel, Protocol.getPacket(Type.VERSION));

					} else if ("fetchops" == jsonPacket.method) {
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
						packet.transmitter.sendPacket(packet.socketChannel, Protocol.getPacket(Type.DB_SYNCOFFER));
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

package tomahawk.network;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
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
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonReader;

import database.TomahawkDBInterface;
import database.model.DeleteFileAction;
import database.model.FileAction;
import database.model.NewFileAction;
import database.model.Track;

public class PacketWorker implements Runnable {

	private static final Logger LOG = LoggerFactory.getLogger(PacketWorker.class);

	private final Queue<TomahawkPacket> queue = new ConcurrentLinkedQueue<>();
	private final Gson gson = new GsonBuilder().create();
	private final List<NewControlConnectionHandler> eventHandlers = new LinkedList<>();
	private final TomahawkDBInterface database;

	private final Map<SocketChannel, ClientConnection> channelClientMap;

	public PacketWorker(final Map<SocketChannel, ClientConnection> channelClientMap, TomahawkDBInterface database) {
		this.channelClientMap = channelClientMap;
		this.database = database;
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

			LOG.trace("processing packet - length: " + packet.length);
			if (!Flag.isFlagSet(packet.flags, Flag.PING)) {
				LOG.debug("Raw Packet: " + (packet.flags & 0xFF) + " " + new String(packet.data.array()));
			}

			if (Flag.isFlagSet(packet.flags, Flag.JSON)) {
				JsonReader reader;
				// long realDataLength;
				if (Flag.isFlagSet(packet.flags, Flag.COMPRESSED)) {
					// realDataLength = packet.data.getInt();
					reader = new JsonReader(new InputStreamReader(new InflaterInputStream(new ByteBufferInputStream(packet.data)), Charset.forName("UTF-8")));
				} else {
					// realDataLength = packet.length;
					reader = new JsonReader(new InputStreamReader(new ByteBufferInputStream(packet.data), Charset.forName("UTF-8")));
				}

				JsonPacket jsonPacket = gson.fromJson(reader, JsonPacket.class);
				if ("whitelist".equals(jsonPacket.key)) {
					// control
					if ("accept-offer".equals(jsonPacket.conntype)) {
						ClientConnection client = new ClientConnection(UUID.fromString(jsonPacket.nodeid), packet.transmitter);
						client.put(ConnectionType.CONTROL, packet.socketChannel);

						channelClientMap.put(packet.socketChannel, client);
						LOG.debug("new controlConn from {} ", client.uuid);

						packet.transmitter.sendPacket(packet.socketChannel, Protocol.getPacket(Type.VERSION));

						for (NewControlConnectionHandler handler : eventHandlers) {
							handler.newControlConnection(packet.socketChannel);
						}
					}
				} else if (jsonPacket.key != null && jsonPacket.key.startsWith("FILE_REQUEST_KEY:")) {
					// stream
					int id = Integer.parseInt(jsonPacket.key.split(":")[1]);
					UUID uuid = UUID.fromString(jsonPacket.controlid);
					for (ClientConnection client : channelClientMap.values()) {
						if (client.uuid.equals(uuid)) {
							channelClientMap.put(packet.socketChannel, client);
							client.put(ConnectionType.STREAMING, packet.socketChannel);
							client.streamingId = id;
							break;
						}
					}
					LOG.debug("new FILE_REQUEST_KEY from {}", uuid);
					packet.transmitter.sendPacket(packet.socketChannel, Protocol.getPacket(Type.VERSION));

				} else {
					// dbsync
					if ("accept-offer".equals(jsonPacket.conntype)) {
						ClientConnection client = activeKeys.get(UUID.fromString(jsonPacket.key));
						client.put(ConnectionType.DBSYNC, packet.socketChannel);

						channelClientMap.put(packet.socketChannel, client);

						LOG.debug("new dbConn for {}", client.uuid);
						packet.transmitter.sendPacket(packet.socketChannel, Protocol.getPacket(Type.VERSION));

					} else if ("fetchops".equals(jsonPacket.method)) {
						LOG.debug("dbsync: generating oplist since {}", jsonPacket.lastop);
						List<FileAction> fileActions;
						if (null == jsonPacket.lastop || "".equals(jsonPacket.lastop)) {
							fileActions = database.getAllFileActions();
						} else {
							fileActions = database.getFileActionsSince(UUID.fromString(jsonPacket.lastop));
						}
						for (Iterator<FileAction> fileActionIt = fileActions.iterator(); fileActionIt.hasNext();) {
							FileAction fileAction = fileActionIt.next();
							if (fileAction.getClass() == NewFileAction.class) {
								NewFileAction newFileAction = (NewFileAction) fileAction;
								JsonObject answerObject = new JsonObject();
								answerObject.addProperty("command", "addfiles");
								answerObject.addProperty("guid", newFileAction.uuid.toString());
								JsonArray files = new JsonArray();
								JsonObject file;
								for (Track track : newFileAction.newTracks) {
									file = new JsonObject();
									file.addProperty("album", track.album.name);
									file.addProperty("albumartist", track.album.artist.name);
									file.addProperty("albumpos", track.tracknumber);
									file.addProperty("artist", track.artist.name);
									file.addProperty("bitrate", track.bitrate);
									file.addProperty("composer", "");
									file.addProperty("discnumber", 0);
									file.addProperty("duration", track.duration);
									file.addProperty("hash", "");
									file.addProperty("id", track.id);
									file.addProperty("mimetype", track.mimetype);
									file.addProperty("mtime", track.createTimestamp.getTime() / 1000);
									file.addProperty("size", track.size);
									file.addProperty("track", track.title);
									file.addProperty("url", track.id);
									file.addProperty("year", track.releaseyear);
									files.add(file);
								}
								answerObject.add("files", files);
								String answerString = gson.toJson(answerObject);
								LOG.trace(answerString);
								ByteBuffer buffer = ByteBuffer.allocate(answerString.length() + 4 + 1);
								buffer.putInt(answerString.length());
								if (fileActionIt.hasNext()) {
									buffer.put(Flag.flagsToByte(Flag.DBOP, Flag.JSON, Flag.FRAGMENT));
								} else {
									buffer.put(Flag.flagsToByte(Flag.DBOP, Flag.JSON));
								}
								buffer.put(answerString.getBytes());
								buffer.flip();
								packet.transmitter.sendPacket(packet.socketChannel, buffer);
							} else if (fileAction.getClass() == DeleteFileAction.class) {
								DeleteFileAction deleteFileAction = (DeleteFileAction) fileAction;
								JsonObject answerObject = new JsonObject();
								answerObject.addProperty("command", "deletefiles");
								answerObject.addProperty("deleteAll", false);
								answerObject.addProperty("guid", deleteFileAction.uuid.toString());
								JsonArray files = new JsonArray();
								for (Integer trackId : deleteFileAction.deletedFileIds) {
									files.add(new JsonPrimitive(trackId));
								}
								answerObject.add("ids", files);
								answerObject.addProperty("guid", fileActions.get(fileActions.size() - 1).uuid.toString());
								String answerString = gson.toJson(answerObject);
								LOG.trace(answerString);
								ByteBuffer buffer = ByteBuffer.allocate(answerString.length() + 4 + 1);
								buffer.putInt(answerString.length());
								if (fileActionIt.hasNext()) {
									buffer.put(Flag.flagsToByte(Flag.DBOP, Flag.JSON, Flag.FRAGMENT));
								} else {
									buffer.put(Flag.flagsToByte(Flag.DBOP, Flag.JSON));
								}
								buffer.put(answerString.getBytes());
								buffer.flip();
								packet.transmitter.sendPacket(packet.socketChannel, buffer);
							}
						}
						packet.transmitter.sendPacket(packet.socketChannel, ByteBuffer.wrap(new byte[] { 0, 0, 0, 2, Flag.flagsToByte(Flag.DBOP), 'o', 'k' }));
					} else {
						LOG.info("unknown packet");
					}
				}
			} else if (Flag.isFlagSet(packet.flags, Flag.SETUP)) {
				if (packet.length == 2 && packet.data.get() == 'o' && packet.data.get() == 'k') {
					ClientConnection client = channelClientMap.get(packet.socketChannel);
					ConnectionType type = client.channel2Type.get(packet.socketChannel);
					if (type != null && ConnectionType.CONTROL == type) {
						LOG.trace("control connection established");
						UUID key = UUID.randomUUID();
						activeKeys.put(key, client);
						packet.transmitter.sendPacket(packet.socketChannel, Protocol.getDbSyncPacket(key.toString()));
					} else if (type != null && ConnectionType.DBSYNC == type) {
						LOG.trace("dbsync connection established");
						// nothing todo ... version check was successful
						// maybe the client will now send a "method" : "fetchops"
					} else if (type != null && ConnectionType.STREAMING == type) {
						LOG.trace("streaming connection established");
						int id = channelClientMap.get(packet.socketChannel).streamingId;
						Track track = database.getTrackById(id);
						if (track != null) {

							try {
								ReadableByteChannel channel = new FileInputStream(track.path).getChannel();

								int readBytesTotal = 0, readBytes = 0;
								while (readBytesTotal < track.size) {
									ByteBuffer buffer = ByteBuffer.allocate(4 * 1024 + 4 + 1 + 4); // never ever use any other value than this!
									buffer.position(5);
									buffer.put("data".getBytes());
									readBytes = channel.read(buffer);
									readBytesTotal += readBytes;
									buffer.flip();
									buffer.putInt(readBytes + 4);
									if (readBytesTotal < track.size) {
										buffer.put(Flag.flagsToByte(Flag.RAW, Flag.FRAGMENT));
									} else {
										LOG.debug("streamConn, sent final block of {}", track.id);
										buffer.put(Flag.flagsToByte(Flag.RAW));
									}
									buffer.rewind();
									packet.transmitter.sendPacket(packet.socketChannel, buffer);
								}
							} catch (FileNotFoundException e) {
								e.printStackTrace();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					} else {
						LOG.trace("unknown connection type!");
					}
				}
			} else if (Flag.isFlagSet(packet.flags, Flag.PING)) {
				channelClientMap.get(packet.socketChannel).lastSeen = System.currentTimeMillis();
			}
		}
	}
}

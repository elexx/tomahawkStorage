package niotest;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class RawDataWorker implements Runnable {
	private final Queue<ServerDataEvent> queue = new ConcurrentLinkedQueue<ServerDataEvent>();
	private final Map<SocketChannel, TomahawkPacketPart> packetParts = new ConcurrentHashMap<SocketChannel, TomahawkPacketPart>();

	public void processData(final NioServer server, final SocketChannel socket, final byte[] data, final int count) {
		byte[] dataCopy = Arrays.copyOf(data, count);
		synchronized (queue) {
			queue.add(new ServerDataEvent(server, socket, dataCopy));
			queue.notify();
		}
	}

	@Override
	public void run() {
		ServerDataEvent dataEvent;

		while (true) {
			synchronized (queue) {
				while (queue.isEmpty()) {
					try {
						queue.wait();
					} catch (InterruptedException e) {}
				}
				dataEvent = queue.remove();
			}

			TomahawkPacketPart packetPart = packetParts.get(dataEvent.socketChannel);
			if (null == packetPart) {
				packetPart = new TomahawkPacketPart();
				packetParts.put(dataEvent.socketChannel, packetPart);
			}
			while (packetPart.bytesOfLengthAlreadyRead < 4 && dataEvent.data.hasRemaining()) {
				packetPart.lengthAsByteArray[packetPart.bytesOfLengthAlreadyRead] = dataEvent.data.get();
				packetPart.bytesOfLengthAlreadyRead++;
			}
			if (null == packetPart.data) {
				packetPart.length = ((packetPart.lengthAsByteArray[0] << 24) + (packetPart.lengthAsByteArray[1] << 16) + (packetPart.lengthAsByteArray[2] << 8) + (packetPart.lengthAsByteArray[3] << 0));
				packetPart.data = ByteBuffer.allocate((int) packetPart.length);
			}

			if (dataEvent.data.hasRemaining()) {
				packetPart.flags = dataEvent.data.get();
			}

			if (dataEvent.data.hasRemaining()) {
				packetPart.data.put(dataEvent.data);
			}

			if (!packetPart.data.hasRemaining()) {
				packetPart.server = dataEvent.server;
				packetPart.socketChannel = dataEvent.socketChannel;
				TomahawkPacket packet = packetPart.generatePacket();
				dataEvent.server.addTomahawkPacket(packet);
				packetParts.remove(dataEvent.socketChannel);
			}
		}
	}
}

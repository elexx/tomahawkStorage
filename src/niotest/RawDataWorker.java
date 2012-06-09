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
				packetPart.length += (dataEvent.data.get() << (24 - (packetPart.bytesOfLengthAlreadyRead * 8)));
				packetPart.bytesOfLengthAlreadyRead++;
			}
			if (null == packetPart.data && 4 == packetPart.bytesOfLengthAlreadyRead) {
				packetPart.data = ByteBuffer.allocate((int) packetPart.length);
			}

			if (null == packetPart.flags && dataEvent.data.hasRemaining()) {
				packetPart.flags = dataEvent.data.get();
			}

			if (dataEvent.data.hasRemaining()) {
				if (dataEvent.data.remaining() <= packetPart.data.remaining()) {
					packetPart.data.put(dataEvent.data);
				} else {
					for (int i = packetPart.data.remaining(); i > 0; i--) {
						packetPart.data.put(dataEvent.data.get());
					}
					dataEvent.data.compact();
					dataEvent.data.flip();
					queue.add(dataEvent);
				}
			}

			if (packetPart.data != null && !packetPart.data.hasRemaining()) {
				packetPart.server = dataEvent.server;
				packetPart.socketChannel = dataEvent.socketChannel;
				TomahawkPacket packet = packetPart.generatePacket();
				dataEvent.server.addTomahawkPacket(packet);
				packetParts.remove(dataEvent.socketChannel);
			}
		}
	}
}

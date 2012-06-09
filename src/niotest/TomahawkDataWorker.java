package niotest;

import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TomahawkDataWorker implements Runnable {
	private final Queue<TomahawkPacket> queue = new ConcurrentLinkedQueue<TomahawkPacket>();

	public void processData(final TomahawkPacket packet) {
		synchronized (queue) {
			queue.add(packet);
			queue.notify();
		}
	}

	@Override
	public void run() {
		TomahawkPacket packet;

		while (true) {
			synchronized (queue) {
				while (queue.isEmpty()) {
					try {
						queue.wait();
					} catch (InterruptedException e) {}
				}
				packet = queue.remove();
			}

			ByteBuffer buffer = ByteBuffer.allocate(8 * 1024);
			buffer.putInt(packet.data.limit());
			buffer.put(packet.flags);
			buffer.flip();

			packet.server.send(packet.socketChannel, buffer);
			packet.server.send(packet.socketChannel, packet.data);
		}
	}
}

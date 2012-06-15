package network;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedTransferQueue;

import network.ChangeRequest.ChangeRequestType;

public class NioServer implements Runnable {
	private final ServerSocketChannel serverChannel;
	private final Selector selector;
	private final ByteBuffer readBuffer = ByteBuffer.allocate(8192);
	private final RawDataWorker rawDataWorker = new RawDataWorker();
	private final TomahawkDataWorker tomahawkDataWorker = new TomahawkDataWorker();
	private final Queue<ChangeRequest> pendingChanges = new ConcurrentLinkedQueue<ChangeRequest>();
	private final Map<SocketChannel, Queue<ByteBuffer>> rawDataSend = new ConcurrentHashMap<SocketChannel, Queue<ByteBuffer>>();

	public NioServer(InetSocketAddress address) throws IOException {

		new Thread(rawDataWorker).start();
		new Thread(tomahawkDataWorker).start();

		selector = Selector.open();

		serverChannel = ServerSocketChannel.open();
		serverChannel.configureBlocking(false);

		serverChannel.socket().bind(address);

		serverChannel.register(selector, SelectionKey.OP_ACCEPT);
	}

	public void addTomahawkPacket(TomahawkPacket packet) {
		tomahawkDataWorker.processData(packet);
	}

	public void send(SocketChannel socket, ByteBuffer data) {
		if (rawDataSend.containsKey(socket)) {
			Queue<ByteBuffer> queue = rawDataSend.get(socket);
			synchronized (queue) {
				queue.add(data);
			}
			pendingChanges.add(new ChangeRequest(socket, ChangeRequestType.CHANGEOPS, SelectionKey.OP_WRITE));

			selector.wakeup();
		}
	}

	@Override
	public void run() {
		while (true) {
			try {
				while (!pendingChanges.isEmpty()) {
					ChangeRequest change = pendingChanges.poll();
					switch (change.changeRequestType) {
					case CHANGEOPS:
						SelectionKey key = change.socket.keyFor(selector);
						if (key != null && key.isValid()) {
							key.interestOps(change.ops);
						}
						break;
					}
				}

				selector.select();

				Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();
				while (selectedKeys.hasNext()) {
					SelectionKey key = selectedKeys.next();
					selectedKeys.remove();

					if (!key.isValid()) {
						continue;
					}

					if (key.isAcceptable()) {
						accept(key);
					} else if (key.isReadable()) {
						read(key);
					} else if (key.isWritable()) {
						write(key);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void accept(SelectionKey key) throws IOException {
		ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
		SocketChannel socketChannel = serverSocketChannel.accept();
		socketChannel.configureBlocking(false);
		socketChannel.register(selector, SelectionKey.OP_READ);
		rawDataSend.put(socketChannel, new LinkedTransferQueue<ByteBuffer>());
	}

	private void read(SelectionKey key) throws IOException {
		SocketChannel socketChannel = (SocketChannel) key.channel();

		readBuffer.clear();

		int numRead;
		try {
			numRead = socketChannel.read(readBuffer);
		} catch (IOException e) {
			key.cancel();
			socketChannel.close();
			rawDataSend.remove(socketChannel);
			return;
		}

		if (-1 == numRead) {
			key.cancel();
			socketChannel.close();
			rawDataSend.remove(socketChannel);
			return;
		}
		rawDataWorker.processData(this, socketChannel, readBuffer.array(), numRead);
	}

	private void write(SelectionKey key) throws IOException {
		SocketChannel socketChannel = (SocketChannel) key.channel();

		Queue<ByteBuffer> queue = rawDataSend.get(socketChannel);
		synchronized (queue) {
			while (!queue.isEmpty()) {
				ByteBuffer buf = queue.peek();
				socketChannel.write(buf);
				if (buf.remaining() > 0) {
					break;
				}
				queue.poll();
			}

			if (queue.isEmpty()) {
				key.interestOps(SelectionKey.OP_READ);
			}
		}
	}

	public static void main(String[] args) {
		System.out.println(ManagementFactory.getRuntimeMXBean().getName());
		try {
			new Thread(new NioServer(new InetSocketAddress(9090))).start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

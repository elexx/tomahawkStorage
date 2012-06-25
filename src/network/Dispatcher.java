package network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedTransferQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Dispatcher implements Runnable, Transmitter {

	private final static Logger logger = LoggerFactory.getLogger(Dispatcher.class);

	private final ServerSocketChannel serverChannel;
	private final Selector selector;

	private final Queue<Runnable> synchronEventQueue = new LinkedTransferQueue<>();
	private final Map<SocketChannel, Queue<ByteBuffer>> packetSendMap = new HashMap<>();

	private final Worker dataChunkWorker;

	private final ByteBuffer readBuffer = ByteBuffer.allocate(8 * 1024);

	private boolean isRunning;
	private DispatcherEventHandler eventHandler;

	public Dispatcher(InetSocketAddress address, Worker dataChunkWorker) throws IOException {
		selector = Selector.open();

		serverChannel = ServerSocketChannel.open();
		serverChannel.configureBlocking(false);
		serverChannel.socket().bind(address);
		serverChannel.register(selector, SelectionKey.OP_ACCEPT);

		this.dataChunkWorker = dataChunkWorker;
	}

	@Override
	public void run() {
		isRunning = true;
		while (isRunning) {
			try {
				while (!synchronEventQueue.isEmpty()) {
					Runnable event = synchronEventQueue.poll();
					event.run();
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
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClosedSelectorException e) {
				return;
			}
		}
	}

	@Override
	public void sendPacket(SocketChannel socketChannel, ByteBuffer data) {
		Queue<ByteBuffer> queue = packetSendMap.get(socketChannel);
		synchronized (queue) {
			queue.add(data);
		}
		synchronEventQueue.add(SynchronEventFactory.createWriteInterestEvent(selector, socketChannel));

		selector.wakeup();
	}

	public void setNewConnectionEventHandler(DispatcherEventHandler eventHandler) {
		this.eventHandler = eventHandler;
	}

	private void accept(SelectionKey key) throws IOException {
		ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
		SocketChannel socketChannel = serverSocketChannel.accept();
		socketChannel.configureBlocking(false);
		socketChannel.register(selector, SelectionKey.OP_READ);
		packetSendMap.put(socketChannel, new LinkedTransferQueue<ByteBuffer>());
		if (eventHandler != null) {
			eventHandler.connectEvent(this, socketChannel);
		}
	}

	private void disconnect(SelectionKey key) {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		packetSendMap.remove(socketChannel);
		key.cancel();
		dataChunkWorker.disconnectEvent(socketChannel);
		if (eventHandler != null) {
			eventHandler.disconnectEvent(socketChannel);
		}
		try {
			socketChannel.close();
		} catch (IOException e) {}
	}

	private void read(SelectionKey key) {
		SocketChannel socketChannel = (SocketChannel) key.channel();

		readBuffer.clear();

		int numRead;
		try {
			numRead = socketChannel.read(readBuffer);
		} catch (IOException e) {
			disconnect(key);
			return;
		}

		if (-1 == numRead) {
			disconnect(key);
			return;
		}

		readBuffer.flip();

		dataChunkWorker.processData(this, socketChannel, readBuffer);
	}

	private void write(SelectionKey key) throws IOException {
		logger.info("write to " + key);
		SocketChannel socketChannel = (SocketChannel) key.channel();

		Queue<ByteBuffer> queue = packetSendMap.get(socketChannel);
		synchronized (queue) {
			while (!queue.isEmpty()) {
				ByteBuffer buffer = queue.peek();
				socketChannel.write(buffer);
				if (buffer.remaining() > 0) {
					break;
				}
				queue.poll();
			}

			if (queue.isEmpty()) {
				key.interestOps(SelectionKey.OP_READ);
			}
		}
	}

	public void close() {
		isRunning = false;
		try {
			selector.close();
		} catch (IOException e) {}
		try {
			serverChannel.close();
		} catch (IOException e) {}
	}

}

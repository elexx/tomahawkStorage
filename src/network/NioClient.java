package network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import network.ChangeRequest.ChangeRequestType;

public class NioClient implements Runnable {
	private InetAddress hostAddress;
	private int port;
	private Selector selector;
	private ByteBuffer readBuffer = ByteBuffer.allocate(8192);
	private List<ChangeRequest> pendingChanges = new LinkedList<ChangeRequest>();
	private Map<SocketChannel, List<ByteBuffer>> pendingData = new HashMap<SocketChannel, List<ByteBuffer>>();
	private Map<SocketChannel, RspHandler> rspHandlers = Collections.synchronizedMap(new HashMap<SocketChannel, RspHandler>());

	public NioClient(InetAddress hostAddress, int port) throws IOException {
		this.hostAddress = hostAddress;
		this.port = port;
		selector = initSelector();
	}

	public void send(byte[] data, RspHandler handler) throws IOException {
		SocketChannel socket = initiateConnection();

		rspHandlers.put(socket, handler);

		synchronized (pendingData) {
			List<ByteBuffer> queue = pendingData.get(socket);
			if (null == queue) {
				queue = new ArrayList<ByteBuffer>();
				pendingData.put(socket, queue);
			}
			queue.add(ByteBuffer.wrap(data));
		}

		selector.wakeup();
	}

	@Override
	public void run() {
		while (true) {
			try {
				synchronized (pendingChanges) {
					Iterator<ChangeRequest> changes = pendingChanges.iterator();
					while (changes.hasNext()) {
						ChangeRequest change = changes.next();
						switch (change.changeRequestType) {
						case CHANGEOPS:
							SelectionKey key = change.socket.keyFor(selector);
							key.interestOps(change.ops);
							break;
						case REGISTER:
							change.socket.register(selector, change.ops);
							break;
						}
					}
					pendingChanges.clear();
				}

				selector.select();

				Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();
				while (selectedKeys.hasNext()) {
					SelectionKey key = selectedKeys.next();
					selectedKeys.remove();

					if (!key.isValid()) {
						continue;
					}

					if (key.isConnectable()) {
						finishConnection(key);
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

	private void read(SelectionKey key) throws IOException {
		SocketChannel socketChannel = (SocketChannel) key.channel();

		readBuffer.clear();

		int numRead;
		try {
			numRead = socketChannel.read(readBuffer);
		} catch (IOException e) {
			key.cancel();
			socketChannel.close();
			return;
		}

		if (-1 == numRead) {
			key.channel().close();
			key.cancel();
			return;
		}

		handleResponse(socketChannel, readBuffer.array(), numRead);
	}

	private void handleResponse(SocketChannel socketChannel, byte[] data, int numRead) throws IOException {
		byte[] rspData = new byte[numRead];
		System.arraycopy(data, 0, rspData, 0, numRead);

		RspHandler handler = rspHandlers.get(socketChannel);

		if (handler.handleResponse(rspData)) {
			socketChannel.close();
			socketChannel.keyFor(selector).cancel();
		}
	}

	private void write(SelectionKey key) throws IOException {
		SocketChannel socketChannel = (SocketChannel) key.channel();

		synchronized (pendingData) {
			List<ByteBuffer> queue = pendingData.get(socketChannel);

			while (!queue.isEmpty()) {
				ByteBuffer buf = queue.get(0);
				socketChannel.write(buf);
				if (buf.remaining() > 0) {
					break;
				}
				queue.remove(0);
			}

			if (queue.isEmpty()) {
				key.interestOps(SelectionKey.OP_READ);
			}
		}
	}

	private void finishConnection(SelectionKey key) throws IOException {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		try {
			socketChannel.finishConnect();
		} catch (IOException e) {
			System.out.println(e);
			key.cancel();
			return;
		}

		key.interestOps(SelectionKey.OP_WRITE);
	}

	private SocketChannel initiateConnection() throws IOException {
		SocketChannel socketChannel = SocketChannel.open();
		socketChannel.configureBlocking(false);

		socketChannel.connect(new InetSocketAddress(hostAddress, port));

		synchronized (pendingChanges) {
			pendingChanges.add(new ChangeRequest(socketChannel, ChangeRequestType.REGISTER, SelectionKey.OP_CONNECT));
		}

		return socketChannel;
	}

	private Selector initSelector() throws IOException {
		return Selector.open();
	}

	public static void main(String[] args) {
		try {
			NioClient client = new NioClient(InetAddress.getByName("www.google.com"), 80);
			Thread t = new Thread(client);
			t.setDaemon(true);
			t.start();
			RspHandler handler = new RspHandler();
			client.send("GET / HTTP/1.0\r\n\r\n".getBytes(), handler);
			handler.waitForResponse();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

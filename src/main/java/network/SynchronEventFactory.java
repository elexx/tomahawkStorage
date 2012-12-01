package network;

import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class SynchronEventFactory {

	public static Runnable createWriteInterestEvent(final Selector selector, final SocketChannel socketChannel) {
		return new Runnable() {
			@Override
			public void run() {
				SelectionKey key = socketChannel.keyFor(selector);
				if (key != null && key.isValid()) {
					key.interestOps(SelectionKey.OP_WRITE);
				}
			}
		};
	}

}

package tomahawk.network;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.concurrent.TimeUnit;


public class OldConnectionKiller implements Runnable {

	private final Collection<ClientConnection> channelClientMap;
	private final long timeoutInMillis;

	public OldConnectionKiller(int i, TimeUnit unit, Collection<ClientConnection> collection) {
		channelClientMap = collection;
		timeoutInMillis = unit.toMillis(i);
	}

	@Override
	public void run() {
		long currentTime = System.currentTimeMillis();
		synchronized (channelClientMap) {
			for (ClientConnection client : channelClientMap) {
				if (client.lastSeen + timeoutInMillis < currentTime) {
					for (SocketChannel socketChannel : client.type2Channel.values()) {
						try {
							socketChannel.close();
						} catch (IOException e) {}
					}
					channelClientMap.remove(client);
				}
			}
		}
	}
}

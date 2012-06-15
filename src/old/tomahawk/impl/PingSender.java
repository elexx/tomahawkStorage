package old.tomahawk.impl;

import java.io.IOException;
import java.net.Socket;
import java.util.Set;

public class PingSender implements Runnable {
	private final Set<Socket> openConnections;

	public PingSender(final Set<Socket> openConnections) {
		this.openConnections = openConnections;
	}

	@Override
	public void run() {
		for (Socket socket : openConnections) {
			synchronized (socket) {
				try {
					socket.getOutputStream().write(Protocol.PINGPACKET.array());
					socket.getOutputStream().flush();
				} catch (IOException e) {
					e.printStackTrace();
					openConnections.remove(socket);
				}
			}
		}
	}
}

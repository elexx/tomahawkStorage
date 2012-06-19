package old.tomahawk.impl;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ExecutorService;

public class ConnectionAcceptor implements Runnable {

	private final ServerSocket serverSocket;
	private final ExecutorService cachedService;
	private final Set<Socket> sockets;

	public ConnectionAcceptor(final ServerSocket serverSocket, final ExecutorService cachedService, final Set<Socket> sockets) {
		this.serverSocket = serverSocket;
		this.cachedService = cachedService;
		this.sockets = sockets;
	}

	@Override
	public void run() {
		try {
			while (true) {
				Socket socket = serverSocket.accept();
				cachedService.submit(new ClientConnectionHandler(socket, sockets));
			}
		} catch (IOException e) {}
	}
}

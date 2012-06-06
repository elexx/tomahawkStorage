package tomahawk.impl;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import tomahawk.Network;

public class NetworkResolver implements Network {

	private ScheduledExecutorService scheduledService = Executors.newSingleThreadScheduledExecutor();
	private ExecutorService cachedService = Executors.newCachedThreadPool();
	private Thread tcpAcceptorThread;
	private Set<Socket> openConnections;

	public NetworkResolver(final String identifier, final InterfaceAddress ifaceAddress, final int port) throws IOException {
		DatagramSocket datagramSocket = new DatagramSocket(port);
		DatagramPacket broadcastPacket = new DatagramPacket(identifier.getBytes(), identifier.getBytes().length, ifaceAddress.getBroadcast(), port);
		UDPBroadcaster udpBroadcaster = new UDPBroadcaster(datagramSocket, broadcastPacket);
		scheduledService.scheduleAtFixedRate(udpBroadcaster, 0, 60, TimeUnit.SECONDS);

		scheduledService.scheduleWithFixedDelay(new PingSender(openConnections), 0, 5, TimeUnit.SECONDS);

		ServerSocket serverSocket = new ServerSocket();
		serverSocket.bind(new InetSocketAddress(ifaceAddress.getAddress(), port));
		tcpAcceptorThread = new Thread(new ConnectionAcceptor(serverSocket, cachedService, openConnections));
		tcpAcceptorThread.start();
	}

	@Override
	public void shutdown() {

	}

}

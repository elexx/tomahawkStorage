package tomahawk;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import tomahawk.network.DataChunkWorker;
import tomahawk.network.PacketWorker;
import tomahawk.network.PingSender;

import network.Dispatcher;

public class Nio2Tester {

	public static void main(String[] args) throws IOException {

		ScheduledExecutorService threadPool = Executors.newScheduledThreadPool(5);

		PingSender pingSender = new PingSender();

		PacketWorker packetWorker = new PacketWorker();
		Thread packetWorkerThread = new Thread(packetWorker);

		Dispatcher server = new Dispatcher(new InetSocketAddress(9090), new DataChunkWorker(packetWorker));
		// server.setNewConnectionEventHandler(pingSender); // nur auf der control muessen pings gesendet werden!!
		Thread serverThread = new Thread(server);

		threadPool.scheduleAtFixedRate(pingSender, 0, 5, TimeUnit.SECONDS);
		packetWorkerThread.start();
		serverThread.start();
	}

}

package tomahawk;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.file.FileSystems;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import network.Dispatcher;
import tomahawk.network.BroadcastService;
import tomahawk.network.ClientConnection;
import tomahawk.network.DataChunkWorker;
import tomahawk.network.OldConnectionKiller;
import tomahawk.network.PacketWorker;
import tomahawk.network.PingSender;
import tomahawk.network.UDPBroadcaster;
import database.TomahawkDB;
import database.TomahawkDBInterface;
import filesystem.FileScanner;

public class Nio2Tester {
	public static final int PORT = 50211;
	public static final UUID IDENTIFIER = UUID.fromString("12312312-ffff-eeee-dddd-1234567890ab");

	public static void main(String[] args) throws IOException, InterruptedException {

		Map<SocketChannel, ClientConnection> channelClientMap = Collections.synchronizedMap(new HashMap<SocketChannel, ClientConnection>());

		TomahawkDBInterface database = new TomahawkDB();
		database.connect("tomahawk.storage.jpa");

		FileScanner fileScanner = new FileScanner();
		fileScanner.addNetworkCallback(new BroadcastService(channelClientMap.values()));
		Thread fileScannerThread = new Thread(fileScanner);
		fileScannerThread.start();
		// fileScanner.processDirectory(FileSystems.getDefault().getPath("/Users/alexander/Downloads/Trifling_Wings_-_a46901_---_Jamendo_-_MP3_VBR_192k"), database);
		fileScanner.processDirectory(FileSystems.getDefault().getPath("/Users/alexander/Downloads/Public_Domain_-_a1003_---_Jamendo_-_MP3_VBR_192k"), database);
		Thread.sleep(500);

		ScheduledExecutorService threadPool = Executors.newScheduledThreadPool(5);

		PingSender pingSender = new PingSender();
		OldConnectionKiller connectionKiller = new OldConnectionKiller(10, TimeUnit.MINUTES, channelClientMap.values());

		PacketWorker packetWorker = new PacketWorker(channelClientMap, database);
		packetWorker.addNewControlConnectionHandler(pingSender);
		Thread packetWorkerThread = new Thread(packetWorker);

		Dispatcher dispatcher = new Dispatcher(new InetSocketAddress(PORT), new DataChunkWorker(packetWorker));
		dispatcher.setNewConnectionEventHandler(pingSender);

		Thread serverThread = new Thread(dispatcher);

		threadPool.scheduleAtFixedRate(pingSender, 0, 5, TimeUnit.SECONDS);
		threadPool.scheduleAtFixedRate(new UDPBroadcaster(PORT, IDENTIFIER), 0, 10, TimeUnit.SECONDS);
		threadPool.scheduleAtFixedRate(connectionKiller, 0, 15, TimeUnit.MINUTES);
		packetWorkerThread.start();
		serverThread.start();
	}
}

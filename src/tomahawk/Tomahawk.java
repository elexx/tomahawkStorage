package tomahawk;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import network.Dispatcher;

import org.apache.log4j.xml.DOMConfigurator;

import tomahawk.network.BroadcastService;
import tomahawk.network.ClientConnection;
import tomahawk.network.DataChunkWorker;
import tomahawk.network.OldConnectionKiller;
import tomahawk.network.PacketWorker;
import tomahawk.network.PingSender;
import tomahawk.network.UDPBroadcaster;
import tomahawk.util.Config;
import tomahawk.util.Config.ConfigKey;
import tomahawk.util.WatchedFolder;
import database.TomahawkDB;
import database.TomahawkDBInterface;
import filesystem.FileScanner;

public class Tomahawk {

	public static void main(String[] args) throws IOException, InterruptedException {
		DOMConfigurator.configure("log4j.xml");

		Config config = new Config("storage.conf");
		Map<String, String> persistenceOverrideMap = config.getValuesWithPrefix("javax.persistence.jdbc", "hibernate", "dialect");

		Map<SocketChannel, ClientConnection> channelClientMap = Collections.synchronizedMap(new HashMap<SocketChannel, ClientConnection>());

		TomahawkDBInterface database = new TomahawkDB();
		database.connect("tomahawk.storage.jpa", persistenceOverrideMap);

		WatchedFolder folders = new WatchedFolder("watchedFolders.conf");

		FileScanner fileScanner = new FileScanner(database);
		fileScanner.addNetworkCallback(new BroadcastService(channelClientMap.values()));
		Thread fileScannerThread = new Thread(fileScanner);
		fileScannerThread.start();
		for (Path path : folders) {
			fileScanner.watchDirectory(path);
		}
		fileScanner.asyncScanWatchedDirectories();
		Thread.sleep(500);

		ScheduledExecutorService threadPool = Executors.newScheduledThreadPool(5);

		PingSender pingSender = new PingSender();
		OldConnectionKiller connectionKiller = new OldConnectionKiller(10, TimeUnit.MINUTES, channelClientMap.values());

		PacketWorker packetWorker = new PacketWorker(channelClientMap, database);
		packetWorker.addNewControlConnectionHandler(pingSender);
		Thread packetWorkerThread = new Thread(packetWorker);

		Dispatcher dispatcher = new Dispatcher(new InetSocketAddress(config.getInt(ConfigKey.port)), new DataChunkWorker(packetWorker));
		dispatcher.setNewConnectionEventHandler(pingSender);

		Thread serverThread = new Thread(dispatcher);

		threadPool.scheduleAtFixedRate(pingSender, 0, 5, TimeUnit.SECONDS);
		threadPool.scheduleAtFixedRate(new UDPBroadcaster(config.getInt(ConfigKey.port), UUID.fromString(config.get(ConfigKey.uuid)), config.get(ConfigKey.readable_name)), 0, 30, TimeUnit.SECONDS);
		threadPool.scheduleAtFixedRate(connectionKiller, 0, 15, TimeUnit.MINUTES);
		packetWorkerThread.start();
		serverThread.start();
	}
}

package tomahawk;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.FileSystems;

import network.NioServer;
import database.TomahawkDB;
import database.TomahawkDBInterface;
import filesystem.FileScanner;

public class Tomahawk extends Thread {
	private InetSocketAddress serverAddress;
	private NioServer server;
	private Thread serverThread;
	private TomahawkDBInterface database;
	private FileScanner fileScanner;

	public Tomahawk() throws IOException {
		Runtime.getRuntime().addShutdownHook(this);

		serverAddress = new InetSocketAddress(9090);
		server = new NioServer(serverAddress);
		serverThread = new Thread(server);

		database = new TomahawkDB();
		database.connect("tomahawk.storage.jpa");

		fileScanner = new FileScanner();
		fileScanner.addNetworkCallback(server);
		fileScanner.processDirectory(FileSystems.getDefault().getPath("/Users/alexander/Downloads/Trifling_Wings_-_a46901_---_Jamendo_-_MP3_VBR_192k"), database);
	}

	public static void main(String[] args) throws IOException {
		new Tomahawk();
	}

	@Override
	public void run() {

	}
}

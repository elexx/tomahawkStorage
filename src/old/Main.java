package old;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Main {

	public static void main(String[] args) throws IOException {
		DatagramSocket datagrammSocket = new DatagramSocket(50210);

		byte[] buffer = new byte[256];
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

		Map<String, ClientHandler> clients = new HashMap<String, ClientHandler>();

		do {
			System.out.println("waiting for more");
			datagrammSocket.receive(packet);

			String data = new String(packet.getData(), packet.getOffset(), packet.getLength());
			Scanner scanner = new Scanner(data);
			scanner.useDelimiter(":");
			if (scanner.hasNext() && scanner.next().equals("TOMAHAWKADVERT")) {
				int port = scanner.nextInt();
				String id = scanner.next();

				InetSocketAddress address = new InetSocketAddress(packet.getAddress(), port);
				if (!clients.containsKey(id)) {
					clients.put(id, new ClientHandler(address));
				}
			}

		} while (true);
	}
}

package old;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class UDPBroadcaster implements Runnable {

	private final DatagramSocket socket;
	private final DatagramPacket packet;

	public UDPBroadcaster(final String identifier) throws SocketException, UnknownHostException {
		socket = new DatagramSocket(50210);
		packet = new DatagramPacket(identifier.getBytes(), identifier.getBytes().length, InetAddress.getByName("255.255.255.255"), 50210);
	}

	@Override
	public void run() {
		try {
			socket.send(packet);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}

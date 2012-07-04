package tomahawk.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.UUID;

public class UDPBroadcaster implements Runnable {
	private final DatagramSocket socket;
	private final DatagramPacket packet;

	public UDPBroadcaster(final int port, final UUID uuid, final String readableName) throws SocketException, UnknownHostException {
		byte[] identifier = ("TOMAHAWKADVERT:" + port + ":" + uuid.toString() + ":" + readableName).getBytes();
		socket = new DatagramSocket();
		packet = new DatagramPacket(identifier, identifier.length, InetAddress.getByName("255.255.255.255"), 50210);
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

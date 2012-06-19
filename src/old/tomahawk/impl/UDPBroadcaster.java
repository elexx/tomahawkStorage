package old.tomahawk.impl;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class UDPBroadcaster implements Runnable {

	private final DatagramSocket socket;
	private final DatagramPacket packet;

	public UDPBroadcaster(final DatagramSocket socket, final DatagramPacket packet) {
		this.socket = socket;
		this.packet = packet;
	}

	@Override
	public void run() {
		try {
			socket.send(packet);
		} catch (IOException e) {}
	}
}

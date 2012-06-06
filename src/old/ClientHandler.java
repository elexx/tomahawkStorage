package old;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class ClientHandler implements Runnable {

	private static int SETUP = 0;

	private Socket socket;
	private Thread thread;

	public ClientHandler(InetSocketAddress address) throws IOException {
		socket = new Socket();
		socket.connect(address);

		System.out.println("New Client!! " + address);

		thread = new Thread(this);
		thread.setName("clientthread " + address);
		thread.start();
	}

	@Override
	public void run() {
		int state = SETUP;

		DataInputStream inStream;
		DataOutputStream outStream;
		try {
			outStream = new DataOutputStream(socket.getOutputStream());
			inStream = new DataInputStream(socket.getInputStream());

			System.out.println("connected " + socket.getInetAddress());

			String offer = "{ \"conntype\" : \"accept-offer\", \"key\" : \"whitelist\", \"nodeid\" : \"f7c8da94-edd4-42eb-9f24-d408ac6e707b\", \"port\" : 0 }";
			outStream.writeInt(offer.length());
			outStream.writeByte(2);
			outStream.write(offer.getBytes());

			while (true) {
				long length = inStream.readInt();
				byte flags = inStream.readByte();

				System.out.println("l: " + length);
				System.out.println("f: " + flagsToString(flags));
				System.out.print("data: ");
				for (int i = 0; i < length; i++) {
					System.out.print((char) inStream.readByte());
				}

				if (state == SETUP) {

					outStream.writeInt(2);
					outStream.writeByte(128);
					outStream.write("ok".getBytes());
					state++;
				}

				System.out.println();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private static String flagsToString(byte flags) {
		StringBuilder builder = new StringBuilder();
		if ((flags & (1 << 0)) != 0) {
			builder.append("RAW ");
		}
		if ((flags & (1 << 1)) != 0) {
			builder.append("JSON ");
		}
		if ((flags & (1 << 2)) != 0) {
			builder.append("FRAGMENT ");
		}
		if ((flags & (1 << 3)) != 0) {
			builder.append("COMPRESSED ");
		}
		if ((flags & (1 << 4)) != 0) {
			builder.append("DBOP ");
		}
		if ((flags & (1 << 5)) != 0) {
			builder.append("PING ");
		}
		if ((flags & (1 << 6)) != 0) {
			builder.append("RESERVED_1 ");
		}
		if ((flags & (1 << 7)) != 0) {
			builder.append("SETUP ");
		}
		return builder.toString();
	}
}

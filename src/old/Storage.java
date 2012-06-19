package old;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Storage {

	private static final String IDENTIFIER = "TOMAHAWKADVERT:50210:12312312-ffff-eeee-dddd-1234567890ab";

	public static void main(String[] args) throws IOException {
		ScheduledExecutorService udpTimer = Executors.newSingleThreadScheduledExecutor();
		udpTimer.scheduleAtFixedRate(new UDPBroadcaster(IDENTIFIER), 0, 60, TimeUnit.SECONDS);

		ServerSocket ssocket = new ServerSocket(50210);
		Socket socket;
		while ((socket = ssocket.accept()) != null) {
			System.out.println("new Connection");
			DataInputStream dis = new DataInputStream(socket.getInputStream());
			DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
			try {

				// should be accept-offer
				// JSON ... { "conntype" : "accept-offer", "key" : "whitelist", "nodeid" : "e7c8da94-edd4-42eb-9f24-d408ac6e707b", "port" : 0 }
				readAndEchoPacket(dis);

				// answer with versioncheck
				write(dos, 1 << 7, "4");

				// versioncheck ok
				// SETUP ... ok
				readAndEchoPacket(dis);

				write(dos, 1 << 1, "{ \"key\" : \"" + IDENTIFIER + "\", \"method\" : \"dbsync-offer\" }");

			} catch (IOException e) {
				System.out.println();
				System.out.println(e);
			}
		}

	}

	private static void write(DataOutputStream dos, int flags, String text) throws IOException {
		dos.writeInt(text.length());
		dos.writeByte(flags);
		dos.write(text.getBytes());
	}

	private static void readAndEchoPacket(DataInputStream dis) throws IOException {
		long length = dis.readInt();
		byte flags = dis.readByte();
		byte buffer[] = new byte[(int) length];
		int readCount = 0;
		while (readCount < length) {
			readCount += dis.read(buffer, readCount, (int) (length - readCount));
		}
		System.out.println(flagsToString(flags));
		System.out.println(new String(buffer, 0, buffer.length));
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

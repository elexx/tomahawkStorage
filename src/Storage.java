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
				long length = dis.readInt();
				byte flags = dis.readByte();
				byte buffer[] = new byte[(int) length];
				int readCount = 0;
				while (readCount < length) {
					readCount += dis.read(buffer, readCount, (int) (length - readCount));
				}
				System.out.println(flagsToString(flags));
				System.out.println(new String(buffer, 0, buffer.length));

				// answer with versioncheck
				dos.writeInt(1);
				dos.writeByte(1 << 7);
				dos.write("4".getBytes());

				length = dis.readInt();
				flags = dis.readByte();
				buffer = new byte[(int) length];
				readCount = 0;
				while (readCount < length) {
					readCount += dis.read(buffer, readCount, (int) (length - readCount));
				}
				System.out.println(flagsToString(flags));
				System.out.println(new String(buffer, 0, buffer.length));

			} catch (IOException e) {
				System.out.println();
				System.out.println(e);
			}
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

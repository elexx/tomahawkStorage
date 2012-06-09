package niotest;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.Socket;

public class SingleNioServerTester {
	public static void main(String[] args) throws IOException {
		Socket socket = new Socket();
		socket.connect(new InetSocketAddress("localhost", 9090));
		DataInputStream inStream = new DataInputStream(socket.getInputStream());
		DataOutputStream outStream = new DataOutputStream(socket.getOutputStream());

		outStream.write(new byte[] { 0x00, 0x00, 0x00, 0x04, 0x40, 0x11, 0x12, 0x13, 0x14 });
		outStream.flush();

		long length = inStream.readInt();
		byte flags = inStream.readByte();
		byte buffer[] = new byte[(int) length];
		int readCount = 0;
		while (readCount < length) {
			readCount += inStream.read(buffer, readCount, (int) (length - readCount));
		}
		System.out.println(flagsToString(flags));
		System.out.println(String.format("%x", new BigInteger(1, buffer)));

		inStream.close();
		outStream.close();
		socket.close();
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

package network.tests;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.zip.Deflater;

public class SingleNioServerTester {
	public static void main(String[] args) throws IOException {
		Socket socket = new Socket();
		socket.connect(new InetSocketAddress("localhost", 9090));
		DataInputStream inStream = new DataInputStream(socket.getInputStream());
		DataOutputStream outStream = new DataOutputStream(socket.getOutputStream());

		// byte[] buff1 = new byte[] { 0x00, 0x00, 0x00, 0x04, 0x40, 0x11, 0x12, 0x13, 0x14 };
		// for (byte b : buff1) {
		// try {
		// Thread.sleep(1000); // um zu sehen ob auch fragmentierte packete richtig ankommen!
		// } catch (InterruptedException e) {}
		// outStream.write(b);
		// outStream.flush();
		// }
		// outStream.write(buff1);
		// outStream.flush();
		//
		// readPacket(inStream);
		// readPacket(inStream);

		byte[] json = new String("{ \"conntype\" : \"accpt-offer\", \"controlid\" : \"e7c8da94-edd4-42eb-9f24-d408ac6e707b\" , \"key\" : \"0fba4c3d-169f-4eda-b233-b12897005403\" , \"port\" : 0 }").getBytes();

		Deflater deflater = new Deflater();
		deflater.setInput(json);
		deflater.finish();

		byte[] jsoncomp = new byte[json.length];
		int compressedDataLength = deflater.deflate(jsoncomp);

		outStream.writeInt(compressedDataLength + 4);
		outStream.write((1 << 1) | (1 << 3));
		outStream.writeInt(json.length);
		outStream.write(jsoncomp, 0, compressedDataLength);
		outStream.flush();

		inStream.close();
		outStream.close();
		socket.close();
	}

	private static void readPacket(DataInputStream inStream) throws IOException {
		long length = inStream.readInt();
		byte flags = inStream.readByte();
		System.out.println(flagsToString(flags));
		byte buffer[] = new byte[(int) length];
		int readCount = 0;
		while (readCount < length) {
			readCount += inStream.read(buffer, readCount, (int) (length - readCount));
			System.out.println(String.format("%x", new BigInteger(1, buffer)));
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

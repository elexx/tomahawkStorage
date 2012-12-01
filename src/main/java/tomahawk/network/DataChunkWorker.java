package tomahawk.network;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

import network.Transmitter;
import network.Worker;

public class DataChunkWorker implements Worker {

	private final Map<SocketChannel, TomahawkPacketPart> packetParts = new HashMap<>();
	private final PacketWorker packetWorker;

	public DataChunkWorker(PacketWorker packetWorker) {
		this.packetWorker = packetWorker;
	}

	@Override
	public void processData(Transmitter transmitter, SocketChannel socketChannel, ByteBuffer dataBuffer) {
		do {
			TomahawkPacketPart packetPart;

			if (packetParts.containsKey(socketChannel)) {
				packetPart = packetParts.get(socketChannel);
			} else {
				packetPart = new TomahawkPacketPart();
			}

			while (packetPart.bytesOfLengthAlreadyRead < 4 && dataBuffer.hasRemaining()) {
				packetPart.length += ((dataBuffer.get() & 0xFF) << (24 - (packetPart.bytesOfLengthAlreadyRead * 8)));
				packetPart.bytesOfLengthAlreadyRead++;
			}
			if (null == packetPart.data && 4 == packetPart.bytesOfLengthAlreadyRead) {
				packetPart.data = ByteBuffer.allocate((int) packetPart.length);
			}

			if (null == packetPart.flags && dataBuffer.hasRemaining()) {
				packetPart.flags = dataBuffer.get();
			}

			if (dataBuffer.hasRemaining()) {
				if (dataBuffer.remaining() <= packetPart.data.remaining()) {
					packetPart.data.put(dataBuffer);
				} else {
					for (int i = packetPart.data.remaining(); i > 0; i--) {
						packetPart.data.put(dataBuffer.get());
					}
					dataBuffer.compact();
					dataBuffer.flip();
				}
			}

			if (packetPart.data != null && !packetPart.data.hasRemaining()) {
				TomahawkPacket packet = packetPart.generatePacket(transmitter, socketChannel);
				packetWorker.processPacket(packet);
				packetParts.remove(socketChannel);
			} else {
				packetParts.put(socketChannel, packetPart);
			}
		} while (dataBuffer.hasRemaining());
	}

	@Override
	public void disconnectEvent(SocketChannel socketChannel) {
		packetParts.remove(socketChannel);
	}

}

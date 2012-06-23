package tomahawk.network;

import java.nio.ByteBuffer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

class Protocol {

	public enum Type {
		PING(PINGPACKET), OK(OKPACKET), METHOD_TRIGGER(METHOD_TRIGGER_PACKET), VERSION(VERSIONPACKET), DB_SYNCOFFER(DB_SYNCOFFERPACKET);

		private final byte[] data;

		Type(byte[] data) {
			this.data = data;
		}
	}

	public static ByteBuffer getPacket(Type type) {
		return ByteBuffer.wrap(type.data).asReadOnlyBuffer();
	}

	private static final byte[] PINGPACKET = new byte[] { 0, 0, 0, 0, Flag.flagsToByte(Flag.PING) };
	private static final byte[] OKPACKET = new byte[] { 0, 0, 0, 2, Flag.flagsToByte(Flag.SETUP), 'o', 'k' };
	private static final byte[] VERSIONPACKET = new byte[] { 0, 0, 0, 1, Flag.flagsToByte(Flag.SETUP), '4' };
	private static final byte[] METHOD_TRIGGER_PACKET;
	private static final byte[] DB_SYNCOFFERPACKET;

	private static final Gson gson = new GsonBuilder().create();

	static {
		{
			JsonObject object = new JsonObject();
			object.addProperty("method", "trigger");
			METHOD_TRIGGER_PACKET = writeJsonObjectToByteArray(object);
		}
		{
			JsonObject object = new JsonObject();
			object.addProperty("key", "704443cd-111f-4eda-beef-b12345678900");
			object.addProperty("method", "dbsync-offer");
			DB_SYNCOFFERPACKET = writeJsonObjectToByteArray(object);
		}
	}

	private static byte[] writeJsonObjectToByteArray(JsonObject object) {
		String dataString = gson.toJson(object);
		byte[] array = new byte[4 + 1 + dataString.length()];
		ByteBuffer buffer = ByteBuffer.wrap(array);
		buffer.putInt(dataString.length());
		buffer.put(Flag.flagsToByte(Flag.JSON));
		buffer.put(dataString.getBytes());
		return buffer.array();
	}

}

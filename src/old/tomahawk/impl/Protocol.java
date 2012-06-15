package old.tomahawk.impl;

import java.nio.ByteBuffer;

public class Protocol {

	public static final ByteBuffer PINGPACKET = ByteBuffer.wrap(new byte[] { 0, 0, 0, 0, Flag.PING.byteCode() });

	public enum Flag {
		RAW((byte) (1 << 0)), JSON((byte) (1 << 1)), FRAGMENT((byte) (1 << 2)), COMPRESSED((byte) (1 << 3)), DBOP((byte) (1 << 4)), PING((byte) (1 << 5)), RESERVED_1((byte) (1 << 6)), SETUP((byte) (1 << 7));

		private final byte flag;

		Flag(byte flag) {
			this.flag = flag;
		}

		public byte byteCode() {
			return flag;
		}

		public static byte flagsToByte(Flag... flags) {
			byte code = 0;
			for (Flag flag : flags) {
				code |= flag.byteCode();
			}
			return code;
		}

	}
}

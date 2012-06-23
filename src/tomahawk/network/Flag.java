package tomahawk.network;

public enum Flag {

	RAW((byte) (1 << 0)), JSON((byte) (1 << 1)), FRAGMENT((byte) (1 << 2)), COMPRESSED((byte) (1 << 3)), DBOP((byte) (1 << 4)), PING((byte) (1 << 5)), RESERVED_1((byte) (1 << 6)), SETUP((byte) (1 << 7));

	private final byte flag;

	Flag(byte flag) {
		this.flag = flag;
	}

	public static boolean isFlagSet(byte flags, Flag flag) {
		return (flags & flag.flag) != 0;
	}

	public static byte flagsToByte(Flag... flags) {
		byte code = 0;
		for (Flag flag : flags) {
			code |= flag.flag;
		}
		return code;
	}

}

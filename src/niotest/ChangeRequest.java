package niotest;

import java.nio.channels.SocketChannel;

public class ChangeRequest {
	public enum ChangeRequestType {
		REGISTER, CHANGEOPS;
	}

	public final SocketChannel socket;
	public final ChangeRequestType changeRequestType;
	public final int ops;

	public ChangeRequest(final SocketChannel socket, final ChangeRequestType changeRequestType, final int ops) {
		this.socket = socket;
		this.changeRequestType = changeRequestType;
		this.ops = ops;
	}
}

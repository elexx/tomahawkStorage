package tomahawk.network;

import java.nio.channels.SocketChannel;

public interface NewControlConnectionHandler {

	public void newControlConnection(SocketChannel socketChannel);

}

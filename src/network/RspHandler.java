package network;

public class RspHandler {
	private byte[] rsp = null;

	public synchronized boolean handleResponse(byte[] rsp) {
		this.rsp = rsp;
		notify();
		return true;
	}

	public synchronized void waitForResponse() {
		while (null == rsp) {
			try {
				wait();
			} catch (InterruptedException e) {}
		}

		System.out.println(new String(rsp));
	}
}

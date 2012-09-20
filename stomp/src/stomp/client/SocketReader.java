package stomp.client;

import java.io.InputStream;
import java.util.Queue;

/**
 * @author yaroslav.gaponov
 *
 */
class SocketReader implements Runnable {
	private InputStream is;
	private Queue<StompFrame> queue;

	/** constructor
	 * @param is input stream
	 * @param queue pool
	 */
	public SocketReader(InputStream is, Queue<StompFrame> queue) {
		this.is = is;
		this.queue = queue;
	}

	public void run() {
		int nextByte;
		StringBuilder sb = new StringBuilder();
		while (true) {
			do {
				try {
					nextByte = this.is.read();
				} catch (Exception ex) {
					return;
				}
			} while (sb.toString().isEmpty() && !Character.isLetter(nextByte));
			sb.append((char) nextByte);
			if (nextByte == '\0') {
				StompFrame frame = StompFrame.parse((sb.toString()));
				synchronized (this.queue) {
					this.queue.add(frame);
					this.queue.notify();
				}
				sb.setLength(0);
			}
		}
	}
}

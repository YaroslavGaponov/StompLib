package stomp.client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author yaroslav.gaponov
 *
 */
public class StompClient {
	private ClientMode mode;
	private String address;
	private int port;
	private Socket socket;
	private String sessionId;
	private Thread reader;
	private Queue<StompFrame> queue = new LinkedBlockingQueue<StompFrame>();
	private Map<String,String> messages = new HashMap<String,String>();

	
	/** constructor
	 * @param address STOMP server host
	 * @param port STOMP server port
	 */
	public StompClient(ClientMode mode, String address, int port) {
		this.mode = mode;
		this.address = address;
		this.port = port;
	}
	
	
	/** connect to STOMP server
	 * @return sessionId 
	 * @throws StompException
	 */
	public String connect() throws StompException {
		try {
			InetAddress ipAddress = InetAddress.getByName(address);
			this.socket = new Socket(ipAddress, this.port);

			if (this.mode == ClientMode.AVID) {
				this.reader = new Thread(new SocketReader(this.socket.getInputStream(), queue));
				this.reader.start();
			}

			this.sendFrame(new StompFrame(StompCommand.CONNECT));
			StompFrame frame = null;
			do {
				frame = this.receiveFrame();
			} while (!frame.command.equals(StompCommand.CONNECTED));

			this.sessionId = frame.header.get("session");

		} catch (Exception e) {
			StompException ex = new StompException("some problem with connection");
			ex.initCause(e);
			throw ex;
		}

		return this.sessionId;
	}

	/** begin transaction
	 * @param transaction transaction id
	 * @throws StompException
	 */
	public void begin(String transaction) throws StompException {
		StompFrame frame = new StompFrame(StompCommand.BEGIN);
		frame.header.put("transaction", transaction);
		this.sendFrame(frame);		
	}

	/** commit transaction
	 * @param transaction transaction id
	 * @throws StompException
	 */
	public void commit(String transaction) throws StompException {
		StompFrame frame = new StompFrame(StompCommand.COMMIT);
		frame.header.put("transaction", transaction);
		this.sendFrame(frame);		
	}

	/** rollback transaction
	 * @param transaction transaction id
	 * @throws StompException
	 */
	public void abort(String transaction) throws StompException {
		StompFrame frame = new StompFrame(StompCommand.ABORT);
		frame.header.put("transaction", transaction);
		this.sendFrame(frame);		
	}
	
	
	/** send message to STOMP server
	 * @param destination queue or topic 
	 * @param message some message
	 * @throws StompException
	 */
	public void send(String destination, String message) throws StompException {
		StompFrame frame = new StompFrame(StompCommand.SEND);
		frame.header.put("destination", destination);
		frame.header.put("session", this.sessionId);
		frame.body = message;
		this.sendFrame(frame);
	}

	/** receive message body by messageId
	 * @param message id
	 * @return
	 */
	public String getMessage(String messageId) {
		return this.messages.get(messageId);
	}
	
	/** receive messageId 
	 * @return messageId
	 * @throws StompException
	 */
	public String receive() throws StompException {
		StompFrame frame;
		while ((frame = this.receiveFrame()).command != StompCommand.MESSAGE);
		String messageId = frame.header.get("message-id");
		this.messages.put(messageId, frame.body);
		return messageId;
	}

		
	/** subscribe on some queue/topic
	 * @param destination queue/topic name
	 * @throws StompException
	 */
	public void subscribe(String destination) throws StompException {
		this.subscribe(destination, Ack.auto);
	}

	/** subscribe on some queue/topic
	 * @param destination queue/topic name
	 * @param ack auto/client type
	 * @throws StompException
	 */
	public void subscribe(String destination, Ack ack) throws StompException {
		StompFrame frame = new StompFrame(StompCommand.SUBSCRIBE);
		frame.header.put("destination", destination);
		frame.header.put("session", this.sessionId);
		frame.header.put("ack", ack.toString());		
		this.sendFrame(frame);
	}

	/** unsubscribe from some queue/topic
	 * @param destination queue/topic name
	 * @throws StompException
	 */
	public void unsubscribe(String destination) throws StompException {
		StompFrame frame = new StompFrame(StompCommand.UNSUBSCRIBE);
		frame.header.put("destination", destination);
		frame.header.put("session", this.sessionId);
		this.sendFrame(frame);
	}

	/** correct disconnect from STOM server
	 * 
	 */
	public void disconnect() {
		if (this.socket.isConnected()) {
			try {
				StompFrame frame = new StompFrame(StompCommand.DISCONNECTED);
				frame.header.put("session", this.sessionId);
				this.sendFrame(frame);

				this.socket.close();
			} catch (Exception e) {
			}
		}
	}
	
	/** ack
	 * @param messageId
	 * @throws StompException
	 */
	public void ack(String messageId) throws StompException {
		StompFrame frame = new StompFrame(StompCommand.ACK);
		frame.header.put("message-id", messageId);		
		this.sendFrame(frame);	
		
		if (this.mode.equals(ClientMode.SIMPLE)) {
			frame  = receiveFrame();
			assert(frame.command.equals(StompCommand.RECEIPT));
			assert(frame.header.get("receipt-id").equals(messageId));
		}		
	}
	
	
	/** ack in transaction
	 * @param messageId
	 * @param transaction
	 * @throws StompException
	 */
	public void ack(String messageId, String transaction) throws StompException {
		StompFrame frame = new StompFrame(StompCommand.ACK);
		frame.header.put("message-id", messageId);	
		frame.header.put("transaction", transaction);
		this.sendFrame(frame);	
		
		if (this.mode.equals(ClientMode.SIMPLE)) {
			frame  = receiveFrame();
			assert(frame.command.equals(StompCommand.RECEIPT));
			assert(frame.header.get("receipt-id").equals(messageId));
		}
	}	

	/** send frame to STOMP server 
	 * @param frame frame object
	 * @throws StompException
	 */
	private void sendFrame(StompFrame frame) throws StompException {
		if (!this.socket.isConnected() || this.socket.isOutputShutdown()) {
			throw new StompException("Socket is not open.");
		}
		try {
			this.socket.getOutputStream().write(frame.getBytes());
		} catch (IOException e) {
			StompException ex = new StompException("Problem with sending frame");
			ex.initCause(e);
			throw ex;
		}
	}

	/** receive frame from STOMP server
	 * @return frame object
	 * @throws StompException
	 */
	private synchronized StompFrame receiveFrame() throws StompException {
		if (this.mode == ClientMode.AVID) {
			synchronized (this.queue) {
				while (this.queue.isEmpty()) {
					if (!this.socket.isConnected() || this.socket.isInputShutdown()) {
						throw new StompException("Socket is not open.");
					}
					try {
						this.queue.wait();
					} catch (InterruptedException e) {
					}
	
				}
				return this.queue.poll();
			}
		} else {
			int nextByte;
			StringBuilder sb = new StringBuilder();
			while (true) {
				do {
					try {
						nextByte = this.socket.getInputStream().read();
					} catch (Exception e) {
						StompException ex = new StompException("Some problems with socket.");
						ex.initCause(e);
						throw ex;
					}
				} while (sb.toString().isEmpty() && !Character.isLetter(nextByte));
				sb.append((char) nextByte);
				if (nextByte == '\0') {
					return StompFrame.parse((sb.toString()));
				}
			}
		}
	}

}

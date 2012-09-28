package stomp.client;


import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Map;


public class StompClient {
	private String address;
	private int port;	
	private Socket socket;
	
	private String sessionId;	
	private Thread readerThread;
	
	private volatile boolean running = true;

	/**
	 * constructor
	 * @param address
	 * @param port
	 */
	public StompClient(String address, int port) {
		this.address = address;
		this.port = port;
	}

	// customs handlers
	public void onConnected(String sessionId) {}
	public void onDisconnected() {}
	public void onMessage(String  messageId, String body){}
	public void onReceipt(String receiptId) {}
	public void onError(String message, String description) {}	

	/**
	 * connect() - initialize work with STOMP server
	 * @throws StompException
	 */
	public void connect() throws StompException {
		try {
			// connecting to STOMP server
			InetAddress ipAddress = InetAddress.getByName(address);
			socket = new Socket(ipAddress, this.port);

			// initialize reader thread
			readerThread = new Thread( new Runnable() {
				public void run() {
					reader();
				}				
			});
			
			// run reader thread
			readerThread.start();

			// sending CONNECT command
			send(new StompFrame(StompCommand.CONNECT));
			
			// wait CONNECTED server command
			synchronized(this) {
				wait(5000);
			}
			

		} catch (Exception e) {
			StompException ex = new StompException("some problem with connection");
			ex.initCause(e);
			throw ex;
		}
	}
	
	/**
	 * disconnect() - finalize work with STOMP server
	 */
	public void disconnect() {
		if (socket.isConnected()) {
			try {
				// sending DISCONNECT command
				StompFrame frame = new StompFrame(StompCommand.DISCONNECTED);
				frame.header.put("session", sessionId);
				send(frame);

				// stopping reader thread
				running = false;
				readerThread.interrupt();
				
				// close socket
				socket.close();
			} catch (Exception e) {
			}
		}
	}
	
	/**
	 * reader() - thread for read and parse data from STOMP server
	 */
	private void reader() {		
		try {
			InputStream in = this.socket.getInputStream();
			StringBuilder sb = new StringBuilder();
			while (running) {
				try {					
					sb.setLength(0);					
					int ch;				
					
					// skip lead trash
					do { ch = in.read(); } while (ch < 'A' || ch > 'Z');
					
					// read frame 
					do { sb.append((char)ch); } while ((ch = in.read()) != 0);
					
					// parsing raw data to StompFrame format
					StompFrame frame = StompFrame.parse(sb.toString());				
					
					// run handlers
					switch (frame.command) {
						case CONNECTED:
							// unblock connect()
							synchronized(this) { 
								notify(); 
							}
							sessionId = frame.header.get("session");
							onConnected(sessionId);
							break;
						case DISCONNECTED:
							onDisconnected();
							break;
						case RECEIPT:
							String receiptId = frame.header.get("receipt-id");
							onReceipt(receiptId);
							break;
						case MESSAGE:
							String messageId = frame.header.get("message-id");
							onMessage(messageId, frame.body);
							break;						
						case ERROR:
							String message = frame.header.get("message");
							onError(message, frame.body);
							break;
						default:																				
							break;
					}
					
				} catch (IOException e) {
				} 
			}
		} catch (IOException e) {
		}						
	}

	/**
	 * BEGIN is used to start a transaction. 
	 * @param transaction
	 * @throws StompException
	 */
	public void begin(String transaction) throws StompException {
		StompFrame frame = new StompFrame(StompCommand.BEGIN);
		frame.header.put("transaction", transaction);
		send(frame);		
	}

	/**
	 * COMMIT is used to commit a transaction in progress.
	 * @param transaction
	 * @throws StompException
	 */
	public void commit(String transaction) throws StompException {
		StompFrame frame = new StompFrame(StompCommand.COMMIT);
		frame.header.put("transaction", transaction);
		send(frame);		
	}

	/**
	 * ABORT is used to roll back a transaction in progress.
	 * @param transaction
	 * @throws StompException
	 */
	public void abort(String transaction) throws StompException {
		StompFrame frame = new StompFrame(StompCommand.ABORT);
		frame.header.put("transaction", transaction);
		send(frame);		
	}
	
	/**
	 * The SEND command sends a message to a destination in the messaging system.
	 * @param destination
	 * @param message
	 * @throws StompException
	 */
	public void send(String destination, String message) throws StompException {
		StompFrame frame = new StompFrame(StompCommand.SEND);
		frame.header.put("destination", destination);
		frame.header.put("session", sessionId);
		frame.body = message;
		send(frame);
	}

	/**
	 * The SEND command sends a message to a destination in the messaging system.
	 * @param destination
	 * @param header
	 * @param message
	 * @throws StompException
	 */
	public void send(String destination, Map<String, String> header, String message) throws StompException {
		StompFrame frame = new StompFrame(StompCommand.SEND);
		frame.header.put("destination", destination);
		frame.header.put("session", sessionId);
		for(String key: header.keySet()) {
			frame.header.put(key, header.get(key));
		}
		frame.body = message;
		send(frame);
	}
	
	/**
	 * The SUBSCRIBE command is used to register to listen to a given destination.
	 * @param destination
	 * @throws StompException
	 */
	public void subscribe(String destination) throws StompException {
		subscribe(destination, Ack.auto);
	}

	/**
	 * The SUBSCRIBE command is used to register to listen to a given destination.
	 * @param destination
	 * @param ack
	 * @throws StompException
	 */
	public void subscribe(String destination, Ack ack) throws StompException {
		StompFrame frame = new StompFrame(StompCommand.SUBSCRIBE);
		frame.header.put("destination", destination);
		frame.header.put("session", sessionId);
		frame.header.put("ack", ack.toString());		
		send(frame);
	}

	/**
	 * The UNSUBSCRIBE command is used to remove an existing subscription
	 * @param destination
	 * @throws StompException
	 */
	public void unsubscribe(String destination) throws StompException {
		StompFrame frame = new StompFrame(StompCommand.UNSUBSCRIBE);
		frame.header.put("destination", destination);
		frame.header.put("session", sessionId);
		send(frame);
	}
	
	/**
	 * ACK is used to acknowledge consumption of a message from a subscription using client acknowledgment.
	 * @param messageId
	 * @throws StompException
	 */
	public void ack(String messageId) throws StompException {
		StompFrame frame = new StompFrame(StompCommand.ACK);
		frame.header.put("message-id", messageId);		
		send(frame);			
	}
	
	/**
	 * ACK is used to acknowledge consumption of a message from a subscription using client acknowledgment.
	 * @param messageId
	 * @param transaction
	 * @throws StompException
	 */
	public void ack(String messageId, String transaction) throws StompException {
		StompFrame frame = new StompFrame(StompCommand.ACK);
		frame.header.put("message-id", messageId);	
		frame.header.put("transaction", transaction);
		send(frame);			
	}	
	
	/**
	 * send - help function for sending any frame to STOMP server
	 * @param frame
	 * @throws StompException
	 */
	private void send(StompFrame frame) throws StompException {
		try {
			socket.getOutputStream().write(frame.getBytes());
		} catch (IOException e) {
			StompException ex = new StompException("Problem with sending frame");
			ex.initCause(e);
			throw ex;
		}
	}

}

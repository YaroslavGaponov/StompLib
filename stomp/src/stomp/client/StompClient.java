package stomp.client;


import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Map;


/**
 * @author yaroslav.gaponov
 *
 */
public class StompClient {
	private String address;
	private int port;	
	private Socket socket;	
	private String sessionId;	
	private Thread readerThread;
	
	private volatile boolean running = true;
	
	
	public StompClient(String address, int port) {
		this.address = address;
		this.port = port;
	}
	
	
	public void connect() throws StompException {
		try {
			InetAddress ipAddress = InetAddress.getByName(address);
			this.socket = new Socket(ipAddress, this.port);

			this.readerThread = new Thread( new Runnable() {
				public void run() {
					reader();
				}				
			});
			
			this.readerThread.start();

			this.sendframe(new StompFrame(StompCommand.CONNECT));

		} catch (Exception e) {
			StompException ex = new StompException("some problem with connection");
			ex.initCause(e);
			throw ex;
		}
	}

	
	private void reader() {		
		try {
			InputStream in = this.socket.getInputStream();
			StringBuilder sb = new  StringBuilder();
			while (this.running) {
				try {					
					sb.setLength(0);					
					int ch;				
					
					// skip lead trash
					do { ch = in.read(); } while (ch < 'A' || ch > 'Z');
					
					// read frame 
					do { sb.append((char)ch); } while ((ch = in.read()) != 0);
					
					StompFrame frame = StompFrame.parse(sb.toString());				
					
					switch (frame.command) {
						case CONNECTED:
							String sessionId = frame.header.get("session");
							onconnected(sessionId);
							break;
						case DISCONNECTED:
							ondisconnected();
							break;
						case RECEIPT:
							String receiptId = frame.header.get("receipt-id");
							onreceipt(receiptId);
							break;
						case MESSAGE:
							String messageId = frame.header.get("message-id");
							onmessage(messageId, frame.body);
							break;						
						case ERROR:
							String message = frame.header.get("message");
							onerror(message, frame.body);
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
	
	public void begin(String transaction) throws StompException {
		StompFrame frame = new StompFrame(StompCommand.BEGIN);
		frame.header.put("transaction", transaction);
		this.sendframe(frame);		
	}

	public void commit(String transaction) throws StompException {
		StompFrame frame = new StompFrame(StompCommand.COMMIT);
		frame.header.put("transaction", transaction);
		this.sendframe(frame);		
	}

	public void abort(String transaction) throws StompException {
		StompFrame frame = new StompFrame(StompCommand.ABORT);
		frame.header.put("transaction", transaction);
		this.sendframe(frame);		
	}
	
	
	public void send(String destination, String message) throws StompException {
		StompFrame frame = new StompFrame(StompCommand.SEND);
		frame.header.put("destination", destination);
		frame.header.put("session", this.sessionId);
		frame.body = message;
		this.sendframe(frame);
	}

	public void send(String destination, String message, Map<String, String> header) throws StompException {
		StompFrame frame = new StompFrame(StompCommand.SEND);
		frame.header.put("destination", destination);
		frame.header.put("session", this.sessionId);
		for(String key: header.keySet()) {
			frame.header.put(key, header.get(key));
		}
		frame.body = message;
		this.sendframe(frame);
	}
		
		
	public void subscribe(String destination) throws StompException {
		this.subscribe(destination, Ack.auto);
	}

	public void subscribe(String destination, Ack ack) throws StompException {
		StompFrame frame = new StompFrame(StompCommand.SUBSCRIBE);
		frame.header.put("destination", destination);
		frame.header.put("session", this.sessionId);
		frame.header.put("ack", ack.toString());		
		this.sendframe(frame);
	}

	public void unsubscribe(String destination) throws StompException {
		StompFrame frame = new StompFrame(StompCommand.UNSUBSCRIBE);
		frame.header.put("destination", destination);
		frame.header.put("session", this.sessionId);
		this.sendframe(frame);
	}

	public void disconnect() {
		if (this.socket.isConnected()) {
			try {
				StompFrame frame = new StompFrame(StompCommand.DISCONNECTED);
				frame.header.put("session", this.sessionId);
				this.sendframe(frame);

				this.running = false;				
				
				this.socket.close();
			} catch (Exception e) {
			}
		}
	}
	
	public void ack(String messageId) throws StompException {
		StompFrame frame = new StompFrame(StompCommand.ACK);
		frame.header.put("message-id", messageId);		
		this.sendframe(frame);			
	}
	
	
	public void ack(String messageId, String transaction) throws StompException {
		StompFrame frame = new StompFrame(StompCommand.ACK);
		frame.header.put("message-id", messageId);	
		frame.header.put("transaction", transaction);
		this.sendframe(frame);			
	}	

	
	private synchronized void onconnected(String sessionId) {
		this.sessionId = sessionId;		
	}

	private synchronized void ondisconnected() {		
	}
		
	public synchronized void onmessage(String  message_id, String body) {
	}

	public synchronized void onreceipt(String receipt_id) {
	}
		
	public synchronized void onerror(String message, String description) {
	}
		
	
	private void sendframe(StompFrame frame) throws StompException {
		try {
			this.socket.getOutputStream().write(frame.getBytes());
		} catch (IOException e) {
			StompException ex = new StompException("Problem with sending frame");
			ex.initCause(e);
			throw ex;
		}
	}

}

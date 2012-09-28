Simple STOMP client library in Java.
========


## Example

<p><code>



package stomp.client.test.async;

import java.util.Scanner;

import stomp.client.Ack;
import stomp.client.StompClient;
import stomp.client.StompException;

public class test {

	public static void main(String[] args) throws StompException {
		new test().run("localhost", 61613);
	}
	
	public void run(String host, int port) throws StompException {
		
		
		StompClient client = new StompClient(host, port) {
			public void onConnected(String sessionId) {
				System.out.println("connected: sessionId = " + sessionId);
			}
			
			public void onDisconnected() {
				System.out.println("disconnected");
			}
			
			public void onMessage(String messageId, String body) {
				System.out.println("message: messageId = " + messageId + " body = " + body);
				try {
					ack(messageId);
				} catch (StompException e) {
				}
			}
			
			public void onReceipt(String receiptId) {
				System.out.println("receipt: receiptId = " + receiptId);
			}
			
			public void onError(String message, String description) {
				System.out.println("error: message = " + message + " description = " + description);
			}
		};
		
		// connect to STOMP server, send CONNECT command and wait CONNECTED answer
		client.connect();
		
		// subscribe on queue
		client.subscribe("/queue/test", Ack.client);
		
		
		// send 10 messages
		for(int i=0; i<10; i++) {
			client.send("/queue/test", "message #" + i);
		}
		
		// wait		
		Scanner sc = new Scanner(System.in);
		sc.nextLine();
		
		// unsubscribe
		client.unsubscribe("/queue/test");
		
		// disconnect
		client.disconnect();
		
	}

}


</code></p>

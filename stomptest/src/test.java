import stomp.client.Ack;
import stomp.client.StompClient;
import stomp.client.StompException;

public class test {
	public static void main(String[] args) {

		String host = "acs-dev1";
		int port = 61613;
		String queue = "/queue/test_stomp";

		StompClient client = new StompClient(host, port) {
			public void onmessage(String  message_id, String body) {
				System.out.println("onmessage : " + message_id + " : " + body);
				
				try {
					ack(message_id);
				} catch (StompException e) {}
			}
			public void onreceipt(String receipt_id) {
				System.out.println("onreceipt : " + receipt_id);
			}
				
			public void onerror(String message, String description) {
				System.out.println("onerror : " + message + " (" + description + ")");
			}			
		};
		
		
		try {
			System.out.println(String.format("connecting to STOMP server %s:%s ...", host, port));
			client.connect();

			System.out.println(String.format("subscribing on %s ...", queue));
			client.subscribe(queue, Ack.client);

			System.out.println(String.format("sending messages to %s ...", queue));
			for (int i = 0; i < 10; i++) {
				client.send(queue, "hello #" + i);
			}
													
			try {
				Thread.currentThread().sleep(500000);			
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			
			System.out.println("unsubscribing ...");
			client.unsubscribe(queue);
			
		} catch (StompException e) {
			e.printStackTrace();
		} finally {
			System.out.println("disconnecting ...");
			client.disconnect();
		}

	}
}

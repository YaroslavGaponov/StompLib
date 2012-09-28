Simple STOMP client library in Java.
========


## Example



		StompClient client = new StompClient(host, port) {
			public synchronized void onmessage(String  message_id, String body) {
				System.out.println("onmessage : " + message_id + " : " + body);				
				if (ack == Ack.client) {
					try { ack(message_id); } catch (StompException e) {}
				}
			}
			public synchronized void onreceipt(String receipt_id) {
				System.out.println("onreceipt : " + receipt_id);
			}
				
			public synchronized void onerror(String message, String description) {
				System.out.println("onerror : " + message + " (" + description + ")");
			}			
		};
		
		
		try {
			System.out.println(String.format("connecting to STOMP server %s:%s ...", host, port));
			client.connect();

			System.out.println(String.format("subscribing on %s ...", queue));
			client.subscribe(queue, ack);

			System.out.println(String.format("sending messages to %s ...", queue));
			for (int i = 0; i < 10; i++) {
				client.send(queue, "hello #" + i);
			}
													
			try {
				Thread.sleep(500000);			
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


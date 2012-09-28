Simple STOMP client library in Java.
========


## Example


        String host = "localhost";
        int port = 61613;
        String queue = "/queue/test_stomp";
        
        StompClient client = new StompClient(ClientMode.SIMPLE, host, port);
        try {
                System.out.println(String.format("connecting to STOMP server %s:%s ...", host, port));
                System.out.println("sessionId is " + client.connect());
        
                System.out.println(String.format("subscribing on %s ...", queue));
                client.subscribe(queue, Ack.client);
        
                System.out.println(String.format("sending messages to %s ...", queue));
                for (int i = 0; i < 100; i++) {
                        client.send(queue, "hello #" + i);
                }
                
                List<String> processed = new ArrayList<String>();
                System.out.println(String.format("receive messages from %s ...", queue));
                for (int i = 0; i < 100; i++) {
                        String messageId = client.receive();
                        System.out.println(messageId + " : " + client.getMessage(messageId));
                        processed.add(messageId);
                }
                
                System.out.println("sent ack command to server ...");
                for(String id : processed) {
                        System.out.println("Ack for " + id);
                        client.ack(id);
                }
        
                System.out.println("unsubscribing ...");
                client.unsubscribe(queue);
                
        } catch (StompException e) {
                e.printStackTrace();
        } finally {
                System.out.println("disconnecting ...");
                client.disconnect();
        }


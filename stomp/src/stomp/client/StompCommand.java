package stomp.client;

enum StompCommand {
	// client-commands
	CONNECT, SEND, SUBSCRIBE, UNSUBSCRIBE, BEGIN, COMMIT, ABORT, ACK, DISCONNECT,

	// server-commands
	CONNECTED, MESSAGE, RECEIPT, ERROR, DISCONNECTED
}

package stomp.client;

public class StompException extends Exception {
	private static final long serialVersionUID = 5475019401678519895L;
	private String message;

	public StompException() {
		super();
		this.message = "unknown";
	}

	public StompException(String message) {
		super(message);
		this.message = message;
	}

	public String getMessage() {
		return message;
	}

}

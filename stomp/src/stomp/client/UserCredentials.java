package stomp.client;

public class UserCredentials implements Credentials {
	private String user;
	private String password;
	
	public UserCredentials(String user, String password) {
		this.user = user;
		this.password = password;
	}

	public String getLogin() {
		return user;
	}

	public String getPasscode() {
		return password;
	}

}

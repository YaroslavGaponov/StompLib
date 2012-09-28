package stomp.client;

import java.util.HashMap;
import java.util.Map;

/**
 * @author yaroslav.gaponov
 *
 */
class StompFrame {
	public StompCommand command;
	public Map<String, String> header = new HashMap<String, String>();
	public String body;

	/** constructor
	 *  
	 */
	public StompFrame() {
	}

	/** constructor 
	 * @param command type of frame 
	 */
	public StompFrame(StompCommand command) {
		this.command = command;
	}

	public String toString() {
		return String.format("command: %s, header: %s, body: %s", this.command,
				this.header.toString(), this.body);
	}

	/** getBytes convert frame object to array of bytes
	 * @return array of bytes
	 */
	public byte[] getBytes() {
		String frame = this.command.toString() + '\n';
		for (String key : this.header.keySet()) {
			frame += key + ":" + this.header.get(key) + '\n';
		}
		frame += '\n';

		if (this.body != null) {
			frame += this.body;
		}
		frame += "\0";
		return frame.getBytes();
	}

	/** parse string to frame object
	 * @param raw frame as string
	 * @return frame object
	 */
	public static StompFrame parse(String raw) {
		StompFrame frame = new StompFrame();

		String commandheaderSections = raw.split("\n\n")[0];
		String[] headerLines = commandheaderSections.split("\n");

		frame.command = StompCommand.valueOf(headerLines[0]);

		for (int i = 1; i < headerLines.length; i++) {
			String key = headerLines[i].split(":")[0];
			frame.header.put(key, headerLines[i].substring(key.length() + 1));
		}

		frame.body = raw.substring(commandheaderSections.length() + 2);

		return frame;
	}

}

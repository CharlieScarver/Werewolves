import java.io.*;
/*
 * This class defines the different type of messages that will be exchanged between the
 * Clients and the Server. 
 * When talking from a Java Client to a Java Server a lot easier to pass Java objects, no 
 * need to count bytes or to wait for a line feed at the end of the frame
 */
public class Message implements Serializable {

	protected static final long serialVersionUID = 1112122200L;

	// The different types of message sent by the Client
	// WHOISIN to receive the list of the users connected
	// MESSAGE an ordinary message
	// LOGOUT to disconnect from the Server
	static final int WHOISIN = 0, MESSAGE = 1, LOGOUT = 2, HELP = 3;
	static final int PLAY = 4, CLEAR = 5;
	// ingame commands
	static final int ROLE = 11, PROTECT = 12, KILL = 13;
	static final int STARTVOTE = 14, VOTE = 15, VOTEYES = 16, VOTENO = 17;
	// admin commands
	static final int NR = 20, GS = 21;
	private int type;
	private String message;
	
	// constructor
	Message(int type, String message) {
		this.type = type;
		this.message = message;
	}
	
	// getters
	int getType() {
		return type;
	}
	String getMessage() {
		return message;
	}
}


import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
/*
 * The server that can be run both as a console application or a GUI
 */
public class Server {
	
	private static int Werewolves = 2;
	private static final int Medics = 1;

	private static boolean hasMedicProtectedHimself = false;
	
	private static boolean areRolesAssigned = false;
	private static boolean userFound = false;
	
	private static boolean voteInitiated = false;
	private static boolean skipVoteInitiated = false;
	private static String currentVoteUser = "";
	private static int votes = 0;
	private static int votesCounter = 0;
	private static int maxLynchPoints = 0;
	private static String maxLynchPointsUser = "";
	private static boolean voteIsADraw = false;
	private static ArrayList<String> tiebreakers = new ArrayList();
	private static boolean wasThereATieAlready = false;
	
	public static String voteScores = "";
	public static String voteLog = "";
	private static String log = "";
	
	private static int gameState = 1;
	
	private static boolean medicIsDead = false;
	private static boolean noMoreLivingWerewolves = false;
	private static boolean weGotABody = false;
	
	private static String commandsHelp = "Command list:\n"
			+ " logout - close your connection\n"
			+ " whoisin - list of the currently connected people with their game status\n"
			+ " play - assigns roles once at the beginning of the game\n"
			+ " role - tells you your game role\n"
			+ " protect [name] - protects someone through the night; used by [Medic]\n"
			+ " kill [name] - kills someone; used by [Werewolf]\n"
			+ " vote [name] - starts a vote for a player. \n"
			+ "   After the vote has started every player except the one voted against \n"
			+ "   must use one of the next two commands.\n"
			+ " voteyes - votes with [YES] in the current vote\n"
			+ " voteno - votes with [NO] in the current vote\n"
			+ " vote pass - skips the day for the village";
	
	private static String clearCommand = new String(new char[50]).replace("\0", "\n");
	
	// a unique ID for each connection
	private static int uniqueId;
	
	// an ArrayList to keep the list of the Client
	private ArrayList<ClientThread> al;
	// if I am in a GUI
	private ServerGUI sg;
	// to display time
	private SimpleDateFormat sdf;
	// the port number to listen for connection
	private int port;
	// the boolean that will be turned of to stop the server
	private boolean keepGoing;
	

	/*
	 *  server constructor that receive the port to listen to for connection as parameter
	 *  in console
	 */
	public Server(int port) {
		this(port, null);
	}
	
	public Server(int port, ServerGUI sg) {
		// GUI or not
		this.sg = sg;
		// the port
		this.port = port;
		// to display hh:mm:ss
		sdf = new SimpleDateFormat("HH:mm:ss");
		// ArrayList for the Client list
		al = new ArrayList<ClientThread>();
	}
	
	public void start() {

		keepGoing = true;
		/* create socket server and wait for connection requests */
		try 
		{
			// the socket used by the server
			ServerSocket serverSocket = new ServerSocket(port);

			// infinite loop to wait for connections
			while(keepGoing) 
			{
				// format message saying we are waiting
				display("Server waiting for Clients on port " + port + ".");
				
				Socket socket = serverSocket.accept();  	// accept connection
				// if I was asked to stop
				if(!keepGoing)
					break;
				ClientThread t = new ClientThread(socket);  // make a thread of it
				al.add(t);									// save it in the ArrayList
				t.start();
			}
			// I was asked to stop
			try {
				serverSocket.close();
				for(int i = 0; i < al.size(); ++i) {
					ClientThread tc = al.get(i);
					try {
					tc.sInput.close();
					tc.sOutput.close();
					tc.socket.close();
					}
					catch(IOException ioE) {
						// not much I can do
					}
				}
			}
			catch(Exception e) {
				display("Exception closing the server and clients: " + e);
			}
		}
		// something went bad
		catch (IOException e) {
            String msg = sdf.format(new Date()) + " Exception on new ServerSocket: " + e + "\n";
			display(msg);
		}
	}		
	/*
     * For the GUI to stop the server
     */
	protected void stop() {
		keepGoing = false;
		// connect to myself as Client to exit statement 
		// Socket socket = serverSocket.accept();
		try {
			new Socket("localhost", port);
		}
		catch(Exception e) {
			// nothing I can really do
		}
	}
	/*
	 * Display an event (not a message) to the console or the GUI
	 */
	private void display(String msg) {
		String time = sdf.format(new Date()) + " " + msg;
		if(sg == null)
			System.out.println(time);
		else
			sg.appendEvent(time + "\n");
	}
	/*
	 *  to broadcast a message to all Clients
	 */
	private synchronized void broadcast(String message) {
		// add HH:mm:ss and \n to the message
		//String time = sdf.format(new Date());
		//String messageLf = time + " " + message + "\n";
		String messageLf = message + "\n";
		// display message on console or GUI
		if(sg == null)
			System.out.print(messageLf);
		else
			sg.appendRoom(messageLf);     // append in the room window
		
		// we loop in reverse order in case we would have to remove a Client
		// because it has disconnected
		for(int i = al.size(); --i >= 0;) {
			ClientThread ct = al.get(i);
			// try to write to the Client if it fails remove it from the list
			if(!ct.writeMsg(messageLf)) {
				al.remove(i);
				display("Disconnected Client " + ct.username + " removed from list.");
			}
		}
	}

	// for a client who log off using the LOGOUT message
	synchronized void remove(int id) {
		// scan the array list until we found the Id
		for(int i = 0; i < al.size(); ++i) {
			ClientThread ct = al.get(i);
			// found it
			if(ct.id == id) {
				al.remove(i);
				return;
			}
		}
	}
	
	/*
	 *  To run as a console application just open a console window and: 
	 * > java Server
	 * > java Server portNumber
	 * If the port number is not specified 1500 is used
	 */ 
	public static void main(String[] args) {
		// start server on port 1500 unless a PortNumber is specified 
		int portNumber = 1500;
		switch(args.length) {
			case 1:
				try {
					portNumber = Integer.parseInt(args[0]);
				}
				catch(Exception e) {
					System.out.println("Invalid port number.");
					System.out.println("Usage is: > java Server [portNumber]");
					return;
				}
			case 0:
				break;
			default:
				System.out.println("Usage is: > java Server [portNumber]");
				return;
				
		}
		// create a server object and start it
		Server server = new Server(portNumber);
		server.start();
	}
	
	/** One instance of this thread will run for each client */
	class ClientThread extends Thread {
		// the socket where to listen/talk
		Socket socket;
		ObjectInputStream sInput;
		ObjectOutputStream sOutput;
		// my unique id (easier for disconnection)
		int id;
		// the user name of the Client
		String username;
		// the only type of message a will receive
		Message cm;
		// the date I connect
		String date;
		
		// client role ingame
		// 0 - villager
		// 1 - medic
		// 2 - werewolf
		// 3 - seer
		private int role = 0;
		private String roleName = "";
		
		private boolean isProtected = false;
		private boolean isAlive = true;
		
		private boolean hasVoted = false;
		private boolean canBeVotedAgainst = false;
		private int lynchScore = 0;

		public int getRole() {
			return role;
		}
		
		// Constructor
		ClientThread(Socket socket) {
			// a unique id
			id = ++uniqueId;
			this.socket = socket;
			/* Creating both Data Stream */
			System.out.println("Thread trying to create Object Input/Output Streams");
			try
			{
				// create output first
				sOutput = new ObjectOutputStream(socket.getOutputStream());
				sInput  = new ObjectInputStream(socket.getInputStream());
				// read the username
				String temp = (String) (sInput.readObject());
				username = temp.equals("Anon") || temp.equalsIgnoreCase("PASS") ? temp + id : temp;
				display(username + " just connected.");
			}
			catch (IOException e) {
				display("Exception creating new Input/output Streams: " + e);
				return;
			}
			// have to catch ClassNotFoundException
			// but I read a String, I am sure it will work
			catch (ClassNotFoundException e) {
			}
            date = new Date().toString() + "\n";
		}
		
		// returns the number of alive users
		private int getNumberOfAliveUsers() {
			
			int aliveUsers = 0;
			for(int i = 0; i < al.size(); ++i) {
				ClientThread ct = al.get(i);
				if(ct.isAlive) {
					aliveUsers++;
				}
			}
			return aliveUsers;
			
		} 
		
		// what will run forever
		public void run() {
			// to loop until LOGOUT
			boolean keepGoing = true;
			while(keepGoing) {
				// read a String (which is an object)
				try {
					cm = (Message) sInput.readObject();
				}
				catch (IOException e) {
					display(this.username + " Exception reading Streams: " + e);
					break;				
				}
				catch(ClassNotFoundException e2) {
					break;
				}
				// the message part of the ChatMessage
				String message = cm.getMessage();


				// Switch on the type of message receive
				switch(cm.getType()) {

				case Message.MESSAGE:
					if(this.isAlive) {
						broadcast(this.username + ": " + message);
					} else {
						writeMsg("Dead men tell no tales.\n");
					}
					break;
					
				case Message.LOGOUT:
					display(this.username + " disconnected with a LOGOUT message.\n");
					keepGoing = false;
					break;
					
				case Message.WHOISIN:
					writeMsg("List of the users connected at " + sdf.format(new Date()) + "\n");
					
					int aliveUsers = 0;
					// scan all the users connected
					for(int i = 0; i < al.size(); ++i) {
						ClientThread ct = al.get(i);
						if(ct.isAlive) {
							writeMsg((i+1) + ") " + ct.username + "  -  " + "[alive]");
							aliveUsers++;
						} else {
							writeMsg((i+1) + ") " + ct.username + "  -  " + "dead");
						}
					}
					writeMsg("Alive users: " + aliveUsers + "\n");
					break;
					
				case Message.HELP:
					writeMsg(Server.commandsHelp);
					break;
					
				case Message.CLEAR:
					writeMsg(Server.clearCommand);
					break;
					
				case Message.PLAY:
					if(al.size() < 3) {
						writeMsg("Not enough players\n");
						break;
					}
					
					if(!Server.areRolesAssigned) {
						broadcast("Let's play Werewolves!");						
						
						Server.Werewolves = (al.size() >= 8 ? 2 : 1);
						int werewolf1 = (int) Math.floor(Math.random() * al.size());
						int medic = (int) Math.floor(Math.random() * al.size());
						while(medic == werewolf1) {
							medic = (int) Math.floor(Math.random() * al.size());
						}
						if(Server.Werewolves == 2) {
							int werewolf2 = (int) Math.floor(Math.random() * al.size());
							while(werewolf1 == werewolf2 || medic == werewolf2) {
								werewolf2 = (int) Math.floor(Math.random() * al.size());
							}
						}
						
						// scan all the users connected
						for(int i = 0; i < al.size(); ++i) {
							ClientThread ct = al.get(i);
														
							if(i == werewolf1) {
								ct.role = 2;
								ct.roleName = "werewolf";
							} else if(i == medic) {
								ct.role = 1;
								ct.roleName = "medic";
							} else {
								ct.role = 0;
								ct.roleName = "villager";
							}												
	
							Server.log += " " + ct.username + " was assigned [" + ct.roleName + "]\n";
						}
						
						Server.log += "-----\n";
						Server.areRolesAssigned = true;
						broadcast("Roles have been assigned");
						
						broadcast("The village falls asleep");
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e1) {
							
						}
						Server.gameState = 1;
						broadcast("It's time for the [medic] to make his choice");
						
						
					} else {
						writeMsg("Roles already assigned\n");
					}
	//banana
					break;
					
				case Message.ROLE:
					if(Server.areRolesAssigned) {
						
						switch (this.role) {
							case 0:
								writeMsg("Villager\n");
								break;
							case 1:
								writeMsg("Medic\n");
								break;
							case 2:
								writeMsg("Werewolf\n");
								break;
						}
						
					} else {
						writeMsg("Roles have not been assigned yet\n");
					}
					break;
					
				case Message.PROTECT:
					if(!Server.areRolesAssigned) {
						writeMsg("Roles have not been assigned yet\n");
						break;
					}
					
					if(!this.isAlive) {
						writeMsg("You're dead. Deal with it.\n");
						break;
					}
					
					if(this.role != 1) {
						writeMsg("You don't have the power to protect the ones you love.\n");
						break;
					}
					
					if(Server.gameState != 1) {
						writeMsg("You can't protect right now\n");
						break;
					}
			
					for(int i = 0; i < al.size(); ++i) {
						ClientThread ct = al.get(i);
						
						if(ct.username.equalsIgnoreCase(message)) {
							if(Server.hasMedicProtectedHimself) {
								writeMsg("You have already protected yourself once, you egoist.\n");
								break;
							}	
							
							if(ct.username.equalsIgnoreCase(this.username)) {
								Server.hasMedicProtectedHimself = true;
							}
							
							Server.userFound = true;
							ct.isProtected = true;
							writeMsg(ct.username + " is now protected\n");
							broadcast("A player has been protected.");
							Server.log += " " + ct.username + "(" + ct.roleName + ") was protected\n";
							
							Server.gameState = 2;
							broadcast("It's time for the [werewolves] to make their choice");
						}
					}
					
					if(!Server.userFound) {
						writeMsg("No such user\n");
					}
					Server.userFound = false;
									
					break;
					
				case Message.KILL:
					if(!Server.areRolesAssigned) {
						writeMsg("Roles have not been assigned yet\n");
						break;
					}
					
					if(!this.isAlive) {
						writeMsg("You're dead. Deal with it.\n");
						break;
					}
					
					if(this.role != 2) {
						writeMsg("You don't have the power to kill the ones you hate.\n");
						break;
					}
					
					if(Server.gameState != 2) {
						writeMsg("You can't kill right now\n");
						break;
					}
									
					for(int i = 0; i < al.size(); ++i) {
						ClientThread ct = al.get(i);
						
						if(ct.username.equalsIgnoreCase(message)) {
							Server.userFound = true;
							if(ct.isAlive) {
								if(!ct.isProtected) {
									ct.isAlive = false;
									broadcast(ct.username + " is now dead");
									Server.log += " " + ct.username + "(" + ct.roleName + ") was [killed]\n";
									
								} else {
									broadcast("The target still [lives]");
									Server.log += " " + ct.username + "(" + ct.roleName + ") was targeted but was protected\n";
								}
								
								Server.gameState = 0;
								broadcast("It's time for the village to wake up");
							} else {
								writeMsg("The target is already dead\n");
							}
						}
					}
					
					if(!Server.userFound) {
						writeMsg("No such user\n");
					}
					Server.userFound = false;
										
					break;
					
				case Message.STARTVOTE:
					if(!Server.areRolesAssigned) {
						writeMsg("Roles have not been assigned yet\n");
						break;
					}
					
					if(!this.isAlive) {
						writeMsg("You're dead. Deal with it.\n");
						break;
					}
					
					if(Server.gameState != 0 || Server.voteInitiated) {
						writeMsg("You can't start a vote right now\n");
						break;
					}
							
					Server.voteInitiated = true;
					broadcast(this.username + " has started the voting");
					Server.voteLog += " " + this.username + " has started the voting\n";
					Server.log += " " + this.username + "(" + this.roleName + ") has started the voting\n";;

					break;
					
				case Message.VOTE:
					if(!Server.areRolesAssigned) {
						writeMsg("Roles have not been assigned yet\n");
						break;
					}
						
					if(!this.isAlive) {
						writeMsg("You're dead. Deal with it.\n");
						break;
					}

					if(!Server.voteInitiated) {
						writeMsg("You can't vote right now\n");
						break;
					}
					
					if(this.hasVoted) {
						writeMsg("You voted already\n");
						break;
					}
					
								
					if(message.equalsIgnoreCase("PASS")) {
						Server.userFound = true;
						
						Server.currentVoteUser = "PASS";
						Server.skipVoteInitiated = true;
					} else {
					
						for(int i = 0; i < al.size(); ++i) {
							ClientThread ct = al.get(i);
							
							if(ct.username.equalsIgnoreCase(message)) {
								Server.userFound = true;
								if(ct.isAlive) {

									//Server.currentVoteUser = ct.username;
									//Server.voteInitiated = true;
									ct.lynchScore++;
									Server.votesCounter++;
									this.hasVoted = true;
									
									broadcast(this.username + " has voted");
									Server.voteLog += "  " + this.username + " has voted for " + ct.username + "\n";
									Server.log += "  " + this.username + "(" + this.roleName + ") has voted for " + ct.username + "(" + ct.roleName + ")\n";

								} else {
									writeMsg("The target is already dead\n");
								}
							}
						}
					}
						
					if(!Server.userFound) {
						writeMsg("No such user\n");
					}
					Server.userFound = false;
											
					break;
					
				case Message.VOTEYES:
					if(Server.areRolesAssigned) {
						if(this.isAlive) {
							if(!this.hasVoted) {
								if(Server.gameState == 0 
									&& Server.voteInitiated 
									&& !this.username.equalsIgnoreCase(Server.currentVoteUser)) 
								{
									Server.votes++;
									Server.votesCounter++;
									this.hasVoted = true;
									
									broadcast(this.username + " has voted");
									if(!Server.currentVoteUser.equals("PASS")) {
										Server.voteLog += "  " + this.username + " voted YES\n";
										Server.log += "  " + this.username + "(" + this.roleName + ") voted YES\n";
									}
								} else {
									writeMsg("You can't vote right now\n");
								}
							} else {
								writeMsg("You voted already\n");
							}
						} else {
							writeMsg("You're dead. Deal with it.\n");
						}
					} else {
						writeMsg("Roles have not been assigned yet\n");
					}
					break;
					
				case Message.VOTENO:
					if(Server.areRolesAssigned) {
						if(this.isAlive) {
							if(!this.hasVoted) {
								
								if(Server.gameState == 0 
									&& Server.voteInitiated 
									&& !this.username.equalsIgnoreCase(Server.currentVoteUser)) 
								{
									Server.votes--;								
									Server.votesCounter++;
									this.hasVoted = true;
									
									broadcast(this.username + " has voted");
									if(!Server.currentVoteUser.equals("PASS")) {
										Server.voteLog += "  " + this.username + " voted NO\n";
										Server.log += "  " + this.username + "(" + this.roleName + ") voted NO\n";
									}
								} else {
									writeMsg("You can't vote right now\n");
								}
							
							} else {
								writeMsg("You voted already\n");
							}	
						} else {
							writeMsg("You're dead. Deal with it.\n");
						}
					} else {
						writeMsg("Roles have not been assigned yet\n");
					}
					break;
					
				case Message.NR:
					switch(this.role) {
						case 0:
							this.role++;
							writeMsg("Now Medic\n");
							break;
						case 1:
							this.role++;
							writeMsg("Now Werewolf\n");
							break;
						case 2:
							this.role = 0;
							writeMsg("Now Villager\n");
							break;
					}
					
					this.isAlive = true;
					break;
				case Message.GS:
					if(!Server.areRolesAssigned) {
						Server.areRolesAssigned = true;
					}
					switch(Server.gameState) {
					case 0:
						Server.gameState++;
						writeMsg("Time to protect\n");
						break;
					case 1:
						Server.gameState++;
						writeMsg("Time to kill\n");
						break;
					case 2:
						Server.gameState = 0;
						writeMsg("Time to vote\n");
						break;
					}	
				} 
				// end of switch
				
				
				if(Server.voteInitiated || Server.skipVoteInitiated) {
					
					if(Server.votesCounter == this.getNumberOfAliveUsers()) {
						
						// get lynchPoints for every alive user
						for(int i = 0; i < al.size(); ++i) {
							ClientThread ct = al.get(i);
							if(ct.isAlive) {
								if(ct.lynchScore > Server.maxLynchPoints) {
									Server.maxLynchPoints = ct.lynchScore;
									Server.maxLynchPointsUser = ct.username;
									ct.canBeVotedAgainst = true;
								}
								Server.voteScores += " " + ct.username + " has " + ct.lynchScore + " votes\n";
							}
						}
						
						broadcast(Server.maxLynchPoints + "");
						
						Server.tiebreakers.add(Server.maxLynchPointsUser);
						// check if vote was a draw
						for(int i = 0; i < al.size(); ++i) {
							ClientThread ct = al.get(i);
							if(ct.isAlive) {
								if(!ct.username.equalsIgnoreCase(Server.maxLynchPointsUser) 
									&& ct.lynchScore == Server.maxLynchPoints) {
									Server.voteIsADraw = true;
									Server.tiebreakers.add(ct.username);
									ct.canBeVotedAgainst = true;
									ct.lynchScore = 0;
								}
							}
						}
						
						// if vote was not a draw
						if(!Server.voteIsADraw) {
							for(int i = 0; i < al.size(); ++i) {
								ClientThread ct = al.get(i);
								
								if(ct.username.equalsIgnoreCase(Server.maxLynchPointsUser)) {
									ct.isAlive = false;
									Server.voteLog += "-----\n" + Server.voteScores + "\n";
									Server.voteLog += " " + Server.maxLynchPointsUser + " has been [lynched]. Poor soul.\n----------\n";
									Server.log += " " + Server.maxLynchPointsUser + "(" + ct.roleName + ") has been lynched. Poor soul.\n----------\n";
									break;
								}
							}
						} else { // if it was a draw
							
							Server.voteLog += "-----\n" + Server.voteScores + "\n\n";
							Server.voteLog += " The vote was a draw\n No one was lynched\n----------\n";
							Server.log += " No one was lynched\n----------\n";
						}
						
						Server.maxLynchPoints = 0;
						Server.maxLynchPointsUser = "";
						Server.voteScores = "";
						
						
						/*// vote skip
						if(Server.votes > 0) {
							if(Server.skipVoteInitiated) {
								Server.voteLog += " The villagers desided to [skip] the day\n-----\n";
								Server.log += "  The villagers desided to skip the day\n-----\n";
								
								Server.skipVoteInitiated = false;
							}
						} else {
							if(Server.skipVoteInitiated) {
								Server.voteLog += " The villagers desided [NOT to skip] the day\n-----\n";
								Server.log += "  The villagers desided NOT to skip the day\n-----\n";
								
								Server.skipVoteInitiated = false;
							}
						}
						*/
						
						Server.noMoreLivingWerewolves = true;
						
						// end of vote loop for variable setting on the users
						for(int i = 0; i < al.size(); ++i) {
							ClientThread ct = al.get(i);
							ct.hasVoted = false;
							ct.isProtected = false;
							if(ct.role == 2 && ct.isAlive) {
								Server.noMoreLivingWerewolves = false;
							}
							if(ct.role == 1 && !ct.isAlive) {
								Server.medicIsDead = true;
							}
						}
						
						broadcast("Vote log:\n" + Server.voteLog);
						Server.voteLog = "";
						
						Server.voteInitiated = false;
						Server.votes = 0;
						Server.votesCounter = 0;
						Server.currentVoteUser = "";
						
						// if two or more players are alive
						if(this.getNumberOfAliveUsers() > 2) {
							// if the medic is alive
							if(!Server.medicIsDead) {
								Server.gameState = 1;
								
								broadcast("The village falls asleep");
								broadcast("It's time for the [medic] to make his choice");
							} else { // if the medic is dead
								broadcast("The village falls asleep");
								broadcast("It's time for the [medic] to make his choice");
								try {
									sleep((int) Math.floor(Math.random() * 15000 + 20000));
								} catch (InterruptedException e) {
									
								}
								broadcast("A player has been protected.");
								
								Server.gameState = 2;
								broadcast("It's time for the [werewolves] to make their choice");
							}
						}
					}
				
				}
				
				// if all werewolves are dead
				if(Server.noMoreLivingWerewolves) {
					broadcast("Log:\n" + Server.log);
					broadcast("All werewolves are dead! The [villagers] win!");
					
					keepGoing = false;
				} else { 
					// if only one villager is left
					if(this.getNumberOfAliveUsers() == 2) {
						broadcast("Log:\n" + Server.log);
						broadcast("Only one villager left. The [werewolves] win!");
						
						keepGoing = false;
					}
				}				
				
				
			} 
			// end of infinite loop
				
			// remove myself from the arrayList containing the list of the
			// connected Clients
			remove(id);
			close();
		}
		
		// try to close everything
		private void close() {
			// try to close the connection
			try {
				if(sOutput != null) sOutput.close();
			}
			catch(Exception e) {}
			try {
				if(sInput != null) sInput.close();
			}
			catch(Exception e) {};
			try {
				if(socket != null) socket.close();
			}
			catch (Exception e) {}
		}

		/*
		 * Write a String to the Client output stream
		 */
		private boolean writeMsg(String msg) {
			// if Client is still connected send the message to it
			if(!socket.isConnected()) {
				close();
				return false;
			}
			// write the message to the stream
			try {
				sOutput.writeObject(msg);
			}
			// if an error occurs, do not abort just inform the user
			catch(IOException e) {
				display("Error sending message to " + username);
				display(e.toString());
			}
			return true;
		}
	}
}



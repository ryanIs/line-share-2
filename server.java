import java.sql.*;
import java.io.*;
import java.util.StringTokenizer;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class server implements Runnable {

	public static int maxPlayers = 16;
	public static client players[] = new client[maxPlayers];; // new client[maxPlayers];
	public static final int cycleTime = 500;
	public static boolean updateServer = false;
	public static int updateSeconds = 180; //180 because it doesnt make the time jump at the start :P
	public static long startTime;

	public static server clientHandler = null;			// handles all the clients
	public static java.net.ServerSocket clientListener = null;
	public static boolean shutdownServer = false;		// set this to true in order to shut down and kill the server
	public static boolean shutdownClientHandler;			// signals ClientHandler to shut down
	public static int serverlistenerPort = 43594; //43594=default

	public static int EnergyRegian = 60;

	public static int MaxConnections = 100000;
	public static String[] Connections = new String[MaxConnections];
	public static int[] ConnectionCount = new int[MaxConnections];
	public static boolean ShutDown = false;
	public static int ShutDownCounter = 0;
	
	public static void main(java.lang.String args[]) {
		for (int ii = 0; ii < maxPlayers; ii++ ) {
			players[ii] = null;
		}
		clientHandler = new server();
		(new Thread(clientHandler)).start();			// launch server listener

		int waitFails = 0;
		long lastTicks = System.currentTimeMillis();
		long totalTimeSpentProcessing = 0;
		int cycle = 0;
		while(!shutdownServer) {
		if(updateServer)
			calcTime();
			System.gc();
	
			// taking into account the time spend in the processing code for more accurate timing
			long timeSpent = System.currentTimeMillis() - lastTicks;
			totalTimeSpentProcessing += timeSpent;
			if(timeSpent >= cycleTime) {
				timeSpent = cycleTime;
				if(++waitFails > 100) {
					//shutdownServer = true;
					System.out.println("[KERNEL]: machine is too slow to run this server!");
				}
			}
			try {
				Thread.sleep(cycleTime-timeSpent);
			} catch(java.lang.Exception _ex) { }
			lastTicks = System.currentTimeMillis();
			cycle++;
			if(cycle % 100 == 0) {
				float time = ((float)totalTimeSpentProcessing)/cycle;
				//System.out.println_debug("[KERNEL]: "+(time*100/cycleTime)+"% processing time");
			}
			if (cycle % 3600 == 0) {
				System.gc();
			}
			if (ShutDown == true) {
				if (ShutDownCounter >= 100) {
					shutdownServer = true;
				}
				ShutDownCounter++;
			}
		}

		// shut down the server
		clientHandler.killServer();
		clientHandler = null;
	}
	
	public server() {
		// the current way of controlling the server at runtime and a great debugging/testing tool
		//jserv js = new jserv(this);
		//js.start();
	}
	
	public void run() {
		// setup the listener
		try {
			shutdownClientHandler = false;
			clientListener = new java.net.ServerSocket(serverlistenerPort, 1, null);
			System.out.println("Starting Line Share 2 Server on "+clientListener.getInetAddress().getHostAddress()+":" + clientListener.getLocalPort());
			while(true) {
				java.net.Socket s = clientListener.accept(); // Waits here until someone connects to this address on this port
				s.setTcpNoDelay(true);
				String connectingHost = s.getInetAddress().getHostName();
				if(clientListener != null) {
					if (connectingHost.startsWith("test-server.net")) {
                                               
						System.out.println(connectingHost+": Checking if server still is online...");
					} 
					/*if(!banned(connectingHost)) 
					{
						System.out.println("ClientHandler: Accepted from "+connectingHost+":"+s.getPort());

					}*/else {
						int Found = -1;
						for (int i = 0; i < MaxConnections; i++) {
							if (Connections[i] == connectingHost) {
								Found = ConnectionCount[i];
								break;
							}
						}
						if (Found < 3) {
							// playerHandler.newPlayerClient(s, connectingHost);
							int _playerID = -1;
							for (int j = 0; j < maxPlayers; j++ ) {
								if ( players[j] == null ) {
									_playerID = j;
									client cPlayer = new client (s, connectingHost, _playerID);
									players[_playerID] = cPlayer;
									// System.out.println("ClientHandler: Accepted from "+connectingHost+":"+s.getPort()+" ID: "+_playerID);
									break;
								}
							}
							if ( _playerID == -1 ) { // server is full
								//System.out.println(" ClientHandler: Rejected because server is full ");
								client  rejectedPlayer = new client (s, connectingHost, -1);
							}
						} else {
							s.close();
						}
					}
				} else {
					System.out.println("ClientHandler: Rejected "+connectingHost+":"+s.getPort());
					s.close();
				}
			}
		} catch(java.io.IOException ioe) {
			if(!shutdownClientHandler) {
				System.out.println("Error: Unable to startup listener on "+serverlistenerPort+" - port already in use?");
			} else {
				System.out.println("ClientHandler was shut down.");
			}
		}
	}
	
	public static void calcTime() {
		long curTime = System.currentTimeMillis();
		updateSeconds = 180 - ((int)(curTime - startTime) / 1000);
		if(updateSeconds == 0) {
			shutdownServer = true;
		}
	}


	public void killServer() {
		try {
			shutdownClientHandler = true;
			if(clientListener != null) clientListener.close();
			clientListener = null;
		} catch(java.lang.Exception __ex) {
			__ex.printStackTrace();
		}
	}

}

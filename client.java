/* Java Flash Server-Client V1.00
	Ryan Isler
	Note: When first trying to connect Sam's computer (via Newgrounds .swf) The modem got a "blocked TCP request from (HOUSE_IP):randomport to (HOUSE_IP):843"
	Note: Very specific XML required for flash to stay connected: "<cross-domain-policy><allow-access-from domain=\"*\" to-port \"43594\"/></cross-domain-policy>"
	
*/

import java.sql.*;
import java.io.*;
//import java.util.StringTokenizer;
//import java.util.Calendar;
//import java.util.GregorianCalendar;

public class client implements Runnable {
	
	public final String boss = "g289hq";
	public boolean shutDownClient = false;
	private int i = 0;
	public int playerID = -1;
	public String playerName = "Player";
	public String connectionHost = null;
	public java.net.Socket playerSocket = null;
	public Thread playerThread = null;
	
	private InputStream in = null;
	private OutputStream out = null;
	public stream inStream = null, outStream = null;
	public static final int bufferSize = 1000000;
	public byte buffer[] = null;
	
	public client ( java.net.Socket _socket, String _connectionHost, int _playerID ) {
		playerSocket = _socket;
		connectionHost = _connectionHost;
		playerID = _playerID;
		
		try {
			in = _socket.getInputStream();
			out = _socket.getOutputStream();
		} catch(java.io.IOException ioe) {
			println("IOException");
			ioe.printStackTrace(); 
		}
	
		outStream = new stream(new byte[bufferSize]);
		inStream = new stream(new byte[bufferSize]);

		buffer = new byte[bufferSize];
		
		playerThread = new Thread(this);
		playerThread.start();
		//(new Thread(clientHandler)).start();
	}
	
	public void run() {
		try {
			clientWrite ( "<cross-domain-policy><allow-access-from domain=\"*\" to-ports=\"43594\"/></cross-domain-policy>" );
			if ( playerID != -1 ) {
				printlnC ( connectionHost + " " + "CLIENT INITIATION. ===== \3" );
				while ( shutDownClient == false ) { // println ( "byte: " + inStream.readUnsignedByte() + " byte: " + inStream.readUnsignedByte() + " byte: " + inStream.readUnsignedByte() );
					fillInStream ( 2 ); 
					if ( inStream.buffer[0] == 0x7E && inStream.buffer[1] == 0x0 ) { // ~ and \0 ( 126 )
						fillInStream ( 65 );
						String inData = inStream.readString();
						inData = (inData.split("\1"))[0]; 
						handleData ( inData );
					} else if ( playerSocket.isClosed() == false ) {
						shutDownClient = true;
					}
					//try { Thread.sleep ( 100 ); } catch ( Exception e ) { println("Thread Exception"); }
				}
				printlnC ( connectionHost + " " + "CLIENT DESTROY. ===== \4" );
				server.players[playerID] = null;
				clientWriteAll ( "1~" + boss + "~" + playerName + " has left the cool club." );
			}
			else { // Connection successful, however I was never assigned a player ID (server full)
				clientWrite ( "10~" ); // Server full packet, wait
				printlnC ( connectionHost + " " + "CLIENT REJECTED: SERVER FULL. ===== \5" );
			}
			playerThread.stop(); // playerThread.stop();
		} catch ( java.io.IOException ioe ) {
			println ("client.run(): EXCEPTION");
		}
	}
	public void handleData ( String _d ) {
		String __d = _d; 
		String a[] = __d.split("~");
		int packetID = Integer.parseInt ( a[0] );
		switch ( packetID ) {
			case 0: // requesting username set
			String pName = a[1];
			int returnID = playerID; // -1: username taken, else: username available
			for ( i = 0; i < server.maxPlayers; i++ ) {
				if ( server.players[i] != null ) {
					client c = server.players[i];
					if ( c.playerName.equalsIgnoreCase ( pName ) && c.playerID != playerID ) {
						returnID = -1; // That username is taken.
						break;
					}
				}
			}
			if ( returnID != -1 ) {
				playerName = pName;
				printlnCA ( "LOGIN SUCESSFUL" );
			} else {
				//printlnCA ( "LOGIN UNSUCESSFULL (who cares)" );
			}
			clientWrite ( "0~" + returnID );
			break;
			
			case 1: // send chat to everyone
			String messageToAll = a[1];
			clientWriteAll ( "1~" + playerName + "~" + messageToAll );
			break;
			
			case 2: // client requests all player IDs and usernames. 
			for ( i = 0; i < server.maxPlayers; i++ ) {
				if ( server.players[i] != null ) {
					client c = server.players[i];
					if ( c.playerID != playerID ) {
						c.clientWrite ( "2~" + playerID + "~" + playerName ); // tell everyone else I'm here
					}
					clientWrite ( "2~" + c.playerID + "~" + c.playerName ); // Tell me who is here
				}
			}
			clientWriteAll ( "1~" + boss + "~" + playerName + " has joined the cool club." );
			break;
			
			case 3: // client tells other clients my pen has moved
			for ( i = 0; i < server.maxPlayers; i++ ) {
				if ( server.players[i] != null ) {
					client c = server.players[i];
					if ( c.playerID != playerID ) {
						c.clientWrite ( "3~" + playerID + "~" + a[1] + "~" + a[2] );
					}
				}
			}
			break;
			
			case 4: // Writing on the board
			clientWriteAll ( "4~" + playerID + "~" + a[1] + "~" + a[2] );
			break;
			
			case 5: // Start writing on the board
			clientWriteAll ( "5~" + playerID + "~" + a[1] + "~" + a[2] );
			break;
			
			case 6: // Update pen color, pen size, fill color, and tool
			clientWriteAll ( "6~" + playerID + "~" + a[1] + "~" + a[2] + "~" + a[3] + "~" + a[4] );
			break;
			
			case 7: // I want to erase the field
			clientWriteAll ( "7~" + playerID );
			break;
			
			case 8: // Ending coords of a square being completed ( clients already have starting coords from ase 5 )
			clientWriteAll ( "8~" + playerID + "~" + a[1] + "~" + a[2] );
			break;
			
			case 9: // Ban hammer
			int banID = Integer.parseInt ( a[1] );
			if ( server.players[banID] != null ) {
				server.players[banID].clientWrite ( "9~" ); // Banned
			}
			break;
			
			default:
			println ( "Unhandled packetID: " + packetID );
			break;
		}
		return;
	}
	
	public void clientWriteAll ( String _s ) {
		for ( i = 0; i < server.maxPlayers; i++ ) {
			if ( server.players[i] != null ) {
				server.players[i].clientWrite ( _s );
			}
		}
	}
	
	public void clientWrite ( String _s ) {
		try { 
			if ( outStream != null ) {
				outStream.writeString ( _s );
				directFlushOutStream();
			}
		} catch ( java.io.IOException ioe ) { println ( "clientWrite Root IOException: "+ioe.getMessage() ); }
		return;
	}
	
	/*
	public void flushOutStream() {
		if(disconnected || outStream.currentOffset == 0) return;

		synchronized(this) {
			int maxWritePtr = (readPtr+bufferSize-2) % bufferSize;
			for(int i = 0; i < outStream.currentOffset; i++) {
				buffer[writePtr] = outStream.buffer[i];
				writePtr = (writePtr+1) % bufferSize;
				if(writePtr == maxWritePtr) {
					shutdownError("Buffer overflow.");
					//outStream.currentOffset = 0;
					disconnected = true;
					return;
				}
          		}
			outStream.currentOffset = 0;

			notify();
		}
   	 } */
	private void directFlushOutStream() throws java.io.IOException {
		out.write(outStream.buffer, 0, outStream.currentOffset);
		outStream.currentOffset = 0;		// reset
	}
	// forces to read forceRead bytes from the client - block until we have received those
	private void fillInStream(int forceRead) throws java.io.IOException {
		inStream.currentOffset = 0;
		in.read(inStream.buffer, 0, forceRead);
	}
	
	
	public void println (String _ln) {
		System.out.println (_ln);
		return;
	}
	
	public void printlnC (String _ln) {
		println ( "\n " + playerID + " " + playerName + " " + _ln );
		return;
	}
	
	public void printlnCA (String _ln) {
		println ( " " + playerID + " " + playerName + " " + _ln );
		return;
	}
	
}
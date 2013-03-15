/**
 * 
 */
package com.subject17.jdfs.client;

import java.io.IOException;
import java.util.Scanner;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.subject17.jdfs.client.account.AccountManager;
import com.subject17.jdfs.client.io.Printer;
import com.subject17.jdfs.client.net.PortMgr;
import com.subject17.jdfs.client.net.reciever.Listener;
import com.subject17.jdfs.client.net.sender.Talker;
import com.subject17.jdfs.client.peers.PeersHandler;
import com.subject17.jdfs.client.settings.reader.PeerSettingsReader;
import com.subject17.jdfs.client.settings.reader.SettingsReader;
/**
 * @author James Hughes
 *
 */
public class UserNode {	
	public static Listener serv;
	public static SettingsReader settingsReader;
	public static PeersHandler peers;
	public static AccountManager accountMgr;
	/**
	 * @param args  There will be a "nogui" flag here, or maybe a "gui" flag, which will affect
	 * whether or not a graphical interface appears to edit settings.
	 * 
	 * Also, they will be able to change the space allocated to other's files, the directory where those
	 * files are stored, and possibly the email associated with their account
	 * 
	 */
	public static void main(String[] args) {
		try {
			Scanner inScan = new Scanner(System.in);
			// TODO Auto-generated method stub
			initializeSettingsAndHandlers();
			//dispatchServer(); //Spawn child process here.  Will constantly listen for and manage the files for other peers
			
			//dispatchWatchService(); //Will get the directories and files to watch from configuration.
									//upon change, spawns new child process that will attempt to connect to peers (eventually extended to a peer server)
									//and send the modified files over.
			//Now, how to close program?
			
			
			Printer.println("Do you wish to start a server or client?");
			Printer.println("1) [S]erver");
			Printer.println("2) [C]lient");
			switch(inScan.next().toLowerCase().charAt(0)) {
				case 's': case '1': dispatchServer(); break;
				case 'c': case '2': dispatchClient(); break;
			}
			inScan.close();
		} catch (Exception e) {
			Printer.logErr("An exception was encountered running the program:  Terminating application", Printer.Level.Extreme);
			Printer.logErr(e);
			e.printStackTrace();
		}
	}
	
	private static void initializeSettingsAndHandlers() throws Exception {
		//This function will handle setting up any settings and any handlers related to them
		settingsReader = new SettingsReader();
		peers = new PeersHandler(settingsReader.getPeerSettingsFile());
		accountMgr = new AccountManager(settingsReader.getUserSettingsFile());
		
		//Next, add in code for watch service monitor
		
		//TODO: put dispatch server code in here as well
		
		//TODO Finally:  Add in logic for gui
	}
	
	private static void dispatchServer() {
		Printer.log("Dispatching server");
		//TODO: Find a better way to choose port for server than just getting a random one
		int port;
		try {
			port = PortMgr.getRandomPort();
			Printer.log("Starting Server");
			
			serv = new Listener(port);
			serv.createListener();
		} catch (Exception e) {
			Printer.logErr("Could not start server");
			e.printStackTrace();
		}
	}
	
	
	//this function for testing only TODO delete and move code to watch service
	private static void dispatchClient() { //public for now
		Printer.log("dispatching client");
		try {
			Scanner inScan = new Scanner(System.in);
			int port;
			String serverName;
			
			do {
				Printer.println("What server do you wish to use?");
				serverName = inScan.next();
				if (!serverName.equals("exit")) {
					Printer.println("What port do you wish to use?");
					port = inScan.nextInt();
					
					Printer.log("Using server "+serverName+":"+port);
					
					Talker talk = new Talker(serverName, port);
					talk.createTalker();
					Printer.log("Closed talker");
				}
			} while(!serverName.equals("exit"));
			
			inScan.close();
		} catch (Exception e){}
	}
	
	private static void dispatchWatchService() {
		
	}
}

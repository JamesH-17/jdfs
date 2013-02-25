/**
 * 
 */
package com.subject17.jdfs.client;

import java.util.Scanner;

import com.subject17.jdfs.client.io.Printer;
import com.subject17.jdfs.client.net.PortMgr;
import com.subject17.jdfs.client.net.reciever.Listener;
import com.subject17.jdfs.client.settings.PeersHandler;
import com.subject17.jdfs.client.settings.SettingsReader;
/**
 * @author James Hughes
 *
 */
public class UserNode {	
	public static SettingsReader reader;
	public static PeersHandler peers;
	/**
	 * @param args  There will be a "nogui" flag here, or maybe a "gui" flag, which will affect
	 * whether or not a graphical interface appears to edit settings.
	 * 
	 * Also, they will be able to change the space allocated to other's files, the directory where those
	 * files are stored, and possibly the email associated with their account
	 * 
	 */
	public static void main(String[] args) {
		Scanner inScan = new Scanner(System.in);
		// TODO Auto-generated method stub
		initialize();
		//dispatchServer(); //Spawn child process here.  Will constantly listen for and manage the files for other peers
		
		//dispatchWatchService(); //Will get the directories and files to watch from configuration.
								//upon change, spawns new child process that will attempt to connect to peers (eventually extended to a peer server)
								//and send the modified files over.
		//Now, how to close program?
		
		
		Printer.println("Do you wish to start a server or client?");
		Printer.println("1) [S]erver");
		Printer.println("2) [C]lient");
		switch(inScan.next().toLowerCase().charAt(0)) {
			case 's': case '1': dispatchClient(); break;
			case 'c': case '2': dispatchServer(); break;
		}
		inScan.close();
	}
	
	private static void initialize(){
		reader = new SettingsReader();
		String peerFileLocation = reader.getPeerFileLocation();
		peers = new PeersHandler(peerFileLocation);
		
		//Next, read in user account data
		//This will also read in what directories are being watched 
		
	}
	
	private static void dispatchServer() {
		//int port = (int)(Math.random()*Math.pow(2, 15));
		int port;
		try {
			port = PortMgr.getRandomPort();
			Listener serv = new Listener(port);
		} catch (Exception e) {
			Printer.logErr("Could not start server");
			e.printStackTrace();
		}
	}
	
	
	//this function for testing only TODO delete and move code to watch service
	public static void dispatchClient() { //public for now
		
	}
	
	private static void dispatchWatchService() {
		
	}
}

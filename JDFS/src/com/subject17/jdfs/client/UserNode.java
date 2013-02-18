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
	 * @param args 
	 */
	public static void main(String[] args) {
		Scanner inScan = new Scanner(System.in);
		// TODO Auto-generated method stub
		initialize();
		
		Printer.println("Do you wish to start a server or client?");
		Printer.println("1) Server");
		Printer.println("2) Client");
		switch(inScan.next().toLowerCase().substring(0,1)) {
			case "s": case "1": dispatchClient(); break;
			case "c": case "2": dispatchServer(); break;
		}
		inScan.close();
	}
	
	private static void initialize(){
		reader = new SettingsReader();
		String peerFileLocation = reader.getPeerFileLocation();
		peers = new PeersHandler(peerFileLocation);
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

	public static void dispatchClient() {
		
	}
}

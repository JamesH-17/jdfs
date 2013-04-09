/**
 * @author James Hughes
 */
package com.subject17.jdfs.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Scanner;

import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.digests.SHA3Digest;

import com.subject17.jdfs.client.account.AccountManager;
import com.subject17.jdfs.client.io.Printer;
import com.subject17.jdfs.client.net.PortMgr;
import com.subject17.jdfs.client.net.reciever.Listener;
import com.subject17.jdfs.client.net.sender.Talker;
import com.subject17.jdfs.client.peers.PeersHandler;
import com.subject17.jdfs.client.settings.reader.SettingsReader;

public class UserNode {	
	public static Listener serv;
	
	
	/**
	 * @param args  There will be a "nogui" flag here, or maybe a "gui" flag, which will affect
	 * whether or not a graphical interface appears to edit settings.
	 * 
	 * Also, they will be able to change the space allocated to other's files, the directory where those
	 * files are stored, and possibly the email associated with their account
	 * 
	 */
	public static void main(String[] args) {
		Printer.log("Program started");
		
		try {
			Scanner inScan = new Scanner(System.in);
			
			String hashTest = "hello world";
			String saltsies = "JDFS-ElizabethtownCollege-JamesHughes-Subject17-BarryWittman";
			
			System.out.println("HashTest length:"+hashTest.length());
			System.out.println("Salt length:"+saltsies.length());
			
			SHA3Digest sha3Dig = new SHA3Digest();
			System.out.println("sha3dig len"+sha3Dig.getDigestSize());
			System.out.println("sha3dig byteLen"+sha3Dig.getByteLength());
			sha3Dig.update(hashTest.getBytes(), 0, hashTest.length());
			

			System.out.println("sha3dig len"+sha3Dig.getDigestSize());
			System.out.println("sha3dig byteLen"+sha3Dig.getByteLength());
			sha3Dig.update(saltsies.getBytes(),0,saltsies.length());

			System.out.println("sha3dig len"+sha3Dig.getDigestSize());
			System.out.println("sha3dig byteLen"+sha3Dig.getByteLength());
			
			SHA256Digest sha2Dig = new SHA256Digest();
			sha2Dig.update(hashTest.getBytes(), 0, hashTest.length());
			sha2Dig.update(saltsies.getBytes(),0,saltsies.length());
			System.out.println(sha2Dig.getByteLength());
			System.out.println(sha2Dig.getDigestSize());
			
			//I have no clue why this returns 36 instead of 32 bytes.  They're not using 7-bit bytes since there are a lot of negatives.
			byte[] bytes = new byte[36];
			sha3Dig.doFinal(bytes, 0);
			for (int i = 0; i < bytes.length; ++i){
				System.out.println("Byte #"+i+":"+bytes[i]);
			}
			
			byte[] bytesSHA2 = new byte[32];
			sha2Dig.doFinal(bytesSHA2, 0);
			for (int i = 0; i < bytesSHA2.length; ++i){
				System.out.println("Byte #"+i+":"+bytesSHA2[i]);
			}
			
			
			MessageDigest dig = MessageDigest.getInstance("SHA-256");
			dig.update(saltsies.getBytes());
			dig.update(hashTest.getBytes());
			byte[] bites = dig.digest();
			for (int i = 0; i < bites.length; ++i){
				System.out.println("Byte #"+i+":"+bites[i]);
			}
			
			String s = new String(bites);
			String s2 = new String(bytesSHA2);
			System.out.println("sha2 built in");
			System.out.println(s);
			System.out.println("sha2 bouncy");
			System.out.println(s2);
			
			
			
			
			makeEnvironment();
			//initializeSettingsAndHandlers();
			//dispatchServer(); //Spawn child process here.  Will constantly listen for and manage the files for other peers
			
			//dispatchWatchService(); //Will get the directories and files to watch from configuration.
									//upon change, spawns new child process that will attempt to connect to peers (eventually extended to a peer server)
									//and send the modified files over.
			//Now, how to close program?
			
			System.exit(0);
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
	
	private static void makeEnvironment() throws IOException {
		Path tempDirectory = Paths.get(System.getProperty("user.dir")).resolve("temp");
		if (!Files.exists(tempDirectory)) {
			Files.createDirectory(tempDirectory);
		}
	}
	
	private static void initializeSettingsAndHandlers() throws Exception {
		//This function will handle setting up any settings and any handlers related to them
		Printer.log("Initializing reader....");
		SettingsReader settingsReader = new SettingsReader();
		
		Printer.log("Reader initialized.  Starting Services...");
		
		Printer.log("Starting Peers Handler");
		PeersHandler.setPeersSettingsFile(settingsReader.getPeerSettingsPath());
		
		Printer.log("Starting Account Manager");
		AccountManager.setUsersSettingsFile(settingsReader.getUserSettingsPath());
		
		Printer.log("Starting Watch Service");
		//FileWatcher.setWatchSettingsFile(settingsReader.getWatchSettingsPath());
		
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

/**
 * @author James Hughes
 */
package net.subject17.jdfs.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

import net.subject17.jdfs.client.account.AccountManager;
import net.subject17.jdfs.client.file.db.DBManager;
import net.subject17.jdfs.client.file.db.DBManager.DBManagerFatalException;
import net.subject17.jdfs.client.file.monitor.FileWatcher;
import net.subject17.jdfs.client.file.monitor.PeriodicFileUpdater;
import net.subject17.jdfs.client.io.Printer;
import net.subject17.jdfs.client.net.PortMgr;
import net.subject17.jdfs.client.net.sender.Talker;
import net.subject17.jdfs.client.net.server.Listener;
import net.subject17.jdfs.client.peers.PeersHandler;
import net.subject17.jdfs.client.settings.reader.PeerSettingsReader;
import net.subject17.jdfs.client.settings.reader.SettingsReader;
import net.subject17.jdfs.client.settings.reader.UserSettingsReader;
import net.subject17.jdfs.client.settings.reader.WatchSettingsReader;
import net.subject17.jdfs.client.settings.reader.SettingsReader.SettingsReaderException;
import net.subject17.jdfs.client.settings.writer.SettingsWriter;


public class UserNode {	
	public static Thread serverThread;
	public static Listener serv;
	
	public static Thread updateCheckerThread;
	public static PeriodicFileUpdater updateChecker;
	
	
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
		
		handleArgs(args);	
		
		try (Scanner inScan = new Scanner(System.in)){
			makeEnvironment();
			initializeSettingsAndHandlers();
			//dispatchServer(); //Spawn child process here.  Will constantly listen for and manage the files for other peers
			
			//dispatchWatchService(); //Will get the directories and files to watch from configuration.
									//upon change, spawns new child process that will attempt to connect to peers (eventually extended to a peer server)
									//and send the modified files over.
			//Now, how to close program?
			/*
			System.exit(0);
			Printer.println("Do you wish to start a server or client?");
			Printer.println("1) [S]erver");
			Printer.println("2) [C]lient");
			
			switch(inScan.next().toLowerCase().charAt(0)) {
				case 's': case '1': dispatchServer(); break;
				case 'c': case '2': dispatchClient(); break;
			}
			*/
		} catch (Exception e) {
			Printer.logErr("An exception was encountered running the program:  Terminating application", Printer.Level.Extreme);
			Printer.logErr(e);
		} finally {
			shutdown();
		}
		
		Printer.log("Program Terminated");
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
		SettingsReader settingsReader = SettingsReader.getInstance();
		
		Printer.log("Reader initialized.  Starting Services...");
		
		Printer.log("Starting Peers Handler");
		PeersHandler.setPeersSettingsFile(settingsReader.getPeerSettingsPath());
		
		Printer.log("Starting Account Manager");
		AccountManager.getInstance().setUsersSettingsFile(settingsReader.getUserSettingsPath());
		
		Printer.log("Starting Watch Service");
		FileWatcher.setWatchSettingsFile(settingsReader.getWatchSettingsPath());
		
		//Next, add in code for watch service monitor
		
		//TODO: put dispatch server code in here as well
		
		//TODO Finally:  Add in logic for gui
	}
	
	private static void dispatchServer() {
		Printer.log("Dispatching server");
		//TODO: Find a better way to choose port for server than just getting a random one
		
		try {
			Printer.log("Starting Server");
			
			serverThread = new Thread(serv = new Listener());
			serverThread.setDaemon(true);
			
			serv.run();
		}
		catch (Exception e) {
			Printer.logErr("Could not start server");
			Printer.logErr(e);
		}
	}
	
	
	//this function for testing only TODO delete and move code to watch service
	@Deprecated
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
		} catch (Exception e) {
			Printer.logErr("Error in dispatch client, TalkerPooler");
			Printer.logErr(e);
		}
	}
	
	private static void dispatchWatchService() {
		updateCheckerThread = new Thread(updateChecker = new PeriodicFileUpdater());
		updateCheckerThread.run();
	}
	
	private static void handleArgs(String[] args) {
		//TODO: for args:  Allow to pass in a settings config file
		for (String arg : args) {
			if (arg.toLowerCase().startsWith("defaultserverport")) {
				try {
					String[] strs = arg.split("=");
					if (strs.length > 1) {
						int customDefaultPort = Integer.parseInt(strs[1]);
						PortMgr.setDefaultPort(customDefaultPort);
					}
				}
				catch (NumberFormatException e) {
					Printer.logErr("Exception encountered trying to parse default port passed");
					Printer.logErr(e);
				}
			} else if (arg.toLowerCase().startsWith("defaultserverport")) {
				
			}
		}
	}
	
	private static void shutdown() {
		Printer.log("Preforming shutdown");
		
		//////////////////////////////////////////////////////////////////
		//	Try to separate possible failures as much as possible here	//
		//	Basically, wrap almost every task in a separate try/catch	//
		//////////////////////////////////////////////////////////////////
		
		//Write Settings
		try {
			SettingsWriter writer = new SettingsWriter();
			writer.writeXMLSettings(SettingsReader.getInstance().getSettingsPath());
		}
		catch (SettingsReaderException e) {
			Printer.logErr("A fatal error occured writing settings to xml.");
			Printer.logErr(e);
		}
		catch (Exception e) {
			Printer.logErr("A fatal error occured writing settings to xml.");
			Printer.logErr(e);
		}
		
		//Write Peers
		try {
			PeersHandler.writePeersToFile( PeerSettingsReader.getInstance().getPeerSettingsPath() );
		}
		catch (SettingsReaderException e) {
			Printer.logErr("A fatal error occured writing peer information to xml.");
			Printer.logErr(e);
		}
		catch (Exception e) {
			Printer.logErr("A fatal error occured writing peer information to xml.");
			Printer.logErr(e);
		}
		
		//Write Users
		try {
			AccountManager.getInstance().writeUsersToFile( UserSettingsReader.getInstance().getUserSettingsPath() );
		}
		catch (SettingsReaderException e) {
			Printer.logErr("A fatal error occured writing user data to xml.");
			Printer.logErr(e);
		}
		catch (Exception e) {
			Printer.logErr("A fatal error occured writing user data to xml.");
			Printer.logErr(e);
		}
		
		//Write WatchFiles TODO is this needed?
		try {
			FileWatcher.writeWatchListsToFile(WatchSettingsReader.getInstance().getWatchSettingsPath());
		} catch (SettingsReaderException e) {
			Printer.logErr("A fatal error occured writing settings.");
			Printer.logErr(e);
		}
		catch (Exception e) {
			Printer.logErr("A fatal error occured writing watch list data to xml.");
			Printer.logErr(e);
		}

		try {
			DBManager.getInstance().finalizeSesssion();
		}
		catch (DBManagerFatalException e) {
			Printer.logErr("A fatal error occured finalizing the DB session.");
			Printer.logErr(e);
		}
		catch (Exception e) {
			Printer.logErr("A fatal error occured finalizing the DB session.");
			Printer.logErr(e);
		}
		
		try {
			//Stop the listener
			synchronized(serverThread) {
				synchronized (serv) {
					serv.stopListener();
					serv = null;
				}
				serverThread.interrupt();
				serverThread = null;
			}
		}
		catch (Exception e) {
			Printer.logErr("A fatal error occured shutting down the Listener.");
			Printer.logErr(e);
		}
		
		try {
			//Stop the file updater
			synchronized(updateCheckerThread) {
				synchronized (updateChecker) {
					updateChecker.stopChecking();
					updateChecker = null;
				}
				updateCheckerThread.interrupt();
				updateCheckerThread = null;
			}
		}
		catch (Exception e) {
			Printer.logErr("A fatal error occured shutting down the file update checker.");
			Printer.logErr(e);
		}
	}
}

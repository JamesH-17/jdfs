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
import net.subject17.jdfs.client.io.UserGUI;
import net.subject17.jdfs.client.io.UserInput;
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
	private static boolean continueProgram = true;
	
	/**
	 * @param args  There will be a "nogui" flag here, or maybe a "gui" flag, which will affect
	 * whether or not a graphical interface appears to edit settings.
	 * 
	 * Also, they will be able to change the space allocated to other's files, the directory where those
	 * files are stored, and possibly the email associated with their account
	 * 
	 */
	public static void main(String[] args) {
		UserInput.getInstance(); //Need to run this statement to show exit button
		//Printer.log(UserInput.getInstance().getNextString("Hello"));
		
		
		Printer.log("Program started");
		
		handleArgs(args);	
		
		try (Scanner inScan = new Scanner(System.in)){
			makeEnvironment();
			initializeSettingsAndHandlers();
			
			dispatchServer(); //Spawn child process here.  Will constantly listen for and manage the files for other peers
			
			dispatchWatchService(); //Will get the directories and files to watch from configuration.
									//upon change, spawns new child process that will attempt to connect to peers (eventually extended to a peer server)
									//and send the modified files over.
			dispatchFileMonitor();
			
						
			while(continueProgram) {
				Thread.sleep(1000);
			};
			
			Printer.log("Exiting program");
			
			
			updateChecker.stopChecking();
			FileWatcher.stopWatchEventDispatcher();
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
		AccountManager.getInstance().readUsersFromFile(settingsReader.getUserSettingsPath());
		
		Printer.log("Starting Watch Service");
		FileWatcher.setWatchSettingsFile(settingsReader.getWatchSettingsPath());
	
	}
	
	private static void dispatchServer() {
		Printer.log();
		Printer.log("Dispatching server");
		
		try {
			Printer.log("Starting Server");
			
			serverThread = new Thread(serv = new Listener());
			serverThread.setDaemon(true);
			
			serverThread.start();
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
	
	private static void dispatchWatchService() throws IOException, DBManagerFatalException {
		FileWatcher.setActiveWatchList(AccountManager.getInstance().getActiveUser());
		FileWatcher.startWatchEventDispatcher();
		
	}
	
	private static void dispatchFileMonitor() {
		Printer.log("Distpatching FileMonitor");
		
		updateCheckerThread = new Thread(updateChecker = new PeriodicFileUpdater());
		updateCheckerThread.start();
		//updateCheckerThread.run();
		//
		Printer.log("Distpatched");
	}
	
	private static void handleArgs(String[] args) {
		//TODO: for args:  Allow to pass in a settings config file
		for (String arg : args) {
			try {
				String[] strs = arg.split("=");
				if (strs.length > 1) { 
					if (strs[0].toLowerCase().contains("defaultserverport")) {
								int customDefaultPort = Integer.parseInt(strs[1]);
								PortMgr.setDefaultPort(customDefaultPort);
					}
					else if (strs[0].toLowerCase().contains("settingsPath")) {
						//set settings path
					}
				}
			}
			catch (NumberFormatException e) {
				Printer.logErr("Exception encountered trying to parse default port passed");
				Printer.logErr(e);
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
			Printer.log("Writing XML settings");
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
			Printer.log("Writing Peer settings");
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
			Printer.log("Writing account settings");
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
		
		//Write WatchFiles //TODO follow subdirectories hard set to true
		try {
			Printer.log("Shutting down file watcher");
			FileWatcher.writeWatchListsToFile(WatchSettingsReader.getInstance().getWatchSettingsPath());
		}
		catch (SettingsReaderException e) {
			Printer.logErr("A fatal error occured writing settings.");
			Printer.logErr(e);
		}
		catch (Exception e) {
			Printer.logErr("A fatal error occured writing watch list data to xml.");
			Printer.logErr(e);
		}

		try {
			Printer.log("Finalizing DB");
			DBManager.getInstance().finalizeSession();
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
			Printer.log("Stopping server");
			
			if (null != serv) {
				synchronized (serv) {
					serv.stopListener();
					serv = null;
				}
			}	
			
			if (null != serverThread) {
				synchronized(serverThread) {
					serverThread.interrupt();
					serverThread = null;
				}
			}
		}
		catch (Exception e) {
			Printer.logErr("A fatal error occured shutting down the Listener.");
			Printer.logErr(e);
		}
		
		try {
			//Stop the file updater
			Printer.log("Stopping file updater");
			
			if (null != updateChecker) {
				synchronized (updateChecker) {
					updateChecker.stopChecking();
					updateChecker = null;
				}
			}
			
			if (null != updateCheckerThread) {
				synchronized(updateCheckerThread) {
					updateCheckerThread.interrupt();
					updateCheckerThread = null;
				}
			}
		}
		catch (Exception e) {
			Printer.logErr("A fatal error occured shutting down the file update checker.");
			Printer.logErr(e);
		}
		
		try {
			UserInput.closeGUI();
		}
		catch(Exception e) {
			Printer.logErr("A fatal error occured shutting down the GUI.");
			Printer.logErr(e);
		}
	}

	public static void exitProgram() {
		continueProgram  = false;
	}
}

/*
	KNOWN BUGS:
	-Currently, we always assume they want directories tracked
*/
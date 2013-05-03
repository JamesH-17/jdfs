package net.subject17.jdfs.client.file.monitor;

import net.subject17.jdfs.client.account.AccountManager;
import net.subject17.jdfs.client.file.db.DBManager.DBManagerFatalException;
import net.subject17.jdfs.client.io.Printer;
import net.subject17.jdfs.client.net.sender.TalkerPooler;
import net.subject17.jdfs.client.peers.PeersHandler;
import net.subject17.jdfs.client.settings.reader.PeerSettingsReader;
import net.subject17.jdfs.client.settings.reader.SettingsReader;
import net.subject17.jdfs.client.settings.reader.UserSettingsReader;
import net.subject17.jdfs.client.settings.reader.WatchSettingsReader;
import net.subject17.jdfs.client.settings.reader.SettingsReader.SettingsReaderException;
import net.subject17.jdfs.client.settings.writer.SettingsWriter;

public final class PeriodicFileUpdater implements Runnable {

	private boolean keepChecking = true;
	private final int millisBreakBetweenUpdates = 10_000;//100_000; //100 seconds
	
	@Override
	public void run() {
		try {
			while (keepChecking) {
				Printer.log("Checking for file updates");
				
				TalkerPooler.checkForUpdates();
				Thread.sleep(millisBreakBetweenUpdates);
				
				Printer.log("Waiting "+(millisBreakBetweenUpdates/1000)+" seconds before next update");
				Printer.log();
			}
		}
		catch (InterruptedException e) {
			Printer.log("FileUpdater thread interrupted");
			Printer.logErr(e);
		}
		catch (DBManagerFatalException e) {
			Printer.logErr("DBManagerFatalException", Printer.Level.Extreme);
			Printer.logErr(e);
		}
		catch (Exception e) {
			Printer.logErr("Exception in PeriodicUpdater", Printer.Level.Extreme);
			Printer.logErr(e);
		}
	}

	public void stopChecking() {
		keepChecking = false;
	}
	
	private void writeSettings() {
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
	}
}

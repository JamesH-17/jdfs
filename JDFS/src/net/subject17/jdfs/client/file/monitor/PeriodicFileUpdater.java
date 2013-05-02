package net.subject17.jdfs.client.file.monitor;

import net.subject17.jdfs.client.file.db.DBManager.DBManagerFatalException;
import net.subject17.jdfs.client.io.Printer;
import net.subject17.jdfs.client.net.sender.TalkerPooler;

public final class PeriodicFileUpdater implements Runnable {

	private boolean keepChecking = true;
	private final int millisBreakBetweenUpdates = 100_000; //100 seconds
	
	@Override
	public void run(){
		while (keepChecking) {
			try {
				TalkerPooler.checkForUpdates();
				Thread.sleep(millisBreakBetweenUpdates);
			}
			catch (InterruptedException e) {
				Printer.log("FileUpdater thread interrupted");
				Printer.logErr(e);
			}
			catch (DBManagerFatalException e) {
				Printer.logErr("DBManagerFatalException", Printer.Level.Extreme);
				Printer.logErr(e);
				break;
			}
		}
	}

	public void stopChecking() {
		keepChecking = false;
	}
}

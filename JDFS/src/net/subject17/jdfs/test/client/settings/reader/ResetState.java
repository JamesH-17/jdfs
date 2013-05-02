package net.subject17.jdfs.test.client.settings.reader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;

import net.subject17.jdfs.client.file.db.DBManager;
import net.subject17.jdfs.client.file.db.DBManager.DBManagerFatalException;
import net.subject17.jdfs.client.io.Printer;

import org.junit.Test;

public class ResetState {

	
	Path localRoot = Paths.get(System.getProperty("user.dir"));
	
	@Test
	public void runSuite() {
		//truncateDB();
		resetFileWatchXML();
	}
	
	public void truncateDB() throws SQLException, DBManagerFatalException {
		DBManager.getInstance().truncateEverything2();
	}
	
	
	public void resetFileWatchXML() {
		Path testSource = localRoot.resolve("TEST").resolve("FileWatchTest.xml");
		Path toDelete = localRoot.resolve("FileWatch.xml");
		
		try {
			Files.deleteIfExists(toDelete);
			Files.copy(testSource, toDelete);
		} catch (IOException e) {
			Printer.logErr(e);
		}
	}

}

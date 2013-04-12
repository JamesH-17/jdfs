package net.subject17.jdfs.test.client.settings.reader;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.xml.parsers.ParserConfigurationException;

import net.subject17.jdfs.JDFSUtil;
import net.subject17.jdfs.client.file.model.WatchDirectory;
import net.subject17.jdfs.client.file.model.WatchFile;
import net.subject17.jdfs.client.file.model.WatchList;
import net.subject17.jdfs.client.io.Printer;
import net.subject17.jdfs.client.settings.reader.SettingsReader;
import net.subject17.jdfs.client.settings.reader.WatchSettingsReader;

import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.SAXException;

public class WatchSettingsReaderTest {
	//private static final String rootDirectory = System.getProperty("user.dir");
	private static final Path rootDirectory = Paths.get(JDFSUtil.defaultDirectory);
	private static final Path rootTestDirectory = rootDirectory.resolve("TEST");
	
	private static SettingsReader settingsReader, settingsReaderTest;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		System.out.println("Root Directory: "+rootDirectory);
		System.out.println("Root Test Directory: "+rootTestDirectory);
		
		settingsReader = new SettingsReader();
		settingsReaderTest = new SettingsReader(rootTestDirectory.resolve("SettingsReaderTest.conf"));
	}
	
	@Test
	public void testNoFile() throws ParserConfigurationException, SAXException, IOException {
		WatchSettingsReader reader = new WatchSettingsReader(settingsReader.getWatchSettingsPath());
		for (WatchList list : reader.getAllWatchLists().values()) {
			Printer.log("User:"+list.getUser());
			
			Printer.log("Directories");
			for (WatchDirectory dir : list.getDirectories()) {
				Printer.log("GUID:"+dir.getGUID());
				Printer.log("Tracks subdirectories: "+dir.followSubDirectories);
				Printer.log("Enabling subdirectory tracking");
				dir.enableSubdirectoryTracking();
				for (Path path : dir.getDirectoriesToWatch()) {
					Printer.log("Watchdirectory: "+path);
				}
			}
			Printer.log("Files");
			for (WatchFile file : list.getFiles()) {
				Printer.log(file.getPath());
				Printer.log(file.getGUID());
			}
		}
	}
	
	@Test
	public void testFile() throws ParserConfigurationException, SAXException, IOException {
		WatchSettingsReader reader = new WatchSettingsReader(settingsReaderTest.getWatchSettingsPath());
		for (WatchList list : reader.getAllWatchLists().values()) {
			Printer.log("User:"+list.getUser());
			
			Printer.log("Directories");
			for (WatchDirectory dir : list.getDirectories()) {
				Printer.log("GUID:"+dir.getGUID());
				Printer.log("Tracks subdirectories: "+dir.followSubDirectories);
				Printer.log("Enabling subdirectory tracking");
				dir.enableSubdirectoryTracking();
				for (Path path : dir.getDirectoriesToWatch()) {
					Printer.log("Watchdirectory: "+path);
				}
			}
			Printer.log("Files");
			for (WatchFile file : list.getFiles()) {
				Printer.log(file.getPath());
				Printer.log(file.getGUID());
			}
		}
	}

}

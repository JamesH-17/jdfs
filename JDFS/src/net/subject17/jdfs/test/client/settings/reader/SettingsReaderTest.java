package net.subject17.jdfs.test.client.settings.reader;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.xml.parsers.ParserConfigurationException;

import net.subject17.jdfs.JDFSUtil;
import net.subject17.jdfs.client.io.Printer;
import net.subject17.jdfs.client.settings.reader.SettingsReader;
import net.subject17.jdfs.client.settings.writer.SettingsWriter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;


public class SettingsReaderTest {
	//private static final String rootDirectory = System.getProperty("user.dir");
	private static final Path rootDirectory = Paths.get(JDFSUtil.defaultDirectory);
	private Path rootTestDirectory;
	
	@Before
	public void setUp() throws Exception {
		System.out.println("Root Directory: "+rootDirectory);
		rootTestDirectory = rootDirectory.resolve("TEST");
		System.out.println("Root Test Directory: "+rootTestDirectory);
	}

	@After
	public void tearDown() throws Exception {
	}
	
	@Test
	public void testDefaultLocations() throws ParserConfigurationException, SAXException, IOException {
		SettingsReader settingsReader = new SettingsReader();
		Printer.log(rootDirectory.resolve("settings.conf").toString());
		assertEquals("Failed settings File equality",rootDirectory.resolve("settings.conf").toString(),settingsReader.getSettingsPath().toString());
		assertEquals("Failed peers settings file equality",rootDirectory.resolve("Peers.xml").toString(),settingsReader.getPeerSettingsPath().toString());
		assertEquals("Failed user settings file equality",rootDirectory.resolve("Users.xml").toString(),settingsReader.getUserSettingsPath().toString());
		assertEquals("Failed watch settings file equality",rootDirectory.resolve("FileWatch.xml").toString(),settingsReader.getWatchSettingsPath().toString());
		assertEquals("Failed storage directory equality",rootDirectory.resolve("storage").toString(),settingsReader.getStorageDirectory().toString());
	}
	
	@Test
	public void testSpecificLocations() throws ParserConfigurationException, SAXException, IOException {
		SettingsReader settingsReader = new SettingsReader(rootTestDirectory.resolve("SettingsReaderTest.conf"));
		assertEquals("","","");
		assertEquals("Failed settings File equality",rootTestDirectory.resolve("SettingsReaderTest.conf").toString(),settingsReader.getSettingsPath().toString());
		assertEquals("Failed peers settings file equality",rootTestDirectory.resolve("PeersTest.xml").toString(),settingsReader.getPeerSettingsPath().toString());
		assertEquals("Failed user settings file equality",rootTestDirectory.resolve("UsersTest.xml").toString(),settingsReader.getUserSettingsPath().toString());
		assertEquals("Failed watch settings file equality",rootTestDirectory.resolve("FileWatchTest.xml").toString(),settingsReader.getWatchSettingsPath().toString());
		assertEquals("Failed storage directory equality",rootTestDirectory.resolve("storage").toString(),settingsReader.getStorageDirectory().toString());
	}
	
	@Test
	public void testWriter() {
		SettingsWriter writer = new SettingsWriter();
		writer.writeXMLSettings(rootTestDirectory.resolve("SettingsWriter.xml"));
	}
}

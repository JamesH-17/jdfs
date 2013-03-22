package com.subject17.jdfs.test.client.settings.reader;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import com.subject17.jdfs.client.io.Printer;
import com.subject17.jdfs.client.settings.reader.SettingsReader;

public class SettingsReaderTest {

	private static final String rootDirectory = System.getProperty("user.dir");
	private String rootTestDirectory;
	
	@Before
	public void setUp() throws Exception {
		System.out.println("Root Directory: "+rootDirectory);
		rootTestDirectory = new File(System.getProperty("user.dir"),"TEST").getCanonicalPath();
		System.out.println("Root Test Directory: "+rootTestDirectory);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() throws ParserConfigurationException, SAXException, IOException {
		try {
			Printer.println("Testing default locations");
			testDefaultLocations();
			Printer.println("Testing specific locations");
			testSpecificLocations();
			Printer.println("All tests passed");
		} catch (ParserConfigurationException e) {
			Printer.logErr("TEST FAILED -- PARSER EXCEPTION");
			Printer.logErr(e);
		}
	}
	
	public void testDefaultLocations() throws ParserConfigurationException, SAXException, IOException {
		SettingsReader settingsReader = new SettingsReader();
		assertEquals(new File(rootDirectory,"settings.conf").getCanonicalPath(),settingsReader.getSettingsPath().toString());
		assertEquals(new File(rootDirectory,"Peers.xml").getCanonicalPath(),settingsReader.getPeerSettingsPath().toString());
		assertEquals(new File(rootDirectory,"Users.xml").getCanonicalPath(),settingsReader.getUserSettingsPath().toString());
		assertEquals(new File(rootDirectory,"FileWatch.xml").getCanonicalPath(),settingsReader.getWatchSettingsPath().toString());
		assertEquals(new File(rootDirectory,"storage").getCanonicalPath(),settingsReader.getStorageDirectory().toString());
	}
	
	public void testSpecificLocations() throws ParserConfigurationException, SAXException, IOException {
		SettingsReader settingsReader = new SettingsReader(rootTestDirectory,"SettingsReaderTest.conf");
		assertEquals("Failed settings File equality",new File(rootTestDirectory,"SettingsReaderTest.conf").getCanonicalPath(),settingsReader.getSettingsPath().toString());
		assertEquals("Failed peers settings file equality",new File(rootTestDirectory,"PeersTest.xml").getCanonicalPath(),settingsReader.getPeerSettingsPath().toString());
		assertEquals("Failed user settings file equality",new File(rootTestDirectory,"UsersTest.xml").getCanonicalPath(),settingsReader.getUserSettingsPath().toString());
		assertEquals("Failed watch settings file equality",new File(rootTestDirectory,"FileWatchTest.xml").getCanonicalPath(),settingsReader.getWatchSettingsPath().toString());
		assertEquals("Failed storage directory equality",new File(rootTestDirectory,"storage").getCanonicalPath(),settingsReader.getStorageDirectory().toString());
	}
}

package com.subject17.jdfs.test.client.settings.reader;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashSet;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.SAXException;

import com.subject17.jdfs.client.io.Printer;
import com.subject17.jdfs.client.peers.Peer;
import com.subject17.jdfs.client.settings.reader.PeerSettingsReader;
import com.subject17.jdfs.client.settings.reader.SettingsReader;

public class PeerSettingsTest {
	private static final String rootDirectory = System.getProperty("user.dir");
	private static String rootTestDirectory;
	
	private static SettingsReader settingsReader, settingsReaderTest;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		System.out.println("Root Directory: "+rootDirectory);
		rootTestDirectory = new File(System.getProperty("user.dir"),"TEST").getCanonicalPath();
		System.out.println("Root Test Directory: "+rootTestDirectory);
		
		settingsReader = new SettingsReader();
		settingsReaderTest = new SettingsReader(rootTestDirectory,"SettingsReaderTest.conf");
	}
/*
	@Test
	public void test() throws ParserConfigurationException, SAXException, IOException, Exception {
		try {
			Printer.println("Testing default file creation");
			testBlankFile();
			
			Printer.println("Testing specific locations");
			testTestFile();
			
			Printer.println("Testing email validation");
			TestRegexEmailValidation();
			
			Printer.println("Testing username validation");
			
			
			Printer.println("All tests passed");
		} catch (ParserConfigurationException e) {
			Printer.logErr("TEST FAILED -- PARSER EXCEPTION");
			Printer.logErr(e);
		}
	}
	*/
	@Test
	public void testBlankFile() throws ParserConfigurationException, SAXException, IOException, Exception {
		PeerSettingsReader peerSettingsReader = new PeerSettingsReader(settingsReader.getPeerSettingsPath());
		assertEquals(new File(rootDirectory,"Peers.xml").getCanonicalPath(),settingsReader.getPeerSettingsPath().toString());
		
		HashSet<Peer> peers = peerSettingsReader.getPeers();
		assertEquals(true, peers != null && peers.isEmpty());
		
		for(Peer peer : peers){
			Printer.log(peer.getEmail());
			Printer.log(peer.getUsername());
			for(String ip4 : peer.getIp4s()){
				Printer.log(ip4);
			}
			for(String ip6 : peer.getIp6s()){
				Printer.log(ip6);
			}
		}
	}
	@Test
	public void testTestFile() throws ParserConfigurationException, SAXException, IOException, Exception {
		PeerSettingsReader peerSettingsReader = new PeerSettingsReader(settingsReaderTest.getPeerSettingsPath());
		
		assertEquals("Failed settings File equality",Paths.get(rootTestDirectory,"SettingsReaderTest.conf").toString(),settingsReaderTest.getSettingsPath().toString());
		assertEquals("Failed peers settings file equality",new File(rootTestDirectory,"PeersTest.xml").getCanonicalPath(),settingsReaderTest.getPeerSettingsPath().toString());
		
		HashSet<Peer> peers = peerSettingsReader.getPeers();
		assertEquals("No peers found (Make sure file pointed to contains some)",false, peers == null || peers.isEmpty());
		int i = 0;
		for(Peer peer : peers){
			if (i++ == 0){
				assertEquals("jdfs-test@mailinator.net",peer.getEmail());
				assertEquals("test",peer.getUsername());
			}
			Printer.log("Email: "+peer.getEmail());
			Printer.log("Username: "+peer.getUsername());
			for(String ip4 : peer.getIp4s()){
				Printer.log(ip4);
			}
			for(String ip6 : peer.getIp6s()){
				Printer.log(ip6);
			}
		}
	}
	@Test
	private void TestRegexEmailValidation(){
		String validEmail = "jdfs-test@mailinator.net";
		assertEquals("Valid email does not match regex",true,validEmail.matches("[^@]+@[^@]+"));
	}

}

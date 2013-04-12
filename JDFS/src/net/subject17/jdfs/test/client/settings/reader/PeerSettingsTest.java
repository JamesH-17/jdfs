package net.subject17.jdfs.test.client.settings.reader;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;

import javax.xml.parsers.ParserConfigurationException;

import net.subject17.jdfs.JDFSUtil;
import net.subject17.jdfs.client.io.Printer;
import net.subject17.jdfs.client.peers.Peer;
import net.subject17.jdfs.client.settings.reader.PeerSettingsReader;
import net.subject17.jdfs.client.settings.reader.SettingsReader;
import net.subject17.jdfs.client.settings.writer.PeerSettingsWriter;

import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.SAXException;


public final class PeerSettingsTest {
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
	public void testBlankFile() throws ParserConfigurationException, SAXException, IOException, Exception {
		PeerSettingsReader peerSettingsReader = new PeerSettingsReader(settingsReader.getPeerSettingsPath());
		assertEquals(rootDirectory.resolve("Peers.xml").toString(), settingsReader.getPeerSettingsPath().toString());
		
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
		
		assertEquals("Failed settings File equality",rootTestDirectory.resolve("SettingsReaderTest.conf").toString(),settingsReaderTest.getSettingsPath().toString());
		assertEquals("Failed peers settings file equality",rootTestDirectory.resolve("PeersTest.xml").toString(),settingsReaderTest.getPeerSettingsPath().toString());
		
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
	public void testWriter() throws Exception {
		PeerSettingsWriter writer = new PeerSettingsWriter();
		
		PeerSettingsReader peerSettingsReader = new PeerSettingsReader(settingsReader.getPeerSettingsPath());
		writer.writePeerSettings(rootTestDirectory.resolve("PeersWriterTestBlank.xml"), peerSettingsReader.getPeers());
		
		peerSettingsReader = new PeerSettingsReader(settingsReaderTest.getPeerSettingsPath());
		writer.writePeerSettings(rootTestDirectory.resolve("PeersWriterTest.xml"), peerSettingsReader.getPeers());
	}
	
	@Test
	public void TestRegexEmailValidation(){
		String validEmail = "jdfs-test@mailinator.net";
		assertEquals("Valid email does not match regex",true,validEmail.matches("[^@]+@[^@]+"));
	}

}

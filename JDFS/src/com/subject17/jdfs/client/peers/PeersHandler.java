package com.subject17.jdfs.client.peers;

import java.io.File;
import java.net.InetAddress;
import java.util.ArrayList;

import org.w3c.dom.Document;

import com.subject17.jdfs.client.settings.reader.PeerSettingsReader;
import com.subject17.jdfs.client.settings.writer.PeerSettingsWriter;

public class PeersHandler {
	
	private static File peersFile;
	private static Document peersXML;
	private static PeerSettingsReader peersReader;
	private static ArrayList<Peer> peers;
	
	public PeersHandler(File peerSettingsFile) throws Exception {
		
	}

	public static File getPeersFile() { return peersFile; }
	public static void addIncomingPeer(InetAddress ip, int port) {
		//TODO add peer to peersfileList
	}
	
	public static Peer getPeerByAccount(String account) {
		for (Peer peer : peers){
			if (peer.getEmail().equals(account))
				return peer;
		}
		return null;
	}
	
	public static Peer getPeerByUsername(String username) {
		for (Peer peer : peers){
			if (peer.getUsername().equals(username))
				return peer;
		}
		return null;
	}
	
	public static ArrayList<Peer> getPeersByIp4(String ip4) {
		ArrayList<Peer> peersMatchingIp4 = new ArrayList<Peer>();
		for (Peer peer : peers){
			if (peer.getIp4s().contains(ip4))
				peersMatchingIp4.add(peer);
		}
		return peersMatchingIp4;
	}
	
	public static ArrayList<Peer> getPeersByIp6(String ip6) {
		ArrayList<Peer> peersMatchingIp6 = new ArrayList<Peer>();
		for (Peer peer : peers) {
			if (peer.getIp6s().contains(ip6))
				peersMatchingIp6.add(peer);
		}
		return peersMatchingIp6;
	}
	
	public void writePeersToFile() {
		writePeersToFile(peersFile);
	}
	
	public void writePeersToFile(String path, String filename) {
		writePeersToFile(new File(path, filename));
	}
	
	public void writePeersToFile(File file) {
		PeerSettingsWriter writer = new PeerSettingsWriter();
		writer.writePeerSettings(file,peers);
	}

	public static void setPeersSettingsFile(File peerSettingsFile) throws Exception {
		peersReader = new PeerSettingsReader(peerSettingsFile);
		peersFile = peerSettingsFile;
		
		peers.addAll(peersReader.getPeers());
		peersXML = peersReader.getPeerDocument(); //TODO is needed?
		
	}
}

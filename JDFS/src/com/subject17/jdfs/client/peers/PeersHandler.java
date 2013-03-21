package com.subject17.jdfs.client.peers;

import java.io.File;
import java.net.InetAddress;
import java.util.HashSet;

import org.w3c.dom.Document;

import com.subject17.jdfs.client.io.Printer;
import com.subject17.jdfs.client.settings.reader.PeerSettingsReader;
import com.subject17.jdfs.client.settings.writer.PeerSettingsWriter;

public class PeersHandler {
	
	private static File peersFile;
	private static PeerSettingsReader peersReader;
	private static HashSet<Peer> peers = new HashSet<Peer>();

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
	
	public static HashSet<Peer> getPeersByIp4(String ip4) {
		HashSet<Peer> peersMatchingIp4 = new HashSet<Peer>();
		for (Peer peer : peers){
			if (peer.getIp4s().contains(ip4))
				peersMatchingIp4.add(peer);
		}
		return peersMatchingIp4;
	}
	
	public static HashSet<Peer> getPeersByIp6(String ip6) {
		HashSet<Peer> peersMatchingIp6 = new HashSet<Peer>();
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
	}
}

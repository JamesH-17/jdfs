package net.subject17.jdfs.client.peers;

import java.net.InetAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;

import net.subject17.jdfs.client.settings.reader.PeerSettingsReader;
import net.subject17.jdfs.client.settings.writer.PeerSettingsWriter;


public class PeersHandler {
	
	private static Path peersFile;
	private static PeerSettingsReader peersReader;
	private static HashSet<Peer> peers = new HashSet<Peer>();

	public static Path getPeersFile() { return peersFile; }
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
		writePeersToFile(Paths.get(path, filename));
	}
	
	public void writePeersToFile(Path file) {
		PeerSettingsWriter writer = new PeerSettingsWriter();
		writer.writePeerSettings(file,peers);
	}

	public static void setPeersSettingsFile(Path peerSettingsFile) throws Exception {
		peersReader = new PeerSettingsReader(peerSettingsFile);
		peersFile = peerSettingsFile;
		peers.addAll(peersReader.getPeers());
	}
}

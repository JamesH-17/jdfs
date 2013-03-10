package com.subject17.jdfs.client.peers;

import java.io.File;
import java.net.InetAddress;
import java.util.ArrayList;

public class PeersHandler {
	
	private static File peersFile;
	private static ArrayList<Peer> peers;
	public PeersHandler(File fileToUse) {setPeersFile(fileToUse);}
	
	public static void setPeersFile(File fileToUse) {
		peersFile = fileToUse;
	}
	
	public static File getPeersFile(){return peersFile;}

	public static void addIncomingPeer(InetAddress ip, int port) {
		//TODO add peer to peersfileList
	}
	
	public static Peer getPeerByAccount(String account) {
		return null;
	}
	
	public static Peer getPeerByUsername(String username) {
		return null;
	}
	
	public static ArrayList<Peer> getPeersByIp4(String ip4) {
		ArrayList<Peer> peersMatchingIp4 = new ArrayList<Peer>();
		
		return peersMatchingIp4;
	}
	
	public static ArrayList<Peer> getPeersByIp6(String ip6) {
		ArrayList<Peer> peersMatchingIp4 = new ArrayList<Peer>();
		
		return peersMatchingIp4;
	}
}

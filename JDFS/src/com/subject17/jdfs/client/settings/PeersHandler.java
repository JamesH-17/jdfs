package com.subject17.jdfs.client.settings;

import java.net.InetAddress;

public class PeersHandler {
	
	private static String peersFileName="";
	public PeersHandler(String peersFile) {setPeersFile(peersFile);}
	
	public static void setPeersFile(String peersFile) {
		peersFileName=peersFile;
	}
	
	public static String getPeersFile(){return peersFileName;}

	public static void addIncomingPeer(InetAddress ip, int port) {
		//TODO add peer to peersfileList
	}
}

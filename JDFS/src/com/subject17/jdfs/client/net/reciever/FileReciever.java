package com.subject17.jdfs.client.net.reciever;

import java.net.ServerSocket;

public class FileReciever {
	
	protected String serverName;
	protected int port;
	
	ServerSocket servSock;
	
	public FileReciever(String server, int prt) {
		serverName=server; port=prt;
	}
	
	public boolean startServer() {
		try {
			
			return true;
		} catch(Exception e){return false;}
	}
	
	public String RecieveFile(String secretMsg, long fileSizeInBytes) {
		
		return "";
	}
}

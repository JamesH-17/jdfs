package com.subject17.jdfs.client.net.reciever;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

import com.subject17.jdfs.client.net.LanguageProtocol;

/**
 * 
 * @author james
 * @code All meta-data relating to the file should be finalized before this transfer is initiated
 */
public class FileReciever {
	
	protected String serverName;
	protected int port;
	
	protected String secretMessage;
	protected int fileSizeInBytes; 
	
	public FileReciever(String server, int prt, String secretMsg, int fileSize, String AESHASH) {
		serverName=server; 
		port=prt; 
		secretMessage=secretMsg;
		fileSizeInBytes=fileSize;
	}
	
	public String RecieveFile() {
		String filename = "test";
		try (ServerSocket servSock = new ServerSocket(port);
			Socket sock = servSock.accept();
			InputStream inStrm = sock.getInputStream();
			FileOutputStream fOut = new FileOutputStream(filename);
			BufferedOutputStream outStrm = new BufferedOutputStream(fOut)
		){
			byte[] bytes=new byte[fileSizeInBytes];
			int bytesRead = inStrm.read(bytes, 0, bytes.length);
			
			assert(bytesRead == fileSizeInBytes);
			
			outStrm.write(bytes);
			outStrm.flush();
			
			return LanguageProtocol.FILE_RECV_SUCC;
		} catch(Exception e){return LanguageProtocol.FILE_RECV_FAIL;}
	}
	
}

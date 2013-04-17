package net.subject17.jdfs.client.net.sender;

import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Path;
import java.util.UUID;

import net.subject17.jdfs.client.file.FileUtil;
import net.subject17.jdfs.client.io.Printer;

public class FileRetriever implements Runnable {
	public String ipAddr;
	public int port;
	public final Path storeLocation;
	public final String jsonPayload;
	public final UUID fileToRetrieveIdentifier;
	
	public FileRetriever(String ip, int port, Path storeLocation, UUID fileUUID, String jsonPayload) {
		this(storeLocation, fileUUID, jsonPayload);
		this.ipAddr = ip;
		this.port = port;
	}
	
	public FileRetriever(Path storeLocation, UUID fileUUID, String jsonPayload) {
		this.fileToRetrieveIdentifier = fileUUID;
		this.jsonPayload = jsonPayload;
		this.storeLocation= storeLocation; 
	}
	
	public void setIP(String ip)	{ this.ipAddr = ip; }
	public void setPort(int port)	{ this.port = port; }
	
	@Override
	public void run() {
		try( Socket socket = new Socket(ipAddr, port);
			 OutputStream toServer = socket.getOutputStream()
		 ) {
		 	//TODO put in a guid
			Printer.log("Sending file "+path.getFileName()+"on connection "+ipAddr+":"+port);
			
			FileUtil.getInstance().readFileToStream(path, toServer);
			
			Printer.log("File sent!");
	    }
	    catch (Exception ex) {
	    	Printer.logErr("Exception encountered in sending file to server "+ipAddr);
	    	ex.printStackTrace();
	    }
	}
}

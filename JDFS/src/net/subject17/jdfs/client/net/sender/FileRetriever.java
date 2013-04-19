package net.subject17.jdfs.client.net.sender;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.UUID;

import net.subject17.jdfs.client.file.FileUtil;
import net.subject17.jdfs.client.file.handler.FileHandler;
import net.subject17.jdfs.client.file.model.FileRetrieverRequest;
import net.subject17.jdfs.client.io.Printer;

public class FileRetriever implements Runnable {
	public String ipAddr;
	public int port;
	public final Path storeLocation;
	//public final String jsonPayload; //FileRetrieverRequest
	public final UUID fileToRetrieveIdentifier;
	public final FileRetrieverRequest criteria;
	
	public FileRetriever(String ip, int port, Path storeLocation, UUID fileUUID, String jsonPayload, FileRetrieverRequest criteria) {
		this(storeLocation, fileUUID, criteria);
		this.ipAddr = ip;
		this.port = port;
	}
	
	public FileRetriever(Path storeLocation, UUID fileUUID, String jsonPayload, FileRetrieverRequest criteria) {
		this.fileToRetrieveIdentifier = fileUUID;
		//this.jsonPayload = jsonPayload;
		this.storeLocation= storeLocation; 
		this.criteria = criteria;
	}
	
	public void setIP(String ip)	{ this.ipAddr = ip; }
	public void setPort(int port)	{ this.port = port; }
	
	@Override
	public void run() {
		try( ServerSocket servSock = new ServerSocket(port);
			 Socket socket = servSock.accept();
			 InputStream fromListener = socket.getInputStream()
		 ) {
		 	//TODO put in a guid
			Printer.log("Recieving file "+storeLocation.getFileName()+" on connection "+ipAddr+":"+port);
			
			//Path storageLocation = FileHandler.getInstance().getStorageLocation
			
			FileUtil.getInstance().readStreamToStream(fromListener, new FileOutputStream(storeLocation.toFile()));
			
			Printer.log("File sent!");
	    }
	    catch (Exception ex) {
	    	Printer.logErr("Exception encountered in sending file to server "+ipAddr);
	    	ex.printStackTrace();
	    }
	}
}

package net.subject17.jdfs.client.net.sender;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import net.subject17.jdfs.client.file.FileUtil;
import net.subject17.jdfs.client.file.model.FileRetrieverRequest;
import net.subject17.jdfs.client.io.Printer;

public class FileRetriever implements Runnable {
	public String ipAddr;
	public int port;
	public final Path storeLocation;
	public final FileRetrieverRequest criteria;
	
	/**
	 * @param storeLocation Write location of received file
	 * @param criteria Criteria used to get file.  If a peer finds a match, they'll send us one
	 */
	public FileRetriever(Path storeLocation, FileRetrieverRequest criteria) {
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
			Printer.log("Recieving file "+storeLocation.getFileName()+" on connection "+ipAddr+":"+port);
			
			FileUtil.getInstance().readStreamToStream(fromListener, new FileOutputStream(storeLocation.toFile()));
			
			Printer.log("File Recieved!");
	    }
	    catch (Exception ex) {
	    	Printer.logErr("Exception encountered in sending file to server "+ipAddr);
	    	ex.printStackTrace();
	    }
	}
}

package net.subject17.jdfs.client.net.sender;

import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;

import net.subject17.jdfs.client.file.FileUtil;
import net.subject17.jdfs.client.io.Printer;
import net.subject17.jdfs.client.net.NetworkUtil;


public class FileSender implements Runnable {
	
	public String ipAddr;
	public int port;
	public final Path path;
	public final String jsonPayload;
	
	public FileSender(String ip, int port, Path pathToUse, String jsonFileInfo){
		this.ipAddr = ip;
		this.port = port;
		this.path = pathToUse;
		this.jsonPayload = jsonFileInfo;
	}
	
	public FileSender(Path fileToSend, String jsonPayload) {
		this.path = fileToSend;
		this.jsonPayload = jsonPayload;  
	}

	public boolean sendFile() {
		 try( Socket socket = new Socket(ipAddr, port);
		      OutputStream toServer = socket.getOutputStream()
		 ) {
		 	//TODO put in a guid
			Printer.log("Sending file "+path.getFileName()+"on connection "+ipAddr+":"+port);
			
			/*
			FileChannel fileSrc = FileChannel.open(path);
			
			
			//Send over the file size first; they're waiting on this
			int fileSize = (int)  Files.size(path);
			ByteBuffer fileSizeBuff = ByteBuffer.allocate(NetworkUtil.numBytesInInt);
			fileSizeBuff.putInt(fileSize);
			toServer.write(fileSizeBuff.array());
			
			//Read in the file
			byte[] fileBytes = new byte[fileSize];
			ByteBuffer fileBytesBuff = ByteBuffer.wrap(fileBytes);
			fileSrc.read(fileBytesBuff);
			
			//Send it to the server
			toServer.write(fileBytesBuff.array());
			toServer.flush();
			*/
			FileUtil.getInstance().readFileToStream(path, toServer);
			
			Printer.log("File sent!");
			return true;
	    }
	    catch (Exception ex) {
	    	Printer.logErr("Exception encountered in sending file to server "+ipAddr);
	    	ex.printStackTrace();
	    	return false;
	    }
	}

	@Override
	public void run() {
		sendFile(); //Don't really care if it succeeds
	}
	
	
}

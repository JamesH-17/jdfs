package com.subject17.jdfs.client.net.sender;

import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;

import com.subject17.jdfs.client.io.Printer;
import com.subject17.jdfs.client.net.NetworkUtil;

public class FileSender {
	
	private final String ipAddr;
	private final int port;
	private final Path path;
	
	public FileSender(String ip, int port, Path pathToUse){
		ipAddr = ip;
		this.port = port;
		path = pathToUse;
	}

	public boolean sendFile() {
		 try( Socket socket = new Socket(ipAddr, port);
		      OutputStream toServer = socket.getOutputStream()
		 ) {
		 	//TODO put in a guid
			Printer.log("Sending file "+path.getFileName()+"on connection "+ipAddr+":"+port);
			
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
			
			Printer.log("File sent!");
			return true;
	    }
	    catch (Exception ex) {
	    	Printer.logErr("Exception encountered in sending file to server "+ipAddr);
	    	ex.printStackTrace();
	    	return false;
	    }
	}
}

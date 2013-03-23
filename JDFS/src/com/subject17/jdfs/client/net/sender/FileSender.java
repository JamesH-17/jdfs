package com.subject17.jdfs.client.net.sender;

import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;

import com.subject17.jdfs.client.io.Printer;

public class FileSender {
	private static final int numBytesInInt = 4;
	
	String ipAddr;
	int port;
	Path path;
	
	public FileSender(String ip, int port, Path pathToUse){
		ipAddr = ip;
		this.port = port;
		path = pathToUse;
	}

	public void sendFile() {
		 try( Socket socket = new Socket(ipAddr, port);
		      OutputStream os = socket.getOutputStream()
		 ) {
		 	//TODO put in a guid
			Printer.log("Seriously, using connection "+ipAddr+":"+port);
			FileChannel fc = FileChannel.open(path);
			
			int size = (int)  Files.size(path);
			ByteBuffer sizeBuff = ByteBuffer.allocate(numBytesInInt);
			sizeBuff.putInt(size);
			os.write(sizeBuff.array());
			
			byte[] bytes = new byte[size];
			ByteBuffer bf = ByteBuffer.wrap(bytes);
			fc.read(bf);
			
			os.write(bf.array());
			os.flush();
			Printer.log("Done writing");
	    }
	    catch (Exception ex) {
	    	Printer.logErr("Exception encountered in sending file to server "+ipAddr);
	    	ex.printStackTrace();
	    }
	}
}

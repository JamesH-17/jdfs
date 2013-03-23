package com.subject17.jdfs.client.net.sender;

import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileSender {
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
		 ){
		 	//TODO put in a guid
			FileChannel fc = FileChannel.open(path);
			
			int size =(int)  Files.size(path) ;
			os.write(size);
			
			byte[] bytes = new byte[size];
			ByteBuffer bf = ByteBuffer.wrap(bytes);
			fc.read(bf);
			
			os.write(bf.array());
			os.flush();
	    }
	    catch (Exception ex) {
	      ex.printStackTrace();
	    }
	}
}

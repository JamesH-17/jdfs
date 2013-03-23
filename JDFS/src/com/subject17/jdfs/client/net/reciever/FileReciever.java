package com.subject17.jdfs.client.net.reciever;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.subject17.jdfs.client.io.Printer;
import com.subject17.jdfs.client.net.LanguageProtocol;

/**
 * 
 * @author james
 * @code All meta-data relating to the file should be finalized before this transfer is initiated
 */
public class FileReciever implements Runnable {
	
	protected String serverName;
	protected int port;
	
	protected String secretMessage;
	protected int fileSizeInBytes; 
	
	public FileReciever(int p){
		port = p;
	}
	public FileReciever(String server, int prt, String secretMsg, int fileSize, String AESHASH) {
		serverName=server; 
		port=prt; 
		secretMessage=secretMsg;
		fileSizeInBytes=fileSize;
	}
	
	public String receiveFile() {
		Path filename = Paths.get(System.getProperty("user.dir"),"test2.txt");
		Printer.log("Awaiting file to recieve");
		
		try (ServerSocket servSock = new ServerSocket(port);
			Socket sock = servSock.accept();
			InputStream inStrm = sock.getInputStream();
		) {
			Printer.log("Oh yeah, we're in!");
			
			//Get filesize
			byte[] numConv = new byte[4];
			inStrm.read(numConv, 0, 4);
			
			fileSizeInBytes = convertBytesToInt(numConv); 
			Printer.log("fileSize:"+fileSizeInBytes);
			
			byte[] bytes = new byte[fileSizeInBytes];
			int bytesRead = inStrm.read(bytes, 0, bytes.length);
			
			assert(bytesRead == fileSizeInBytes);
			
			writeFile(filename,bytes);
			
			Printer.log("Returning");
			
			return LanguageProtocol.FILE_RECV_SUCC;
			
		} catch(Exception e){
			Printer.logErr("Exception encountered in reciving file on port "+port);
			Printer.logErr("fileSizeInBytes"+fileSizeInBytes);
			Printer.logErr(e.getMessage());
			return LanguageProtocol.FILE_RECV_FAIL;
		}
	}
		
	private void writeFile(Path outFile, byte[] bytes) throws IOException {
		Files.deleteIfExists(outFile);
		try(
			FileOutputStream fOut = new FileOutputStream(outFile.toString());
			BufferedOutputStream outStrm = new BufferedOutputStream(fOut)
		) {
			
			Printer.log("Writing bytes");
			outStrm.write(bytes);
			outStrm.flush();
		} catch (IOException e) {
			Printer.logErr("Exception encountered while writing file in FileReciever from client "+serverName+":"+port);
			Printer.logErr(e);
			e.printStackTrace();
		}
	}
	
	public void run() {
		receiveFile();
	}
	
	private final int convertBytesToInt(byte[] bytes){
		return ByteBuffer.wrap(bytes).getInt();
	} 
	
}

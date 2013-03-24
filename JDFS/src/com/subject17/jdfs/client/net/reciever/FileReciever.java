package com.subject17.jdfs.client.net.reciever;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.subject17.jdfs.client.io.Printer;
import com.subject17.jdfs.client.net.LanguageProtocol;
import com.subject17.jdfs.client.net.NetworkUtil;

/**
 * 
 * @author james
 * @code All meta-data relating to the f
 */
public final class FileReciever {
	
	protected final int port;
	//for secure version of transfer
	private final String secretMessage; 
	private final String AESHashOfFile; 
	
	private BufferedReader commIn;
	private PrintWriter commOut;
	
	public FileReciever(int port, BufferedReader commIn, PrintWriter commOut) {
		this.port = port;
		secretMessage = null;
		AESHashOfFile = null;
	}
	//Secure version
	public FileReciever(int port, String secretMessage, String AESHashOfFile, BufferedReader commIn, PrintWriter commOut) {
		this.port = port; 
		this.secretMessage = secretMessage;
		this.AESHashOfFile = AESHashOfFile;
	}
	public String run() {
		if (secretMessage == null || secretMessage.equals("") || AESHashOfFile == null || AESHashOfFile.equals(""))
			return receiveFile();
		else
			return recieveFileSecure();
	}
	
	public String receiveFile() {
		Path filename = Paths.get(System.getProperty("user.dir"),"test2.txt");
		Printer.log("Awaiting file to recieve");
		
		try (ServerSocket servSock = new ServerSocket(port);
			Socket sock = servSock.accept();
			InputStream inStrm = sock.getInputStream();
		) {
			Printer.log("Ready to recieve file -- Connection established");
			
			//Get filesize
			byte[] fileSizeBuff = new byte[4];
			inStrm.read(fileSizeBuff, 0, 4);

			final int fileSizeInBytes = NetworkUtil.convertBytesToInt(fileSizeBuff); 
			Printer.log("fileSize:"+fileSizeInBytes);
			
			//Read in the File
			byte[] bytesRead = new byte[fileSizeInBytes];
			int numBytesRead = inStrm.read(bytesRead, 0, bytesRead.length);
			
			if (numBytesRead == fileSizeInBytes) {
				//Write the received file to disk
				return writeFile(filename, bytesRead);
			} else {
				Printer.log("Error in receiving file -- Bytes read differs from expected by "+bytesRead+" bytes");
				return LanguageProtocol.FILE_RECV_FAIL;
			}
			
		} catch(Exception e){
			Printer.logErr("Exception encountered in reciving file on port "+port);
			Printer.logErr(e.getMessage());
			return LanguageProtocol.FILE_RECV_FAIL;
		}
	}
		
	private String writeFile(Path outFile, byte[] bytes) throws IOException {
		Files.deleteIfExists(outFile);
		try(
			FileOutputStream fOut = new FileOutputStream(outFile.toString());
			BufferedOutputStream outStrm = new BufferedOutputStream(fOut)
		) {
			
			Printer.log("Writing bytes");
			outStrm.write(bytes);
			outStrm.flush();
			
			Printer.log("File received successfully");
			return LanguageProtocol.FILE_RECV_SUCC;
		} catch (IOException e) {
			Printer.logErr("Exception encountered while writing file in FileReciever from client on port "+port);
			Printer.logErr(e);
			e.printStackTrace();
			return LanguageProtocol.FILE_RECV_FAIL;
		}
	}
	
	private String recieveFileSecure() {
		
		return LanguageProtocol.UNSUPPORTED; // TODO Implement this
	}
	
}

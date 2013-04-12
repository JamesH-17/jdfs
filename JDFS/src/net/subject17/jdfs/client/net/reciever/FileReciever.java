package net.subject17.jdfs.client.net.reciever;

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

import net.subject17.jdfs.client.file.model.FileSenderInfo;
import net.subject17.jdfs.client.io.Printer;
import net.subject17.jdfs.client.net.LanguageProtocol;
import net.subject17.jdfs.client.net.NetworkUtil;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;


/**
 * 
 * @author james
 * @code All meta-data relating to the f
 */
public final class FileReciever {
	
	protected final int port;
	//private String secretMessage; 
	//private String AESHashOfFile; 
	private final FileSenderInfo info;
	
	public FileReciever(int port, String json) throws JsonParseException, JsonMappingException, IOException {
		this.port = port;
		ObjectMapper mapper = new ObjectMapper();
		this.info = mapper.readValue(json, FileSenderInfo.class);
	}

	public String run() {
		return receiveFile();
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
			//byte[] fileSizeBuff = new byte[4];
			//inStrm.read(fileSizeBuff, 0, 4);
			//final int fileSizeInBytes = NetworkUtil.convertBytesToInt(fileSizeBuff); 
			Printer.log("fileSize:"+info.size);
			
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
}

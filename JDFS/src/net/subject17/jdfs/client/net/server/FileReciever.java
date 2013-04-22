package net.subject17.jdfs.client.net.server;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;

import net.subject17.jdfs.client.file.FileUtil;
import net.subject17.jdfs.client.file.db.DBManager.DBManagerFatalException;
import net.subject17.jdfs.client.file.handler.FileHandler;
import net.subject17.jdfs.client.file.model.FileSenderInfo;
import net.subject17.jdfs.client.io.Printer;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;


/**
 * 
 * @author james
 * @code All meta-data needed to set up and receive a file on the client server
 */
public final class FileReciever implements Runnable {
	
	protected final int port;
	//private String secretMessage; 
	//private String AESHashOfFile; 
	public final FileSenderInfo info;
	
	public FileReciever(int port, String json) throws JsonParseException, JsonMappingException, IOException {
		this.port = port;
		ObjectMapper mapper = new ObjectMapper();
		this.info = mapper.readValue(json, FileSenderInfo.class);
	}

	@Override
	public void run() {
		Printer.log("Awaiting file to recieve");
		
		try (ServerSocket servSock = new ServerSocket(port);
			Socket sock = servSock.accept();
			InputStream inStrm = sock.getInputStream();
		) {
			Printer.log("Ready to recieve file -- Connection established");
			
			writeFile(inStrm);
			//return LanguageProtocol.FILE_RECV_SUCC;
		} catch(Exception e){
			Printer.logErr("Exception encountered in reciving file on port "+port);
			Printer.logErr(e.getMessage());
		}
		//return LanguageProtocol.FILE_RECV_FAIL;
	}
		
	private void writeFile(InputStream inStream) throws IOException, DBManagerFatalException {
		Path outFile = FileUtil.tempDir.resolve(info.userGuid.toString()).resolve(info.fileGuid.toString()+".xz.enc");
		
		Files.deleteIfExists(outFile);
		
		FileUtil.getInstance().readStreamToStream(inStream, new FileOutputStream(outFile.toFile()));
		
		FileHandler.getInstance().organizeFile(outFile, info);
	}
}

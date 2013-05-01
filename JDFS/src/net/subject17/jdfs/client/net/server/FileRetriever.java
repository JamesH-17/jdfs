package net.subject17.jdfs.client.net.server;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import net.subject17.jdfs.client.file.FileUtil;
import net.subject17.jdfs.client.file.model.FileRetrieverInfo;
import net.subject17.jdfs.client.io.Printer;
import net.subject17.jdfs.client.net.LanguageProtocol;

public class FileRetriever {
	private final FileRetrieverInfo info;
	private final int port;
	private final String server;
	
	public FileRetriever(FileRetrieverInfo info, int port, String server) {
		this.info = info;
		this.port = port;
		this.server = server;
	}
	
	public String sendFile() {
		try (Socket sock = new Socket(server, port);
			OutputStream outStream = sock.getOutputStream()
		) {
			FileUtil.getInstance().readFileToStream(info.fileLocation, outStream);
			
			return LanguageProtocol.FILE_SEND_SUCC;
		}
		catch (IOException e) {
			Printer.logErr("Error encountered sending file "+info.fileLocation.getFileName()+" to host "+server+" on port "+port);
			Printer.logErr(e);
		}
		return LanguageProtocol.FILE_SEND_FAIL;
	}
}

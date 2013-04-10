package net.subject17.jdfs.client.net.sender;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import net.subject17.jdfs.client.io.Printer;
import net.subject17.jdfs.client.user.User;


/**
 * 
 * @author James
 *	This class is a direct extension of Talker, handling the details of any communications and requests.
 *	It currently is written to be single threaded, but in a manner such that multithreading it shouldn't be an issue
 */
public class TalkerResponder {

	private final BufferedReader input;
	private final PrintWriter output;
	private final String serverName;
	
	public TalkerResponder(String serverName, BufferedReader input, PrintWriter output){
		this.input = input;
		this.output = output;
		this.serverName = serverName;
	}
	
	public boolean HandleFileSender(Path fileToSend) throws IOException {		
		try {
			//TODO see if we can delete the following, or it's partner in connection handler
			//They're waiting on the filesize
			int fSize = (int) Files.size(fileToSend);
			output.write(fSize+"\n");
			Printer.log("fSize:"+fSize);
			
			int fPort = Integer.parseInt(input.readLine());
			Printer.log("Sending file on port "+fPort);
			
			FileSender fileSender = new FileSender(serverName, fPort, fileToSend,"");
			
			return fileSender.sendFile();
			
		} catch (IOException e){
			Printer.logErr("Failed to send file (client issue");
			Printer.logErr(e);
			
			return false;
		}
	}
	
	public boolean handleAccountQuery(User user){
		return false;
	}
}

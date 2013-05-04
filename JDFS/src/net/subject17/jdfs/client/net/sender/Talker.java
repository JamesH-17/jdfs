package net.subject17.jdfs.client.net.sender;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

import org.codehaus.jackson.map.ObjectMapper;

import net.subject17.jdfs.JDFSUtil;
import net.subject17.jdfs.client.file.handler.FileHandler;
import net.subject17.jdfs.client.file.model.FileRetrieverInfo;
import net.subject17.jdfs.client.io.Printer;
import net.subject17.jdfs.client.net.LanguageProtocol;
import net.subject17.jdfs.client.net.PortMgr;
import net.subject17.jdfs.client.net.PortMgrException;
import net.subject17.jdfs.client.net.model.MachineInfo;


public final class Talker implements Runnable {
	//DEV VARIABLES for easy access when connecting to the same machine
	private static String defaultServerName = "";
	private static int defaultPort = PortMgr.getServerPort(); //TODO Ability to change server port via command line, along with default server port
	
	protected Object payload;
	
	protected String serverName;
	protected int port;
	/**
	 * @category Constructor
	 * @param port -- The port Number that this service will listen on
	 * @param String
	 */
	public Talker() {this(defaultServerName); setPort(defaultPort); }
	/*
	public Talker(String serv) { setServer(serv); setPort(defaultPort); }
	public Talker(int targetPort) { setServer(defaultServerName);setPort(targetPort); }
	*/
	//For testing only, or maybe just to do a handshake
	public Talker(String serv){
		setServer(serv);
		setPort(defaultPort);
		payload = null;
	}
	public Talker(String targetServer, Object opPayload){
		setServer(targetServer);
		setPort(defaultPort);
		
		this.payload = opPayload;		
	}
	
	@Override
	public void run() {
		createTalker();
	}
	
	public void setServer(String s) {serverName=s;}
	public void setPort(int p) {port=p;}
	public int getPort() {return port;}
	public String getServer() {return serverName;}
	
	public void createTalker() {
		try (
			Scanner userInput = new Scanner(System.in);
			Socket sock = new Socket(serverName, port); 
			PrintWriter output = new PrintWriter(sock.getOutputStream(), true);
			BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
		){
			//TalkerResponder responder = new TalkerResponder(serverName, in, output);
			
			Printer.log("Connected to server "+sock.getInetAddress());
			
			String serverMessage = "";
			//output.println(LanguageProtocol.SYN);
			
			if (jdfsRunningOnServer(output, in)) {
				
				int attempts = 0;
				do {
					sendMachineInfo(output, in);
					serverMessage = in.readLine();
				} while (
							( null == serverMessage || 
								!(
									serverMessage.equals(LanguageProtocol.CONFIRM_ADD_ACCOUNT) || 
									serverMessage.equals(LanguageProtocol.QUERY_ACCOUNT_EXISTS)
								)
							) &&
							attempts++ < 3
				);
				
				
				if (LanguageProtocol.keepGoing(serverMessage)){
					
					if (payload instanceof FileSender) {
						sendFile(output, in);
					}
					else if (payload instanceof FileRetriever){
						retrieveFile(output, in);
					} //TODO: Add peer request
					
					output.println(LanguageProtocol.CLOSE);
				}
				
				
				/*
				String msg = "", recieved = "";
				do {
					
					
					//////////////////////////TODO
					//Drop all this logic.
					//Instead, We're also gonna pass in an object to create the talker
					//We'll tell the desired operation from the class type
					//Then, we'll call a commonly named method from that class
					
					
					if (!msg.equals(LanguageProtocol.INIT_FILE_TRANS)) {
						msg = userInput.next();
						Printer.log("Message to post:"+msg);
						
						if (msg != null && !msg.equals("")) {
							
							if(msg.contains("send-file")) {
								msg=LanguageProtocol.INIT_FILE_TRANS;
							}
							
							output.println(msg); //TODO do this in a much cleaner, more modular way
	
							Printer.log("Messsage from server:"+recieved);
							recieved = in.readLine();
							Printer.log("Messsage from server:"+recieved);
						}
					}
					else {
						Printer.log("Calling handle");
						//responder.HandleFileSender(path); //TODO figure out how to get the path as input
						msg = "asd";
					}
				} while(!(null == msg || msg.equals("") || msg.equals("exit") || msg.equals(LanguageProtocol.CLOSE)));
				*/
			}
			
		} catch(IOException e) {
			Printer.logErr("Could not listen on port "+port);
			Printer.logErr(e);

		} catch (Exception e) {
			Printer.logErr(e);
		}
	}
	
	private void sendMachineInfo(PrintWriter output, BufferedReader in) {
		
		MachineInfo info = new MachineInfo();
		try {
			output.println(info.toJSON());
		} catch (IOException e) {
			Printer.logErr(e);
			output.println(LanguageProtocol.SKIPPED);
		}
	}
	
	private void retrieveFile(PrintWriter output, BufferedReader in) throws IOException, PortMgrException {
		FileRetriever fileRetriever = (FileRetriever) payload;
		
		output.println(LanguageProtocol.INIT_FILE_RETRIEVE);
		String response = in.readLine();
		
		if (null != response && response.equals(LanguageProtocol.ACK)) { 
			String jsonOut = JDFSUtil.toJSON(fileRetriever.criteria);
			
			Printer.log("JSON of criteria:"+jsonOut);
			output.println( jsonOut );
			
			response = in.readLine();
			Printer.log("Got response "+response);
			
			if (null != response && response.equals(LanguageProtocol.ACCEPT_FILE_TRANS)) {
				output.println(LanguageProtocol.ACK);
				
				String json = in.readLine();
				ObjectMapper mapper = new ObjectMapper();
				FileRetrieverInfo incomingInfo = mapper.convertValue(json, FileRetrieverInfo.class);
				
				Printer.log("Got json:"+json);
				
				fileRetriever.setPort(PortMgr.getNextAvailablePort());
				
				Thread thread = new Thread(fileRetriever);
				thread.run();
				
				output.println(fileRetriever.port);
				
				/*
				response = in.readLine();
				if (null != response && response.equals(LanguageProtocol.ACK)) {
					Thread thread = new Thread(fileRetriever);
					thread.run();
				}*/
				
				FileHandler.getInstance().manageRecievedFile(fileRetriever.storeLocation, incomingInfo);
			}
		}
	}
	
	private void sendFile(PrintWriter output, BufferedReader in) throws IOException, NumberFormatException {
		
		FileSender fileSender = (FileSender) payload;
		
		output.println(LanguageProtocol.INIT_FILE_TRANS);
		
		String response = in.readLine();
		if (null != response && response.equals(LanguageProtocol.ACK)) { 
			output.println(fileSender.jsonPayload);
			response = in.readLine();
			
			if (null != response && response.equals(LanguageProtocol.ACCEPT_FILE_TRANS)) {
				output.println(LanguageProtocol.ACK);
				
				fileSender.port = this.port;
				fileSender.ipAddr = this.serverName;
				
				fileSender.port = Integer.parseInt(in.readLine());
				
				Thread thread = new Thread(fileSender);
				thread.run();
			}
		}
	}
	private boolean jdfsRunningOnServer(PrintWriter output, BufferedReader input) throws IOException {
		for (int attempt = 0; attempt < 3; ++attempt) {
			
			output.println(LanguageProtocol.SYN);
			
			if (input.readLine().equals(LanguageProtocol.ACK)) {
				return true;
			}
		}
		return false;
	}
	
}

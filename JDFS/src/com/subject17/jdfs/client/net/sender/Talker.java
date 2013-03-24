package com.subject17.jdfs.client.net.sender;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

import com.subject17.jdfs.client.io.Printer;
import com.subject17.jdfs.client.net.LanguageProtocol;

public final class Talker {
	private String defaultServerName = "";
	private int defaultPort = 0;
	
	protected String serverName;
	protected int port;
	/**
	 * @category Constructor
	 * @param port -- The port Number that this service will listen on
	 * @param String
	 */
	public Talker() {setServer(defaultServerName); setPort(defaultPort); }
	public Talker(String serv) { setServer(serv); setPort(defaultPort); }
	public Talker(int targetPort) { setServer(defaultServerName);setPort(targetPort); }
	
	public Talker(String serv, int p){
		setServer(serv);
		setPort(p);
	}
	
	public void run(){}; //TODO Implement this bitch since it's definitely gonna need to be threaded.
						//Mayhaps make multiple constructors that'll do different, predetermined things?
						//Or have it take in a peer as a paramater, assigning a talker to each peer?
						//Or make it a super class that's abstract, implementing different functionality for each possible type?
						//Or make it 
	
	public void setServer(String s) {serverName=s;}
	public void setPort(int p) {port=p;}
	public int getPort() {return port;}
	public String getServer() {return serverName;}
	
	public void createTalker() {
		//TODO switch from user input to the config file
		
		//BufferedReader userInput2 = new BufferedReader(new InputStreamReader(System.in));
		
		try (
			Scanner userInput = new Scanner(System.in);
			Socket sock = new Socket(serverName, port); 
			PrintWriter output = new PrintWriter(sock.getOutputStream(), true);
			BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
		){
			TalkerResponder responder = new TalkerResponder(serverName, in, output);
			
			Printer.log("Connected to server "+sock.getInetAddress());
			Printer.log("IsInputShutDown"+sock.isInputShutdown());
			Printer.log("IsOutputShutDown"+sock.isOutputShutdown());
			
			//output.print
			Printer.log("begin output");
			output.println(LanguageProtocol.SYN);
			Printer.log("output done");
			
			String msg="", recieved="";
			do {
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
					responder.HandleFileSender(path); //TODO figure out how to get the path as input
					msg = "asd";
				}
			}
			while(msg != null && !msg.equals("") && !msg.equals("exit"));

		} catch(IOException e){
			Printer.logErr("Could not listen on port "+port);
			Printer.logErr("Reason:"+e.getMessage());

		} catch (Exception e) {
			Printer.logErr(e.getMessage());
		}
	}
	
	private void handleFileInput(BufferedReader in, PrintWriter out) throws IOException {
		Scanner temp = new Scanner(System.in);
		
		Printer.println("Enter the path of a file to send, or enter . to send the default");
		String msg = temp.next();
		
		temp.close();
		
		Path pathToUse = Paths.get(System.getProperty("user.dir"),"test.txt");
		
		if (!msg.trim().equals("."))
			pathToUse = Paths.get(msg);
		Printer.log("using file "+pathToUse.toString());
		
		int fSize = (int) Files.size(pathToUse);
		out.write(fSize+"\n");
		Printer.log("fSize:"+fSize);
		
		
		int fPort = Integer.parseInt(in.readLine());
		Printer.log("Sending file on port "+fPort);
		
		FileSender f = new FileSender(serverName, fPort, pathToUse);
		f.sendFile();
	}
}

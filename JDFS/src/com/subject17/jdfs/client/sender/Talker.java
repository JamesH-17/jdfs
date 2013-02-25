package com.subject17.jdfs.client.sender;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import com.subject17.jdfs.client.io.Printer;
import com.subject17.jdfs.client.net.LanguageProtocol;

public class Talker {
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
	
	
	public void setServer(String s) {serverName=s;}
	public void setPort(int p) {port=p;}
	public int getPort() {return port;}
	public String getServer() {return serverName;}
	
	public void createTalker() {
		BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
		try {
			System.out.println("Please enter a servername:");
			String serverName = userInput.readLine();
			Socket sock = new Socket(serverName, port);
			Printer.log("Connected to server "+sock.getInetAddress());
			Printer.log("IsInputShutDown"+sock.isInputShutdown());
			Printer.log("IsOutputShutDown"+sock.isOutputShutdown());
			PrintWriter output = new PrintWriter(sock.getOutputStream(), true);
			BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			
			//output.print
			Printer.log("begin output");
			output.println(LanguageProtocol.SYN);
			Printer.log("output done");
			//if (in.readLine().equals(LanguageProtocol.ACK)) {
			//	
				String msg;
				do {
					msg = userInput.readLine();
					output.println(msg);
				}
				while(msg!=null && msg.equals("") && !msg.equals("exit"));
			//}
			output.close();
			in.close();
			userInput.close();
			sock.close();
		} catch(IOException e){
			System.out.println("Could not listen on port "+port);
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
		}
		finally {
		}
	}
}

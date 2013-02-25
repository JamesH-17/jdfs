package com.subject17.jdfs.client.net.sender;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Talker {
	private static String defaultServerName = "";
	private static int defaultPort = 0;
	
	protected String serverName;
	protected int port;
	
	/**
	 * @category Constructor
	 * @param port -- The port Number that this service will listen on
	 * @param String
	 */
	public Talker() { this(defaultServerName,defaultPort); }
	public Talker(String serv) { this(serv,defaultPort); }
	public Talker(int targetPort) { this(defaultServerName,targetPort); }
	
	public Talker(String serv, int p){
		setServer(serv);
		setPort(p);
	}
	
	
	public void setServer(String s) {serverName=s;}
	public void setPort(int p) {port=p;}
	public int getPort() {return port;}
	public String getServer() {return serverName;}
	
	public static void createTalker(int port) {
		BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
		try {
			System.out.println("Please enter a servername:");
			String serverName = userInput.readLine();
			Socket sock = new Socket(serverName, port);
			
			PrintWriter output = new PrintWriter(sock.getOutputStream(), true);
			BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			
			String serverMsg;
			while (!((serverMsg = in.readLine()) == null ? "" : serverMsg).equals("exit")) {
				System.out.println(serverMsg);
				
				output.println(userInput.readLine());
			}

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
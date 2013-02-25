package com.subject17.jdfs.client.reciever;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Listener {
	private static int defaultPort = 0;
	
	protected static int port;
	
	/**
	 * @category Constructor
	 * @param port -- The port Number that this service will listen on
	 * @param String
	 */
	public Listener() { this(defaultPort); }
	public Listener(int targetPort){
		setPort(targetPort);
	}

	public void setPort(int newPort) {port=newPort;}
	public int getPort() {return port;}
	
	public static void createListener() {

		BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
		try {
			ServerSocket socket = new ServerSocket(port);
			Socket sock = socket.accept();
			PrintWriter output = new PrintWriter(sock.getOutputStream(), true);
			BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			
			String serverMsg;
			System.out.println("Connected to client ");
			output.println("Welcome to mah server!");
			
			do {
				serverMsg = in.readLine();
				if (serverMsg != null)
					System.out.println(serverMsg);
				
				output.println(userInput.readLine());
			} while(!serverMsg.equals("exit"));
			

			output.close();
			in.close();
			userInput.close();
			sock.close();
			socket.close();
		} catch(IOException e){
			System.out.println("IO exception: "+e.getMessage());
			System.out.println("Port in use:"+port);
		}
		catch (Exception e) {
			System.out.println("Exception: "+e.getMessage());
			System.out.println("Port in use:"+port);
		}
		finally {
		}
	}
}
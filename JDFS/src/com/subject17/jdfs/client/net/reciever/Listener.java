package com.subject17.jdfs.client.net.reciever;

import java.io.IOException;
import java.net.ServerSocket;
import com.subject17.jdfs.client.io.Printer;

public class Listener {
	private static int defaultPort = 2718;
	
	protected int port;
	
	/**
	 * @category Constructor
	 * Creates a new listener using {@defaultPort}
	 */
	public Listener() { this(defaultPort); }
	/**
	 * @category Constructor
	 * @param port -- The port Number that this service will listen on
	 */
	public Listener(int targetPort){
		setPort(targetPort);
	}

	public void setPort(int newPort) {port = newPort;}
	public int getPort() {return port;}
	
	public void createListener() throws IOException {
		try (ServerSocket servSock = new ServerSocket(port)){
			Printer.log("Listener started");
			ListenConnectionHandler connHandler;
			while(true) {
				try {
					connHandler = new ListenConnectionHandler(servSock.accept());
					Printer.log("New connection!");
					Thread t = new Thread(connHandler);
					t.start();
				} catch(Exception e) {
					Printer.logErr("Exception encountered for a connecting client");
					Printer.logErr("Port in use:"+port);
					Printer.logErr(e.getMessage());
				}
			}
		} catch(IOException e) {
			Printer.log("IO exception: "+e.getMessage());
			Printer.log("Port in use:"+port);
		}
		catch (Exception e) {
			Printer.log("Exception: "+e.getMessage());
			Printer.log("Port in use:"+port);
		}
	}
}

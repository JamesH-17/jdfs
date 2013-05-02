package net.subject17.jdfs.client.net.server;

import java.io.IOException;
import java.net.ServerSocket;

import net.subject17.jdfs.client.io.Printer;
import net.subject17.jdfs.client.net.PortMgr;

public final class Listener implements Runnable {
	private final static int defaultPort = PortMgr.getServerPort();
	
	protected int port;

	private boolean run;
	
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

	@Override
	public void run() {
		try {
			createListener();
		}
		catch (IOException e) {
			Printer.logErr("IOException in Listener:  Probable networking issue");
			Printer.logErr(e);
		} finally {
			Printer.log("Listener shut down!");
		}
	}
	public void setPort(int newPort) {port = newPort;}
	public int getPort() {return port;}
	
	
	
	public void createListener() throws IOException {
		try (ServerSocket servSock = new ServerSocket(port)){
			run = true;
			ListenConnectionHandler connHandler;
			
			Printer.log("Listener started");
			
			while(run) {
				try { //TODO in listen connection handler, 
					connHandler = new ListenConnectionHandler(servSock.accept());
					Printer.log("New connection!");
					
					Thread t = new Thread(connHandler);
					t.start();
					
				} catch(Exception e) {
					Printer.logErr("Exception encountered for a connecting client");
					Printer.logErr("Port in use:"+port);
					Printer.logErr(e);
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
	
	public void stopListener(){
		run = false;
	}
}

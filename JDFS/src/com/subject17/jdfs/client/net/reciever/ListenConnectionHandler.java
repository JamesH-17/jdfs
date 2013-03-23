package com.subject17.jdfs.client.net.reciever;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

import com.subject17.jdfs.client.io.Printer;
import com.subject17.jdfs.client.net.LanguageProtocol;
import com.subject17.jdfs.client.net.PortMgr;
import com.subject17.jdfs.client.net.reciever.FileReciever;
import com.subject17.jdfs.client.peers.PeersHandler;

public class ListenConnectionHandler implements Runnable {
	final protected Socket handlingSock; //<3 final, much more useful than const in many situations
	
	public ListenConnectionHandler(Socket accept) {
		handlingSock = accept;
	}
	
	public void run(){
		try {			
			handleSocket();
			handlingSock.close();
		} catch(IOException e) {
			Printer.logErr(e.getMessage());
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * This function initializes the input/output streams for the client
	 * @throws IOException
	 */
	private void handleSocket() throws IOException, Exception {
		try (
				PrintWriter toClient = new PrintWriter(handlingSock.getOutputStream(), true);
				BufferedReader fromClient = new BufferedReader(new InputStreamReader(handlingSock.getInputStream()))
		)
		{
			Printer.log("Connected to client,"+handlingSock.getInetAddress()+" awaiting SYN");
			
			if (handleInitialConnection(fromClient, toClient)) {
				handleConnection(fromClient,toClient);
			}
			
			Printer.log("Closed connection to client "+handlingSock.getInetAddress());
		} catch (IOException e) {
			Printer.logErr("Exception encountered with client "+handlingSock.getInetAddress()+" on port "+handlingSock.getPort());
			throw e;
		}
	}
	
	public boolean handleInitialConnection(BufferedReader fromClient, PrintWriter toClient) throws IOException {
		String clientResponse = "", serverResponse = "";
		Printer.log("here");
		
		for (int attempt = 0; attempt < 3 ; ++attempt) {

			clientResponse = fromClient.readLine();
			Printer.log("Client Says:"+clientResponse);

			toClient.println(serverResponse = LanguageProtocol.handleResponse(clientResponse));
			
			Printer.log("Responded with:"+serverResponse);
			
			if (serverResponse.equals(LanguageProtocol.ACK)) {
				Printer.log("Initial connection established!");
				return true;
			}
		}
		return false;
	}
	
	public void handleConnection(BufferedReader fromClient, PrintWriter output) throws Exception{
		PeersHandler.addIncomingPeer(handlingSock.getInetAddress(), handlingSock.getPort());
		String incomingMessage=null, serverMessage="";

		do {
			incomingMessage = fromClient.readLine();
			if (incomingMessage!=null) {
				Printer.log("Message from Client:"+incomingMessage);
				
				serverMessage = handleClientResponse(incomingMessage);
				
				Printer.log("Responding with:"+serverMessage);
				output.println(serverMessage);
			} else incomingMessage = "";
		} while(!(incomingMessage.equals(LanguageProtocol.CLOSE)));
	}
	
	public String handleClientResponse(String resp) throws Exception {
		if (resp == null)
			return "";
		switch(resp) {
			case LanguageProtocol.INIT_FILE_TRANS: return handleFileTrans();
			default: return LanguageProtocol.UNKNOWN;
		}
	}
	
	private String handleFileTrans() throws Exception{
		
		int Port = PortMgr.getRandomPort();
		Printer.log("Using port "+Port);
		
		Printer.log("Starting new file reciever");
		Thread t = new Thread(new FileReciever(Port));
		t.start();
		
		Printer.log("Returning");
		return Port + "";
	}
	
	protected void finalize() {
		try {handlingSock.close();}
		catch(IOException e) {
			Printer.logErr("Error closing incoming socket connection:"+e.getMessage());
			e.printStackTrace();
		}
	}
}

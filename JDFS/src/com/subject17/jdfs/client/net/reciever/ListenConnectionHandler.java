package com.subject17.jdfs.client.net.reciever;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import com.subject17.jdfs.client.io.Printer;
import com.subject17.jdfs.client.net.LanguageProtocol;
import com.subject17.jdfs.client.settings.PeersHandler;

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
		}
	}
	
	/**
	 * This function intializes the input/output streams for the client
	 * @throws IOException
	 */
	private void handleSocket() throws IOException {
		try (
				PrintWriter toClient = new PrintWriter(handlingSock.getOutputStream(), true);
				BufferedReader fromClient = new BufferedReader(new InputStreamReader(handlingSock.getInputStream()))
		)
		{
			Printer.log("Connected to client,"+handlingSock.getInetAddress()+" awaiting SYN");
			
			if (handleInitialConnection(fromClient, toClient)) {
				handleInitialConnection(fromClient,toClient);
			}
			
			Printer.log("Closed connection to client "+handlingSock.getInetAddress());
		}
	}
	
	public boolean handleInitialConnection(BufferedReader fromClient, PrintWriter toClient) throws IOException {
		String clientResponse = "", serverResponse = "";
		for (int attempt=0; attempt < 3 ; ++attempt) {
			clientResponse = fromClient.readLine();
			toClient.print(
				serverResponse = LanguageProtocol.handleResponse(clientResponse)
			);
			
			Printer.log("Client Says:"+clientResponse);
			Printer.log("Responded with:"+serverResponse);
			
			if (serverResponse.equals(LanguageProtocol.ACK))
				return true;
		}
		return false;
	}
	
	public void handleConnection(BufferedReader fromClient, PrintWriter output) throws IOException{
		PeersHandler.addIncomingPeer(handlingSock.getInetAddress(), handlingSock.getPort());
		String incomingMessage=null, serverMessage="";

		do {
			incomingMessage = fromClient.readLine();
			Printer.log("Message from Client:"+incomingMessage);
			
			serverMessage=handleClientResponse(incomingMessage);
			
			output.println(serverMessage);
		} while(!incomingMessage.equals(LanguageProtocol.CLOSE));
	}
	
	public String handleClientResponse(String resp) {
		switch(resp) {
			default: return LanguageProtocol.UNKNOWN;
		}
	}
	
	
	
	
	
	
	
	protected void finalize() {
		try {handlingSock.close();}
		catch(IOException e) {
			Printer.logErr("Error closing incoming socket connection:"+e.getMessage());
			e.printStackTrace();
		}
	}
}
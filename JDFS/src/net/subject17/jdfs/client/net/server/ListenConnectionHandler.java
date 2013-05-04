package net.subject17.jdfs.client.net.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.SQLException;

import net.subject17.jdfs.client.file.db.DBManager.DBManagerFatalException;
import net.subject17.jdfs.client.file.handler.FileHandler;
import net.subject17.jdfs.client.file.model.FileRetrieverInfo;
import net.subject17.jdfs.client.file.model.FileRetrieverRequest;
import net.subject17.jdfs.client.io.Printer;
import net.subject17.jdfs.client.net.LanguageProtocol;
import net.subject17.jdfs.client.net.PortMgr;
import net.subject17.jdfs.client.net.PortMgrException;
import net.subject17.jdfs.client.peers.PeersHandler;

import org.codehaus.jackson.annotate.JsonMethod;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;


public final class ListenConnectionHandler implements Runnable {
	final protected Socket handlingSock; //<3 final, much more useful than const in many situations
	
	private final static int MAX_CONNECTION_ATTEMPTS = 3;
	
	private PrintWriter toClient;
	private BufferedReader fromClient;
	
	public ListenConnectionHandler(Socket accept) {
		handlingSock = accept;
	}
	
	@Override
	public void run() {
		try {			
			handleSocket();
			handlingSock.close();
		}
		catch(IOException e) {
			Printer.logErr("An error occured in setting up the socket to "+handlingSock.getInetAddress().getHostAddress());
			Printer.logErr(e);
		}
		catch (DBManagerFatalException e) {
			//TODO find a way to signal main
			Printer.logErr(e);
		}
		catch (Exception e) {
			Printer.logErr(e);
		}
	}
	
	/**
	 * This function initializes the input/output streams for the client, then proceeds to the next step in the handshake process
	 * @throws IOException
	 * @throws DBManagerFatalException 
	 * @throws SQLException 
	 */
	private void handleSocket() throws IOException, DBManagerFatalException, SQLException {
		try {
			
			toClient = new PrintWriter(handlingSock.getOutputStream(), true);
			fromClient = new BufferedReader(new InputStreamReader(handlingSock.getInputStream()));
			
			Printer.log("Connected to client,"+handlingSock.getInetAddress()+" awaiting SYN");
			
			if (handleInitialConnection(fromClient, toClient))
				handleConnection(fromClient,toClient);
			else
				Printer.log("Client does not appear to be another jdfs program");
			
			Printer.log("Closed connection to client "+handlingSock.getInetAddress());
			
		} catch (IOException e) {			
			Printer.logErr("Exception encountered with client "+handlingSock.getInetAddress()+" on port "+handlingSock.getPort());
		}
		finally {
			cleanUpStreams();
		}
	}
	
	private void cleanUpStreams() throws IOException {
		if (toClient != null)
			toClient.close();
		if (fromClient != null)
			fromClient.close();
	}

	public boolean handleInitialConnection(BufferedReader fromClient, PrintWriter toClient) throws IOException {
		String clientResponse = "", serverResponse = "";
		
		for (int attempt = 0; attempt < MAX_CONNECTION_ATTEMPTS ; ++attempt) { //Give them a few tries to send the correct signal

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
	
	public void handleConnection(BufferedReader fromClient, PrintWriter output) throws IOException, DBManagerFatalException, SQLException {
		//PeersHandler.addIncomingPeer(handlingSock.getInetAddress(), handlingSock.getPort());
		String incomingMessage=null, serverMessage="";
		
		try {
			PeersHandler.addIncomingMachine(handlingSock.getInetAddress(), fromClient.readLine());
			output.println(LanguageProtocol.CONFIRM_ADD_ACCOUNT);
		}
		catch (DBManagerFatalException | SQLException e) {
			output.println(LanguageProtocol.DENY_ADD_ACCOUNT);
			output.println(LanguageProtocol.CLOSE);
			throw e;
		}
		
		do {
			incomingMessage = fromClient.readLine();
			if (incomingMessage != null) {
				Printer.log("Message from Client:"+incomingMessage, Printer.Level.VeryLow);
				
				serverMessage = handleClientResponse(incomingMessage);
				
				Printer.log("Responding with:"+serverMessage, Printer.Level.VeryLow);
				output.println(serverMessage);
			} //else incomingMessage = "";
		} while(!(null == incomingMessage || incomingMessage.equals("") || incomingMessage.equals(LanguageProtocol.CLOSE)));
	}
	
	public String handleClientResponse(String resp) throws DBManagerFatalException {
		if (resp == null)
			return "";
		switch(resp) {
			case LanguageProtocol.INIT_FILE_TRANS: return handleFileTrans(); //returns success/failure string
			case LanguageProtocol.INIT_FILE_RETRIEVE: return handleFileSend();
			default: return LanguageProtocol.UNKNOWN;
		}
	}
	
	private String handleFileSend() throws DBManagerFatalException {
		try {
			//Acknowledge, wait on receiving file info
			toClient.println(LanguageProtocol.ACK);
			Printer.log("Responding with "+LanguageProtocol.ACK);
			
			
			String json = fromClient.readLine();
			Printer.print("Handling file retrieval, got json "+json);
			
			for (int attempt = 0; attempt < 3 && (null == json || json.equals("")); ++attempt ) {
				toClient.println(LanguageProtocol.UNKNOWN);
				json = fromClient.readLine();
				Printer.print("Handling file retrieval, got json "+json);
			}
			
			if (null == json || json.equals("")) {
				Printer.log("Failed to get good json from client");
				toClient.println(LanguageProtocol.UNKNOWN);
			} else {


				ObjectMapper mapper = new ObjectMapper().setVisibility(JsonMethod.FIELD, Visibility.ANY);
				mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
				//See if we have the file, if so, send it
				FileRetrieverRequest criteria = mapper.readValue(json, FileRetrieverRequest.class);
				FileRetrieverInfo info = FileHandler.getInstance().getFileStoredOnMachine(criteria);
				if (null != info) {
				
					//TODO finish this conditional
					Printer.log("Starting new file sender");
					toClient.println(LanguageProtocol.ACCEPT_FILE_TRANS);
					
					String clientMsg = fromClient.readLine();
					
					if (clientMsg.equals(LanguageProtocol.ACK)) {
						//toClient.println((new NewHandlerInfo(Port)).toJSON());
						toClient.println(info.toJSON());
						
						int targetPort = Integer.parseInt(fromClient.readLine());
						toClient.println(LanguageProtocol.ACK);
						
						FileRetriever sender = new FileRetriever(info, targetPort, handlingSock.getInetAddress().getHostAddress());
						sender.sendFile();
						
						return LanguageProtocol.FILE_SEND_SUCC;
					}
					else {
						Printer.logErr("Didn't equal ack");
					}
				}
				else {
					Printer.logErr("info was nulll");
				}
			}
			
		}
		catch (IOException e) {
			Printer.logErr(e);
		}
		return LanguageProtocol.FILE_SEND_FAIL;
	}

	private String handleFileTrans() {
		try {
			
			//Set up a new port to grab their file on so we don't block this one
			int Port = PortMgr.getRandomPort();
			Printer.log("Using port "+Port);

			//Acknowledge, wait on receiving file info
			toClient.println(LanguageProtocol.ACK);
			
			String json = fromClient.readLine();
			
			for (int attempt = 0; attempt < 3 && (null == json || json.equals("")); ++attempt ) {
				toClient.println(LanguageProtocol.UNKNOWN);
				json = fromClient.readLine();
			}
			
			if (null == json || json.equals("")) {
				toClient.println(LanguageProtocol.UNKNOWN);
			} else {
			
				Printer.log("Starting new file reciever");
				FileReciever reciever = new FileReciever(Port, json);
				
				if (FileHandler.getInstance().canStoreFile(reciever.info)) {
					toClient.println(LanguageProtocol.ACCEPT_FILE_TRANS);
					
					String clientMsg = fromClient.readLine();
					
					if (clientMsg.equals(LanguageProtocol.ACK)) {
						//toClient.println((new NewHandlerInfo(Port)).toJSON());
						toClient.println(Port);
						reciever.run();
						
						return LanguageProtocol.FILE_RECV_SUCC;
					}
					
				}
			}
		} catch (PortMgrException | IOException e) {
			Printer.logErr(e);
		}
		return LanguageProtocol.FILE_RECV_FAIL;
	}
	
	protected void finalize() {
		try {
			cleanUpStreams();
			handlingSock.close();
		}
		catch(IOException e) {
			Printer.logErr("Error closing incoming socket connection:"+e.getMessage());
			e.printStackTrace();
		}
	}
}

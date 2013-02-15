/**
 * 
 */
package com.subject17.jdfs.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

/**
 * @author James Hughes
 *
 */
public class UserNode {
	static BufferedReader input;
	/**
	 * @param args 
	 */
	public static void main(String[] args) {
		input = new BufferedReader(new InputStreamReader(System.in));
		// TODO Auto-generated method stub
		final int port = 27312;
		
		
		createServerListener(port);
	}
	
	public static void createClientListener(int port) {
		try {
			System.out.println("Please enter a servername:");
			String serverName = input.readLine();
			Socket sock = new Socket(serverName, port);
			
			PrintWriter output = new PrintWriter(sock.getOutputStream(), true);
			BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			
			String serverMsg;
			while (!((serverMsg = in.readLine()) == null ? "" : serverMsg).equals("exit")) {
				System.out.println(serverMsg);
				
				output.println(input.readLine());
			}

			output.close();
			in.close();
			input.close();
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
	
	
	public static void createServerListener(int port) {
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
				if (serverMsg!=null)
					System.out.println(serverMsg);
				
				output.println(input.readLine());
			} while(!serverMsg.equals("exit"));
			

			output.close();
			in.close();
			input.close();
			sock.close();
			socket.close();
		} catch(IOException e){
			System.out.println("Server Could not listen on port "+port);
			System.out.println("ReasON: "+e.getMessage());
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
		}
		finally {
		}
	}

}

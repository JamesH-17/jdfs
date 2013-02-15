/**
 * 
 */
package com.subject17.jdfs.client;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

/**
 * @author James Hughes
 *
 */
public class UserNode {

	/**
	 * @param args 
	 */
	public static void main(String[] args) {
		Scanner input = new Scanner(System.in);
		// TODO Auto-generated method stub
		final int port = 27312;
		System.out.println("Please enter a servername:");
		String serverName = input.next();
		
		try {
			Socket sock = new Socket(serverName, port);
			
			PrintWriter output = new PrintWriter(sock.getOutputStream(), true);
			BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			
			String serverMsg;
			while (!((serverMsg = in.readLine()) == null ? "" : serverMsg).equals("exit")) {
				System.out.println(serverMsg);
				
				output.println(input.nextLine());
			}
			
		} catch(IOException e){
			System.out.println("Could not listen on port "+port);
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
		}
		finally {
			output.close();
			in.close();
			input.close();
			sock.close();
		}
	}

}

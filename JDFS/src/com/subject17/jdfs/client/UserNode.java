/**
 * 
 */
package com.subject17.jdfs.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Scanner;

import com.subject17.jdfs.client.io.Printer;
import com.subject17.jdfs.client.reciever.Listener;
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
		Scanner inScan = new Scanner(System.in);
		// TODO Auto-generated method stub
		Printer.println("Do you wish to start a server or client?");
		Printer.println("1) Server");
		Printer.println("2) Client");
		switch(inScan.next().toLowerCase().substring(0,1)) {
			case "s": case "1": dispatchClient(); break;
			case "c": case "2": dispatchServer(); break;
		}
		
	}
	
	private static void dispatchServer() {
		int port = (int)(Math.random()*Math.pow(2, 15));
		Listener serv = new Listener();
	}

	public static void dispatchClient() {
		
	}
}

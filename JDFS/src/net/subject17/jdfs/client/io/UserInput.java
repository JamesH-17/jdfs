package net.subject17.jdfs.client.io;

import java.util.Scanner;

public class UserInput {
	
	private Scanner input;
	
	private static UserInput _instance = null;
	private UserInput(){
		input = new Scanner(System.in);
	}
	public static UserInput getInstance(){
		if (null == _instance) {
			synchronized(UserInput.class) {
				if (null == _instance){
					_instance = new UserInput();
				}
			}
		}
		return _instance;
	}
	
	private Object getInput(){
		return input.next();
	}
	
	public String getNextString(String messageToDisplay){
		Printer.println("Please enter a password for encryption/decryption of files:");
		return input.next();
	}
}

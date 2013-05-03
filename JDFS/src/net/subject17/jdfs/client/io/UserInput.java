package net.subject17.jdfs.client.io;

import java.util.Scanner;

public class UserInput {
	
	private Scanner input;
	private static Thread guiThread;
	private static UserGUI gui;
	
	private static String nextInput = null;
	
	private static UserInput _instance = null;
	private UserInput(){
		input = new Scanner(System.in);
		nextInput = null;
		setUpGui();
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
		Printer.println(messageToDisplay);
		while (null == nextInput){
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {}
		}
		String temp = new String(nextInput);
		nextInput = null;
		return temp;
	}
	

	
	private static void setUpGui(){
		gui = new UserGUI();
		guiThread = new Thread(gui);
		guiThread.start();
	}
	public String returnInput(String inputString) {
		return inputString;
	}
	public void setInput(String inputString) {
		nextInput = inputString;
	}
	public static void closeGUI() {
		try {
			gui.cleanup();
			guiThread.interrupt();
		} catch(Exception e) {
			
		}
	}
}

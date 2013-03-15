package com.subject17.jdfs.client.settings.reader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.subject17.jdfs.client.file.handler.FileUtils;
import com.subject17.jdfs.client.io.Printer;
import com.subject17.jdfs.client.user.User;

public class UserSettingsReader extends SettingsReader {
	private File usersFile;
	
	private Document usersDoc;
	
	private User activeUser;
	private ArrayList<User> users;
	
	public UserSettingsReader(String pathloc, String fname) throws Exception {
		usersFile = new File(pathloc, fname);
		Init();
	}
	
	public UserSettingsReader(File src) throws Exception {
		usersFile = src;
		Init();
	}
	
	private void Init() throws Exception {
		FileUtils.checkIfFileReadable(usersFile);
		usersDoc = getUsersDocument();
		readUsers();
	}
	
	//Getters
	public ArrayList<User> getUsers() { return users; }
	public User getActiveUser() { return activeUser; }
	
	//
	private void readUsers() {
		users = new ArrayList<User>();
		activeUser = null;
		
		NodeList lst = usersDoc.getElementsByTagName("user");
		for (int i = 0; i < lst.getLength(); ++i) {
			try {
				Element usr = (Element)lst.item(i);
				User user = new User(usr);
				users.add(user);
				if (isActive(usr))
					activeUser = user;
			}
			catch (Exception e){
				Printer.logErr("Could not read user, number "+i+" in list.");
				Printer.logErr(e);
			}
		}
	}
	
	//Utilities
	private Document getUsersDocument() throws ParserConfigurationException, SAXException, IOException {
		return GetDocument(usersFile);
	}
	
	private static boolean isActive(Element e) { //Where should this go!
		String activeness = e.getAttribute("active");
		return //activeness != null && 
			(activeness.equals("true")||activeness.equals("active"));
	}
}

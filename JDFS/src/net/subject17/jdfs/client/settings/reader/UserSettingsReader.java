package net.subject17.jdfs.client.settings.reader;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;

import net.subject17.jdfs.client.io.Printer;
import net.subject17.jdfs.client.settings.writer.UserSettingsWriter;
import net.subject17.jdfs.client.user.User;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


public class UserSettingsReader extends SettingsReader {
	private Document usersDoc;
	
	private User activeUser;
	private ArrayList<User> users = new ArrayList<User>();
	
	public UserSettingsReader(String pathloc, String fname) throws FileNotFoundException, IOException, SAXException, ParserConfigurationException {
		userSettingsPath = Paths.get(pathloc, fname);
		Init();
	}
	
	public UserSettingsReader(Path src) throws IOException, SAXException, ParserConfigurationException {
		userSettingsPath = src;
		try {
			Init();
		} catch(FileNotFoundException e){
			Printer.logErr("File not found -- "+userSettingsPath.toString());
			Printer.logErr(e);
			Printer.logErr("Attempting to create a default user settings file");
			
			UserSettingsWriter writer = new UserSettingsWriter();
			writer.writeUserSettings(new ArrayList<User>());
		}
	}
	
	private void Init() throws IOException, FileNotFoundException, SAXException, ParserConfigurationException  {
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
		return GetDocument(userSettingsPath);
	}
	
	private static boolean isActive(Element e) { //Where should this go!
		String activeness = e.getAttribute("active");
		return //activeness != null && 
			(activeness.equals("true")||activeness.equals("active"));
	}
}

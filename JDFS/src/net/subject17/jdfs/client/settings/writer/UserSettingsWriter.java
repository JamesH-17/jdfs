package net.subject17.jdfs.client.settings.writer;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;

import javax.xml.transform.TransformerException;

import net.subject17.jdfs.client.io.Printer;
import net.subject17.jdfs.client.user.User;

import org.w3c.dom.Document;
import org.w3c.dom.Element;



public class UserSettingsWriter extends SettingsWriter {
	private Path outputFile = null;
	
	public UserSettingsWriter() {
		outputFile = userSettingsPath;
	}
	public UserSettingsWriter(Path file) {
		outputFile = file;
	}
	public void writeUserSettings(ArrayList<User> users) {
		writeUserSettings(outputFile, users, null);
	}
	public void writeUserSettings(ArrayList<User> users, User activeUser) {
		writeUserSettings(outputFile, users, activeUser);
	}
	public void writeUserSettings(Path loc, ArrayList<User> users) {
		writeUserSettings(loc, users, null);
	}
	public void writeUserSettings(Path loc, ArrayList<User> users, User activeUser) {
		try {
			Document doc = getNewDocBuilder();
			doc = createDocument(doc, users, activeUser);
			
			writeDocument(doc, loc);
			
		} catch (TransformerException e) {
			Printer.logErr("Could not instatiate transformer to write settings file", Printer.Level.Medium);
		} catch (Exception e) {
			Printer.logErr("An unexpected error occured in UserSettingsWriter.writeUserSettings.  Bad filepath?");
		}
	}
	
	private Document createDocument(Document doc, ArrayList<User> users, User activeUser){
		Element root = doc.createElement("users");
		
		for (User user : users) {
			//User settings
			Element userTag = doc.createElement("user");
			Element accountTag = doc.createElement("email");
			Element userNameTag = doc.createElement("userName");
			
			accountTag.appendChild(doc.createTextNode(user.getAccountEmail()));
			userNameTag.appendChild(doc.createTextNode(user.getUserName()));
			
			if (activeUser != null && user.equals(activeUser)) {
				userTag.setAttribute("active", "true");
			}
			
			userTag.appendChild(accountTag);
			userTag.appendChild(userNameTag);
			
			root.appendChild(userTag);
		}
		
		doc.appendChild(root);
		
		return doc;		
	}
}

package com.subject17.jdfs.client.settings.writer;

import java.io.File;
import java.util.ArrayList;

import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.subject17.jdfs.client.io.Printer;
import com.subject17.jdfs.client.user.User;


public class UserSettingsWriter extends SettingsWriter {
	public void writeUserSettings(ArrayList<User> users) {
		writeUserSettings(userSettingsFile, users, null);
	}
	public void writeUserSettings(ArrayList<User> users, User activeUser) {
		writeUserSettings(userSettingsFile, users, activeUser);
	}
	public void writeUserSettings(File loc, ArrayList<User> users) {
		writeUserSettings(loc, users, null);
	}
	public void writeUserSettings(File loc, ArrayList<User> users, User activeUser) {
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
		
		for(User user : users) {
			//User settings
			Element userTag = doc.createElement("user");
			Element accountTag = doc.createElement("email");
			Element userNameTag = doc.createElement("userName");
			
			accountTag.appendChild(doc.createTextNode(user.getAccountEmail()));
			userNameTag.appendChild(doc.createTextNode(user.getUsername()));
			
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

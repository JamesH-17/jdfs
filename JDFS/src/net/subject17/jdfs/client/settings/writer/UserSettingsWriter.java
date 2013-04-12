package net.subject17.jdfs.client.settings.writer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.UUID;

import javax.xml.transform.TransformerException;

import net.subject17.jdfs.client.io.Printer;
import net.subject17.jdfs.client.user.User;

import org.w3c.dom.Document;
import org.w3c.dom.Element;



public class UserSettingsWriter extends SettingsWriter {
	private Path outputFile = null;
	
	public void setOutputFile(Path newPath){outputFile = newPath;}
	
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
			Printer.log("Users file written to "+loc);
		} catch (TransformerException e) {
			Printer.logErr("Could not instatiate transformer to write settings file", Printer.Level.Medium);
		} catch (Exception e) {
			e.printStackTrace();
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
			Element userGUIDTag = doc.createElement("GUID");
			Element userMachinesTag = doc.createElement("linkedMachines");
			
			accountTag.appendChild(doc.createTextNode(user.getAccountEmail()));
			userNameTag.appendChild(doc.createTextNode(user.getUserName()));
			userGUIDTag.appendChild(doc.createTextNode(user.getGUID().toString()));
			
			if (activeUser != null && user.equals(activeUser)) {
				userTag.setAttribute("active", "true");
			}
			
			for (UUID machineGuid : user.getRegisteredMachines()) {
				Element machine = doc.createElement("machine");
				machine.appendChild(doc.createTextNode(machineGuid.toString()));
				userMachinesTag.appendChild(machine);
			}
			
			userTag.appendChild(accountTag);
			userTag.appendChild(userNameTag);
			userTag.appendChild(userGUIDTag);
			userTag.appendChild(userMachinesTag);
			
			root.appendChild(userTag);
		}
		
		doc.appendChild(root);
		
		return doc;		
	}
}

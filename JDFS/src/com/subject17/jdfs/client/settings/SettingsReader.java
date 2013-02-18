/* NOTE:  This file is officially where I hit the factory wall.
 * I could have probably used faster tools for the job, but I'm probably gonna toss
 * my hard drive out the window if I have to read another doc page at this point.
 */


package com.subject17.jdfs.client.settings;

import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class SettingsReader extends Settings {
	
	public SettingsReader() {
		settingsFileName = "";
	}
	public SettingsReader(String nameOfSettingsFileToUse) {
		settingsFileName = nameOfSettingsFileToUse;
		parseAndReadXMLDocument();
	}
	
	public String getSettingsFile() {
		return settingsFileName;
	}
	
	/**
	 * @param newSettingsFilename New name of the settings file to be read.  
	 * No effect if value is same as current value.
	 * @return true if the new file can be read in, or if it's the same name as before 
	 * (will not re-read information in that case)
	 */
	public boolean setSettingsFileLocation(String newSettingsFilename) {
		return (newSettingsFilename.equals(settingsFileName)) || ( //Hoping short circuit works here
			setSettingsFile(newSettingsFilename) && parseAndReadXMLDocument() //This may give concurrency issues
		);
	}
	
	private boolean parseAndReadXMLDocument() {
		try { //I'm really not a fan of being forced to use factories here
			DocumentBuilderFactory docbuilder = DocumentBuilderFactory.newInstance();
			DocumentBuilder parser = docbuilder.newDocumentBuilder();
			Document doc = parser.parse(settingsFileName);
			readXMLDocument(doc);
			
		} catch (Exception e) {
			System.out.print("Error parsing xml document for settings");
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	private void readXMLDocument(Document doc) throws Exception {
		try {
			readUsers(doc);
			readPeersLocation(doc);
		} catch (Exception e) {
			System.out.print("Failed reading one or more settings");
			throw e; 
		} finally {
			;
		}
	}
	
	private void readUsers(Document doc) throws Exception {
		//Nodelist isn't iterable, so we have to use a for loop
		NodeList xmlUsernames = doc.getElementsByTagName("user");
		
		if (xmlUsernames != null) {
			users = new ArrayList<String>();
			for (int i = 0; i < xmlUsernames.getLength(); ++i) {
				String val = xmlUsernames.item(i).getNodeValue();
				if (val!=null && !val.isEmpty())
					users.add(xmlUsernames.item(i).getNodeValue());
			}
			if (users.size() > 0)
				currentUser = users.get(0);
			else throw new Exception("No users found in file, or error accessing class variable users");
		} else throw new Exception("No tag users found in xml document "+doc.getDocumentURI());
	}
	
	private void readPeersLocation(Document doc) {
		//Nodelist isn't iterable, so we have to use a for loop
		NodeList xmlPeerFileLocations = doc.getElementsByTagName("peersFileLocation");
		
		if (xmlPeerFileLocations != null && xmlPeerFileLocations.getLength() > 0) {
			setPeerFile(xmlPeerFileLocations.item(0).getNodeValue());
		}
	}
	
}

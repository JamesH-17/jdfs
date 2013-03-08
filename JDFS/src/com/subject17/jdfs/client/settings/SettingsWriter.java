package com.subject17.jdfs.client.settings;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;

public class SettingsWriter extends Settings {
	
	//public void writeXMLSettings() { writeXMLSettings(settingsFileName); }
	
	public void writeXMLSettings(String settingsFileLocation) {
		try {
			
		} catch (Exception e) {
			
		}
	}
	
	public Document getNewDocBuilder() throws ParserConfigurationException {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
 
		// root elements
		return docBuilder.newDocument();
	}
}

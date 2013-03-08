package com.subject17.jdfs.client.settings;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.subject17.jdfs.client.account.UserUtil;

public class SettingsReader extends Settings {

	public SettingsReader() {
		setSettingsFileLocation(defaultSettingsFileName);
	}
	public SettingsReader(String nameOfSettingsFileToUse) {
		setSettingsFileLocation(nameOfSettingsFileToUse);
		//parseAndReadXMLDocument();
	}
	
	public String getSettingsFile() {
		return settingsFileName;
	}
	public String getPeerFileLocation(){return peerFileLocation;}
	
	protected boolean parseAndReadXMLDocument() {
		try {
			readXMLDocument(GetDocument(settingsFileName));
			
		} catch (Exception e) {
			System.out.print("Error parsing xml document for settings");
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public static Document GetDocument(String filePath, String fileName) throws ParserConfigurationException, SAXException, IOException {
		return GetDocument(new File(filePath, fileName));
	}
	
	public static Document GetDocument(String fileName) throws ParserConfigurationException, SAXException, IOException {
		return GetDocument(new File(fileName));
	}
	
	public static Document GetDocument(File file) throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory docbuilder = DocumentBuilderFactory.newInstance();
		DocumentBuilder parser = docbuilder.newDocumentBuilder();
		return parser.parse(file);
	}
	
	private void readXMLDocument(Document doc) throws Exception {
		try {
			readPeersLocation(doc);
		} catch (Exception e) {
			System.out.print("Failed reading one or more settings");
			throw e; 
		} finally {
			;
		}
	}
	
	private void readPeersLocation(Document doc) {
		//Nodelist isn't iterable, so we have to use a for loop
		NodeList xmlPeerFileLocations = doc.getElementsByTagName("peersFileLocation");
		
		if (xmlPeerFileLocations != null && xmlPeerFileLocations.getLength() > 0) {
			setPeerFile(xmlPeerFileLocations.item(0).getNodeValue());
		}
	}
	
	
}

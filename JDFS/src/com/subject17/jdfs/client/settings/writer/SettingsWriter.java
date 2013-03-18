package com.subject17.jdfs.client.settings.writer;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.subject17.jdfs.client.io.Printer;
import com.subject17.jdfs.client.settings.Settings;

public class SettingsWriter extends Settings {
	
	public void writeXMLSettings() {
		writeXMLSettings(settingsFile);
	}
	public void writeXMLSettings(String settingsFileLocation) {
		writeXMLSettings(getWriteLocation(settingsFileLocation));
	}
	public void writeXMLSettings(File loc) {
		try {
			Document doc = getNewDocBuilder();
			doc = createDocument(doc);
			
			writeDocument(doc, loc);
			
		} catch (TransformerException e) {
			Printer.logErr("Could not instatiate transformer to write settings file", Printer.Level.Medium);
		} catch (Exception e) {
			Printer.logErr("An unexpected error occured in SettingsWriter.writeXMLSettings.  Wrong filepath?");
		}
	}
	
	
	private Document createDocument(Document doc) {
		Element root = doc.createElement("jdfsSettings");
		doc.appendChild(root); //TODO:  Should we move this to the end of this function:
		
		Element configLocations = doc.createElement("configLocations");
		root.appendChild(configLocations);		
		
		//User settings
		Element userFilePath = doc.createElement("userFilePath");
		userFilePath.appendChild(doc.createTextNode(userSettingsFile.getPath()));
		configLocations.appendChild(userFilePath);
		
		Element userFileName = doc.createElement("userFileName");
		userFileName.appendChild(doc.createTextNode(userSettingsFile.getName()));
		configLocations.appendChild(userFileName);
		
		//Peer settings
		Element peersFilePath = doc.createElement("peersFilePath");
		peersFilePath.appendChild(doc.createTextNode(peerSettingsFile.getPath()));
		configLocations.appendChild(peersFilePath);
		
		Element peersFileName = doc.createElement("peersFileName");
		peersFileName.appendChild(doc.createTextNode(peerSettingsFile.getName()));
		configLocations.appendChild(peersFileName);
		
		//Watch settings
		Element watchFilePath = doc.createElement("watchFilePath");
		watchFilePath.appendChild(doc.createTextNode(watchSettingsFile.getPath()));
		configLocations.appendChild(watchFilePath);
		
		Element watchFileName = doc.createElement("watchFileName");
		watchFileName.appendChild(doc.createTextNode(watchSettingsFile.getName()));
		configLocations.appendChild(watchFileName);
		
		//User settings
		Element storageDirectoryTag = doc.createElement("storageDirectory");
		storageDirectoryTag.appendChild(doc.createTextNode(storageDirectory.getPath()));
		root.appendChild(storageDirectoryTag);
		
		return doc;
	}
	
	
	
	//Utilities
	protected void writeDocument(Document doc) throws TransformerException {
		writeDocument(doc, new File(defaultSettingsFilePath, defaultSettingsFileName));
	}
	
	protected void writeDocument(Document doc, File writeLocation) throws TransformerException {
		Transformer transformer = getNewTransformer();
		DOMSource src = new DOMSource(doc);
		StreamResult res = new StreamResult(writeLocation);
		
		transformer.transform(src, res);
	}
	
	protected File getWriteLocation(String settingsFileLocation) {
		File loc = new File(settingsFileLocation);
		
		if (settingsFileLocation == null || settingsFileLocation.equals(""))
			loc = new File(defaultSettingsFilePath, defaultSettingsFileName); //Trusting that I'll always have this be valid
		
		if (loc.isDirectory())
			loc = new File(loc.getParent(), defaultSettingsFileName);
		if (!new File(loc.getParent()).exists())
			loc = new File(defaultSettingsFilePath, loc.getName());
		
		return loc;
	}
	
	protected File getWriteLocation(String settingsFilePath, String settingsFileName) {
		if (settingsFilePath == null || settingsFilePath.equals(""))
			settingsFilePath = defaultSettingsFilePath;
		if (settingsFileName == null || settingsFileName.equals(""))
			settingsFileName = defaultSettingsFileName;
		
		return new File(settingsFilePath, settingsFileName); //If they supply a wrong location, not our problem
	}
	
	protected Document getNewDocBuilder() throws ParserConfigurationException {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		return docBuilder.newDocument();
	}

	private Transformer getNewTransformer() throws TransformerConfigurationException {
		TransformerFactory transFact = TransformerFactory.newInstance();
		return transFact.newTransformer();
	}
}


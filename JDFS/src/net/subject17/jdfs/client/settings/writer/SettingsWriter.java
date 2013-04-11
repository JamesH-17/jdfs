package net.subject17.jdfs.client.settings.writer;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import net.subject17.jdfs.client.io.Printer;
import net.subject17.jdfs.client.settings.Settings;

import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


public class SettingsWriter extends Settings {
	
	public void writeXMLSettings() {
		writeXMLSettings(settingsPath);
	}
	public void writeXMLSettings(String settingsFileLocation) {
		writeXMLSettings(getWriteLocation(settingsFileLocation));
	}
	public void writeXMLSettings(Path loc) {
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
		userFilePath.appendChild(doc.createTextNode(userSettingsPath.getParent().toString()));
		configLocations.appendChild(userFilePath);
		
		Element userFileName = doc.createElement("userFileName");
		userFileName.appendChild(doc.createTextNode(userSettingsPath.getFileName().toString()));
		configLocations.appendChild(userFileName);
		
		Comment userComm = doc.createComment("User Settings");
		configLocations.insertBefore(userComm, userFilePath);
		
		//Peer settings
		Element peersFilePath = doc.createElement("peersFilePath");
		peersFilePath.appendChild(doc.createTextNode(peerSettingsPath.getParent().toString()));
		configLocations.appendChild(peersFilePath);
		
		Element peersFileName = doc.createElement("peersFileName");
		peersFileName.appendChild(doc.createTextNode(peerSettingsPath.getFileName().toString()));
		configLocations.appendChild(peersFileName);
		
		Comment peerComm = doc.createComment("Peer Settings");
		configLocations.insertBefore(peerComm, peersFilePath);
		
		//Watch settings
		Element watchFilePath = doc.createElement("watchFilePath");
		watchFilePath.appendChild(doc.createTextNode(watchSettingsPath.getParent().toString()));
		configLocations.appendChild(watchFilePath);
		
		Element watchFileName = doc.createElement("watchFileName");
		watchFileName.appendChild(doc.createTextNode(watchSettingsPath.getFileName().toString()));
		configLocations.appendChild(watchFileName);
		
		Comment watchComm = doc.createComment("Watch Settings");
		configLocations.insertBefore(watchComm, watchFilePath);
		
		//Storage settings
		Element storageDirectoryTag = doc.createElement("storageDirectory");
		storageDirectoryTag.appendChild(doc.createTextNode(storageDirectory.toString()));
		root.appendChild(storageDirectoryTag);
		
		Comment storageComm = doc.createComment("Locations where other user's files will be stored");
		root.insertBefore(storageComm, storageDirectoryTag);
		
		//Machine GUID settings
		Element MachineGUIDTag = doc.createElement("MachineGUID");
		MachineGUIDTag.appendChild(doc.createTextNode(MachineGUID.toString()));
		root.appendChild(MachineGUIDTag);
		
		Comment machineGuidComm = doc.createComment("Unique Identifier for this machine.  DO NOT CHANGE!!!");
		root.insertBefore(machineGuidComm, MachineGUIDTag);
		
		//TODO for future:
		//Add encryption and compression settings
		
		return doc;
	}
	
	//Utilities
	protected void writeDocument(Document doc) throws TransformerException {
		writeDocument(doc, Paths.get(defaultSettingsDirectory, defaultSettingsPathName));
	}
	
	protected void writeDocument(Document doc, Path sourceFile) throws TransformerException {
		Transformer transformer = getNewTransformer(); //Has pretty print in it as well
		DOMSource src = new DOMSource(doc);
		StreamResult res = new StreamResult(sourceFile.toFile());
		
		transformer.transform(src, res);
	}
	
	protected Path getWriteLocation(String settingsFileLocation) {
		Path path = Paths.get(settingsFileLocation);
		
		if (!Files.exists(path))
			path = Paths.get(defaultSettingsDirectory, defaultSettingsPathName); //Trusting that I'll always have this be valid
		
		if (Files.isDirectory(path))
			path = path.resolve(defaultSettingsPathName);
		
		return path;
	}
	
	protected File getWriteLocation(String settingsFilePath, String settingsFileName) {
		if (settingsFilePath == null || settingsFilePath.equals(""))
			settingsFilePath = defaultSettingsDirectory;
		if (settingsFileName == null || settingsFileName.equals(""))
			settingsFileName = defaultSettingsPathName;
		
		return new File(settingsFilePath, settingsFileName); //If they supply a wrong location, not our problem
	}
	
	protected Document getNewDocBuilder() throws ParserConfigurationException {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		return docBuilder.newDocument();
	}

	private Transformer getNewTransformer() throws TransformerConfigurationException {
		TransformerFactory transFact = TransformerFactory.newInstance();
		Transformer trans = transFact.newTransformer();
		trans.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		trans.setOutputProperty(OutputKeys.INDENT, "yes");
		trans.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
		return trans;
	}
}


package com.subject17.jdfs.client.settings.reader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.subject17.jdfs.client.io.Printer;
import com.subject17.jdfs.client.settings.Settings;
import com.subject17.jdfs.client.settings.writer.SettingsWriter;

public class SettingsReader extends Settings {

	public class SettingsReaderException extends Exception {
		SettingsReaderException(String msg){super(msg);}
		SettingsReaderException(String msg, Throwable thrw){super(msg, thrw);}
	}
	
	public SettingsReader() throws ParserConfigurationException, SAXException, IOException {
		settingsPath = Paths.get(defaultSettingsDirectory,defaultSettingsPathName);
		try {
			parseAndReadXMLDocument();
		} catch(FileNotFoundException e) {
			Printer.logErr("Could not parse document -- using default values for locations");
			//Printer.logErr(e);
			Printer.logErr("Attempting to create file with default values");
			createDefaultSettingsFile();
		}
	}
	
	public SettingsReader(String nameOfSettingsFileToUse) throws ParserConfigurationException, SAXException, IOException {
		settingsPath = Paths.get(nameOfSettingsFileToUse);
		parseAndReadXMLDocument();
	}
	public SettingsReader(String pathOfFile, String nameOfFile) throws ParserConfigurationException, SAXException, IOException {
		settingsPath = Paths.get(pathOfFile, nameOfFile);
		parseAndReadXMLDocument();
	}

	private void createDefaultSettingsFile() throws IOException {
		SettingsWriter sw = new SettingsWriter();
		sw.setDefaultPathLocations();
		sw.writeXMLSettings();
	}
	
	private void parseAndReadXMLDocument() throws ParserConfigurationException, SAXException, IOException {
		setDefaultPathLocations();
			Document settingsXML = GetDocument(settingsPath);

		if (settingsXML != null) {
			Element configNode = GetConfigNode(settingsXML);
			if (configNode != null) {
				readAndSetPeersFile(configNode);
				readAndSetUsersFile(configNode);
				readAndSetWatchFile(configNode);
			}
			readAndSetStorageDirectory(settingsXML);
		}
	}
	
	private void readAndSetStorageDirectory(Document settingsXML) throws IOException {
		Element root = GetFirstNode(settingsXML, "jdfsSettings");
		if (root != null) {
			String storagePath = extractNodeValue(root, "fileStoragePath");
			
			if (!(storagePath == null || storagePath.trim().isEmpty()))
				setStorageDirectory(storagePath);
			
		} else throw new IOException("Invalid XML layout");
	}
	
	private void readAndSetWatchFile(Element configNode) throws IOException {
		String watchFilePath = extractNodeValue(configNode, "watchFilesPath");
		String watchFileName = extractNodeValue(configNode, "watchFilesName");

		if ((watchFilePath == null || watchFilePath.trim().isEmpty() ))
			watchFilePath = defaultSettingsDirectory;
		if ((watchFileName == null || watchFileName.trim().isEmpty()))
			watchFileName = defaultWatchPathName;
		setWatchPath(Paths.get(watchFilePath,watchFileName));
	}
	
	private final void readAndSetUsersFile(Element configNode) throws IOException {
		String userFilePath = extractNodeValue(configNode, "userFilePath");
		String userFileName = extractNodeValue(configNode, "userFileName");

		if ((userFilePath == null || userFilePath.trim().isEmpty() ))
			userFilePath = defaultSettingsDirectory;
		if ((userFileName == null || userFileName.trim().isEmpty()))
			userFileName = defaultUserPathName;
		setUsersPath(Paths.get(userFilePath,userFileName));
		
	}
	private final void readAndSetPeersFile(Element configNode) throws IOException {
		String peersFilePath = extractNodeValue(configNode, "peersFilePath");
		String peersFileName = extractNodeValue(configNode, "peersFileName");
		
		if ((peersFilePath == null || peersFilePath.trim().isEmpty() ))
			peersFilePath = defaultSettingsDirectory;
		if ((peersFileName == null || peersFileName.trim().isEmpty()))
			peersFileName = defaultPeersPathName;
		setPeersPath(Paths.get(peersFilePath,peersFileName));
	}
	
	private static final String extractNodeValue(Element srcNode, String tagName){
		Element tag = GetFirstNode(srcNode, tagName);
		return tag == null || tag.getTextContent() == null ? "" : tag.getTextContent().trim();
	}
	
	/*  Removing these for clarity
	protected Document GetDocument(String filePath, String fileName) throws ParserConfigurationException, SAXException, IOException {
		return GetDocument(Paths.get(filePath, fileName));
	}
	
	protected Document GetDocument(String fileName) throws ParserConfigurationException, SAXException, IOException {
		return GetDocument(Paths.get(fileName));
	}*/
	
	protected Document GetDocument(Path path) throws ParserConfigurationException, SAXException, IOException {
		Printer.log("Parsing file "+path,Printer.Level.VeryLow);
		return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(path.toFile());
	}
	
	public static Element GetFirstNode(Document Doc, String tagName) {
		return (Element) Doc.getElementsByTagName(tagName).item(0);
	}
	
	public static Element GetFirstNode(Element parent, String tagName) {
		return parent == null ? parent : (Element) parent.getElementsByTagName(tagName).item(0);
	}
	
	public static String GetFirstNodeValue(Element parent, String tagName) {
		Element ele = GetFirstNode(parent,tagName);
		return ele == null ? "" : ele.getTextContent();
	}
	
	private Element GetConfigNode(Document Doc) {
		Element root = GetFirstNode(Doc, "jdfsSettings");
		Element configRoot = GetFirstNode(root, "configLocations");
		return configRoot;
	}	
}

package com.subject17.jdfs.client.settings.reader;

import java.io.File;
import java.io.IOException;

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

	public SettingsReader() throws ParserConfigurationException, SAXException, IOException {
		settingsFile = new File(defaultSettingsFilePath,defaultSettingsFileName);
		try {
			parseAndReadXMLDocument();
		} catch(IOException e) {
			Printer.logErr("Could not parse document -- using default values for locations");
			Printer.logErr(e);
			Printer.logErr("Attempting to create file with default values");
			createDefaultSettingsFile();
		}
	}
	
	public SettingsReader(String nameOfSettingsFileToUse) throws ParserConfigurationException, SAXException, IOException {
		settingsFile = new File(nameOfSettingsFileToUse);
		parseAndReadXMLDocument();
	}
	public SettingsReader(String pathOfFile, String nameOfFile) throws ParserConfigurationException, SAXException, IOException {
		settingsFile = new File(pathOfFile, nameOfFile);
		parseAndReadXMLDocument();
	}
	
	public File getSettingsFile() {
		return settingsFile;
	}

	private void createDefaultSettingsFile() throws IOException {
		SettingsWriter sw = new SettingsWriter();
		sw.setDefaultFileLocations();
		sw.writeXMLSettings();
	}
	
	private void parseAndReadXMLDocument() throws ParserConfigurationException, SAXException, IOException {
		setDefaultFileLocations();
		Document settingsXML = GetDocument(settingsFile);
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
		Element root = SettingsReader.GetFirstNode(settingsXML, "jdfsSettings");
		Element storageNode = SettingsReader.GetFirstNode(root, "fileStoragePath");
		String storagePath = storageNode == null ? "" : storageNode.getNodeValue();
		if (!(storagePath == null || storagePath.trim().isEmpty()))
			setStorageDirectory(storagePath);
	}
	
	private void readAndSetWatchFile(Element configNode) throws IOException {
		String watchFilePath = GetFirstNode(configNode, "watchFilePath").getNodeValue();
		String watchFileName = GetFirstNode(configNode, "watchFileName").getNodeValue();

		if (!(watchFilePath == null || watchFilePath.trim().isEmpty()))
			setWatchDirectory(watchFilePath);
		if (!(watchFileName == null || watchFileName.trim().isEmpty()))
			setWatchFileName(watchFileName);
	}
	private void readAndSetUsersFile(Element configNode) throws IOException {
		String userFilePath = GetFirstNode(configNode, "userFilePath").getNodeValue();
		String userFileName = GetFirstNode(configNode, "userFileName").getNodeValue();

		if (!(userFilePath == null || userFilePath.trim().isEmpty()))
			setUsersDirectory(userFilePath);
		if (!(userFileName == null || userFileName.trim().isEmpty()))
			setUsersFileName(userFileName);
		
	}
	private void readAndSetPeersFile(Element configNode) throws IOException {
		String peersFilePath = GetFirstNode(configNode, "peersFilePath").getNodeValue();
		String peersFileName = GetFirstNode(configNode, "peersFileName").getNodeValue();
		
		if (!(peersFilePath == null || peersFilePath.trim().isEmpty() ))
			setPeersDirectory(peersFilePath);
		if (!(peersFileName == null || peersFileName.trim().isEmpty()))
			setPeersFilename(peersFileName);
		
	}
	
	protected Document GetDocument(String filePath, String fileName) throws ParserConfigurationException, SAXException, IOException {
		return GetDocument(new File(filePath, fileName));
	}
	
	protected Document GetDocument(String fileName) throws ParserConfigurationException, SAXException, IOException {
		return GetDocument(new File(fileName));
	}
	
	protected Document GetDocument(File file) throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory docbuilder = DocumentBuilderFactory.newInstance();
		DocumentBuilder parser = docbuilder.newDocumentBuilder();
		return parser.parse(file);
	}
	
	public static Element GetFirstNode(Document Doc, String tagName) {
		return (Element) Doc.getElementsByTagName(tagName).item(0);
	}
	
	public static Element GetFirstNode(Element parent, String tagName) {
		return (Element) parent.getElementsByTagName(tagName).item(0);
	}
	
	private Element GetConfigNode(Document Doc) {
		Element root = GetFirstNode(Doc, "jdfsSettings");
		Element configRoot = GetFirstNode(root, "configLocations");
		return configRoot;
	}	
}

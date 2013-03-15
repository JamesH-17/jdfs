package com.subject17.jdfs.client.settings.reader;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.subject17.jdfs.client.settings.Settings;

public class SettingsReader extends Settings {

	public SettingsReader() throws ParserConfigurationException, SAXException, IOException {
		settingsFile = new File(defaultSettingsFileName);
		parseAndReadXMLDocument();
	}
	public SettingsReader(String nameOfSettingsFileToUse) throws ParserConfigurationException, SAXException, IOException {
		settingsFile = new File(nameOfSettingsFileToUse);
		parseAndReadXMLDocument();
	}
	public SettingsReader(String pathOfFile, String nameOfFile) throws ParserConfigurationException, SAXException, IOException {
		settingsFile = new File(pathOfFile, nameOfFile);
		parseAndReadXMLDocument();
	}
	
	public static File getSettingsFile() {
		return settingsFile;
	}
	
	protected void parseAndReadXMLDocument() throws ParserConfigurationException, SAXException, IOException {
		setDefaultFileLocations();
		Document settingsXML = GetDocument(settingsFile);
		Element configNode = GetConfigNode(settingsXML);
		
		readAndSetPeersFile(configNode);
		readAndSetUsersFile(configNode);
		readAndSetWatchFile(configNode);
		
	}
	
	private void readAndSetWatchFile(Element configNode) {
		String watchFilePath = GetFirstNode(configNode, "watchFilePath").getNodeValue();
		String watchFileName = GetFirstNode(configNode, "watchFileName").getNodeValue();

		if (!watchFilePath.isEmpty())
			setWatchDirectory(watchFilePath);
		if (!watchFileName.isEmpty())
			setWatchFilename(watchFileName);
	}
	private void readAndSetUsersFile(Element configNode) {
		String userFilePath = GetFirstNode(configNode, "userFilePath").getNodeValue();
		String userFileName = GetFirstNode(configNode, "userFileName").getNodeValue();

		if (!userFilePath.isEmpty())
			setUsersDirectory(userFilePath);
		if (!userFileName.isEmpty())
			setUsersFilename(userFileName);
		
	}
	private void readAndSetPeersFile(Element configNode) {
		String peersFilePath = GetFirstNode(configNode, "peersFilePath").getNodeValue();
		String peersFileName = GetFirstNode(configNode, "peersFileName").getNodeValue();

		if (!peersFilePath.isEmpty())
			setPeersDirectory(peersFilePath);
		if (!peersFileName.isEmpty())
			setPeersFilename(peersFileName);
		
	}
	protected void setDefaultFileLocations() {
		String settingsPath = settingsFile.getPath();
		setPeersFile(new File(settingsPath, defaultPeersFileName));
		setUsersFile(new File(settingsPath, defaultUserFileName));
		setWatchFile(new File(settingsPath, defaultWatchFileName));
		setStorageDirectory(defaultStorageDirectory);
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
	
	public static Element GetFirstNode(Document Doc, String tagName) {
		return (Element) Doc.getElementsByTagName(tagName).item(0);
	}
	
	public static Element GetFirstNode(Element parent, String tagName) {
		return (Element) parent.getElementsByTagName(tagName).item(0);
	}
	
	private static Element GetConfigNode(Document Doc) {
		Element root = GetFirstNode(Doc, "jdfsSettings");
		Element configRoot = GetFirstNode(root, "configLocations");
		return configRoot;
	}	
}
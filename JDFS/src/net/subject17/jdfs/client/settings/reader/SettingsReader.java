package net.subject17.jdfs.client.settings.reader;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.subject17.jdfs.client.io.Printer;
import net.subject17.jdfs.client.settings.Settings;
import net.subject17.jdfs.client.settings.writer.SettingsWriter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;


public class SettingsReader extends Settings {

	public class SettingsReaderException extends Exception {
		private static final long serialVersionUID = -5082082063748172393L;
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
	public SettingsReader(Path location) throws ParserConfigurationException, SAXException, IOException {
		settingsPath = location;
		parseAndReadXMLDocument();
	}

	private void createDefaultSettingsFile() throws IOException {
		setMachineGUID(UUID.randomUUID());
		
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
			readAndSetMachineGUID(configNode);
		}
	}
	
	private void readAndSetStorageDirectory(Document settingsXML) throws IOException {
		Element root = GetFirstNode(settingsXML, "jdfsSettings");
		if (root != null) {
			String storagePath = extractNodeValue(root, "storageDirectory");
			
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
	
	private final void readAndSetMachineGUID(Element configNode) {
		String guid = extractNodeValue(configNode, "MachineGUID");
		
		if (null == guid || guid.trim().isEmpty()) {
			Printer.log("Generating new MachineGUID for us");
			setMachineGUID(UUID.randomUUID());
		} else
			setMachineGUID(UUID.fromString(guid));
	}
	
	private static final String extractNodeValue(Element srcNode, String tagName){
		Element tag = GetFirstNode(srcNode, tagName);
		return null == tag || null == tag.getTextContent() ? "" : tag.getTextContent().trim();
	}
	
	/**
	 * 
	 * @param path Attempts to parse a document from this location
	 * @return The parsed document (if successful)
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	protected Document GetDocument(Path path) throws ParserConfigurationException, SAXException, IOException {
		Printer.log("Parsing file "+path,Printer.Level.VeryLow);
		return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(path.toFile());
	}
	
	/**
	 * Grabs the first child node of the document with the provided tagname
	 * @param parent Document that we're looking in for tagName
	 * @param tagName name of the tag you want to grab
	 * @return First element named "tagName", or null if not found in document
	 */
	public static Element GetFirstNode(Document Doc, String tagName) {
		return null == Doc ? null : (Element) Doc.getElementsByTagName(tagName).item(0);
	}
	
	/**
	 * Grabs the first child node of the parent with the provided tagname
	 * @param parent Element that we're looking in for tagName
	 * @param tagName name of the tag you want to grab
	 * @return First element named "tagName", or null if not found under parent
	 */
	public static Element GetFirstNode(Element parent, String tagName) {
		return null == parent ? null : (Element) parent.getElementsByTagName(tagName).item(0);
	}
	
	/**
	 * Grabs the textual value of the first child node of parent found with the given tagname,
	 * or the empty string if anything in the chain came back as null
	 * @param parent
	 * @param tagName
	 * @return Node value, or empty string if not found
	 */
	public static String GetFirstNodeValue(Element parent, String tagName) {
		Element ele = GetFirstNode(parent,tagName);
		return null == ele ? "" : ele.getTextContent();
	}
	
	private Element GetConfigNode(Document Doc) {
		Element root = GetFirstNode(Doc, "jdfsSettings");
		Element configRoot = GetFirstNode(root, "configLocations");
		return configRoot;
	}
	
}

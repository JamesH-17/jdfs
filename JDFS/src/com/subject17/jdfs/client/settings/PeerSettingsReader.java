package com.subject17.jdfs.client.settings;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class PeerSettingsReader extends SettingsReader {
	private String file;
	private String path;
	
	private Document peerDoc;
	
	public PeerSettingsReader(String filename, String pathName) throws ParserConfigurationException, SAXException, IOException{
		file = filename;
		path = pathName;
		
		peerDoc = GetDocument(file, path);
	}

}

package com.subject17.jdfs.client.settings.reader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.subject17.jdfs.client.file.handler.FileUtils;
import com.subject17.jdfs.client.io.Printer;
import com.subject17.jdfs.client.peers.Peer;
import com.subject17.jdfs.client.settings.writer.PeerSettingsWriter;

public class PeerSettingsReader extends SettingsReader {
	private File sourceFile;
	private HashSet<Peer> peers;
	
	private Document peerDoc;
	
	public PeerSettingsReader(String fileName, String pathName) throws Exception {
		sourceFile = new File(pathName, fileName);
		Init();
	}
	
	public PeerSettingsReader(File fileToUse) throws Exception {

		sourceFile = fileToUse;
		Init();
	}
	private void Init() throws IOException, ParserConfigurationException, SAXException {
		try {
			
			FileUtils.checkIfFileReadable(sourceFile);
			peerDoc = GetDocument(sourceFile);
			peers = ReadInPeers(peerDoc);
			
		} catch (FileNotFoundException e){
			Printer.logErr("File not found -- "+sourceFile.getPath());
			Printer.logErr(e);
			Printer.logErr("Attempting to create a default peer settings file");
			
			InitDefault();
		}
	}
	
	private void InitDefault(){
		PeerSettingsWriter peerSettingsWriter = new PeerSettingsWriter();
		peerSettingsWriter.writePeerSettings(sourceFile,new HashSet<Peer>());
		peers = new HashSet<Peer>();
		peerDoc = null;

		Printer.log("Default peer settings file initialized.");
	}

	private HashSet<Peer> ReadInPeers(Document doc) {		
		NodeList peerTags = GetFirstNode(doc, "peers").getElementsByTagName("peer");
		HashSet<Peer> peersFound = new HashSet<Peer>();
		
		Printer.log("Found "+peerTags.getLength()+" potential peers");
		
		for (int i = 0; i < peerTags.getLength(); ++i) {
			Peer currentNode = new Peer((Element) peerTags.item(0));
			if (!currentNode.isBlankPeer())
				peersFound.add(currentNode);
		}
		return peersFound;
	}
	
	public HashSet<Peer> getPeers(){
		return peers;
	}
	
	public Document getPeerDocument() { return peerDoc; }

}

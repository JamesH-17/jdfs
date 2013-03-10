package com.subject17.jdfs.client.settings;

import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.subject17.jdfs.client.peers.Peer;
import com.subject17.jdfs.client.peers.PeersHandler;

public class PeerSettingsReader extends SettingsReader {
	private String file;
	private String path;
	private ArrayList<Peer> peers;
	
	private Document peerDoc;
	
	public PeerSettingsReader(String filename, String pathName) throws ParserConfigurationException, SAXException, IOException{
		file = filename;
		path = pathName;
		
		peerDoc = GetDocument(file, path);
		ReadInPeers(peerDoc);
	}
	
	private void ReadInPeers(Document doc) {
		NodeList peerTags = GetFirstNode(doc, "peers").getElementsByTagName("peer");
		peers = new ArrayList<Peer>();
		for (int i = 0; i < peerTags.getLength(); ++i) {
			Peer currentNode = new Peer((Element) peerTags.item(0));
			if (!currentNode.isBlankPeer())
				peers.add(currentNode);
		}
	}

}

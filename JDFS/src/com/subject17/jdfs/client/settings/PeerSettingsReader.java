package com.subject17.jdfs.client.settings;

import java.io.File;
import java.util.ArrayList;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import com.subject17.jdfs.client.file.handler.FileUtils;
import com.subject17.jdfs.client.peers.Peer;

public class PeerSettingsReader extends SettingsReader {
	private File sourceFile;
	private ArrayList<Peer> peers;
	
	private Document peerDoc;
	
	public PeerSettingsReader(String fileName, String pathName) throws Exception {
		sourceFile = new File(pathName, fileName);
		Init();
	}
	
	public PeerSettingsReader(File fileToUse) throws Exception {
		sourceFile = fileToUse;
		Init();
	}
	private void Init() throws Exception{
		FileUtils.checkIfFileReadable(sourceFile);
		peerDoc = GetDocument(sourceFile);
		peers = ReadInPeers(peerDoc);
	}

	private ArrayList<Peer> ReadInPeers(Document doc) {
		NodeList peerTags = GetFirstNode(doc, "peers").getElementsByTagName("peer");
		ArrayList<Peer> peersFound = new ArrayList<Peer>();
		for (int i = 0; i < peerTags.getLength(); ++i) {
			Peer currentNode = new Peer((Element) peerTags.item(0));
			if (!currentNode.isBlankPeer())
				peersFound.add(currentNode);
		}
		return peersFound;
	}
	
	public ArrayList<Peer> getPeers(){
		return peers;
	}
	
	public Document getPeerDocument() { return peerDoc; }

}

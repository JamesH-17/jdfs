package net.subject17.jdfs.client.settings.reader;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;

import javax.xml.parsers.ParserConfigurationException;

import net.subject17.jdfs.client.file.db.DBManager;
import net.subject17.jdfs.client.io.Printer;
import net.subject17.jdfs.client.peers.Peer;
import net.subject17.jdfs.client.settings.writer.PeerSettingsWriter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


public class PeerSettingsReader extends SettingsReader {
	private Path sourceFile;
	private HashSet<Peer> peers;
	
	private Document peerDoc;
	
	public PeerSettingsReader(Path pathToUse) throws Exception {
		sourceFile = pathToUse;
		Init();
	}
	private void Init() throws IOException, ParserConfigurationException, SAXException {
		try {
			Files.isReadable(sourceFile);
			peerDoc = GetDocument(sourceFile);
			peers = ReadInPeers(peerDoc);
			
		} catch (FileNotFoundException e){ //Also a possibility of malformed layout (ex: no peers tag), which will give a nullptr exception
			
			Printer.logErr("File not found -- "+sourceFile);
			//Printer.logErr(e);
			Printer.logErr("Attempting to create a default peer settings file at provided location");
			
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
		HashSet<Peer> peersFound = new HashSet<Peer>();
		try {
			NodeList peerTags = GetFirstNode(doc, "peers").getElementsByTagName("peer");
			
			Printer.log("Found "+peerTags.getLength()+" potential peers");
		
			for (int i = 0; i < peerTags.getLength(); ++i) {
				Peer currentNode = new Peer((Element) peerTags.item(0));
				if (!currentNode.isBlankPeer())
					peersFound.add(currentNode);
			}
		} catch(NullPointerException e) {
			Printer.logErr(e);
			Printer.log(
					"----------------------------------------------------"+
					System.getProperty("line.separator")+
					"YO!   AY, YOU!  A Null Pointer Exception was thrown in PeerSettingsReader." +
					System.getProperty("line.separator")+
					"This is bad.  Like, real bad.  You should check to see if the file located at ["+
					sourceFile +
					"] is malformed.  "+
					System.getProperty("line.separator")+
					"Is there a tag labelled <peers>? Cause there's supposed to be."+
					System.getProperty("line.separator")+
					"Anyway, no peers were loaded, and no attempt to fix the file was made."+
					System.getProperty("line.separator")+
					"----------------------------------------------------"
					,
					Printer.Level.High);
		}
		return peersFound;
	}
	
	public HashSet<Peer> getPeers(){
		return peers;
	}
	
	public Document getPeerDocument() { return peerDoc; }

}

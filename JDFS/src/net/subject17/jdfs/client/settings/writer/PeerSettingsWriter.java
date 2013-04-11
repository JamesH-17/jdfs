package net.subject17.jdfs.client.settings.writer;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.UUID;

import javax.xml.transform.TransformerException;

import net.subject17.jdfs.client.io.Printer;
import net.subject17.jdfs.client.net.IPUtil;
import net.subject17.jdfs.client.peers.Peer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;


public class PeerSettingsWriter extends SettingsWriter {
	
	public PeerSettingsWriter(){}
	public void writePeerSettings(HashSet<Peer> peers) {
		writePeerSettings(peerSettingsPath, peers);
	}
	public void writePeerSettings(Path loc, HashSet<Peer> peers) {
		try {
			Document doc = getNewDocBuilder();
			doc = createDocument(doc, peers);
			
			writeDocument(doc, loc);
			
		} catch (TransformerException e) {
			Printer.logErr("Could not instatiate transformer to write settings file "+loc.toString(), Printer.Level.Medium);
		} catch (Exception e) {
			Printer.logErr("An unexpected error occured in PeerSettingsWriter.writePeerSettings.  Bad filepath?");
		}
	}
	
	private Document createDocument(Document doc, HashSet<Peer> peers){
		Element root = doc.createElement("peers");
		
		for(Peer peer : peers) {
			//Peer settings
			Element peerTag = doc.createElement("peer");
			Element accountTag = doc.createElement("accountEmail");
			Element userNameTag = doc.createElement("userName");
			Element guidTag = doc.createElement("peerGUID");
			
			
			accountTag.appendChild(doc.createTextNode(peer.getEmail()));
			userNameTag.appendChild(doc.createTextNode(peer.getUsername()));
			guidTag.appendChild(doc.createTextNode(peer.getGUID().toString()));
			
			for(String ip4 : peer.getIp4s()){
				if (IPUtil.isValidIP4Address(ip4)) {
					Element ip4Tag = doc.createElement("ip4");
					ip4Tag.appendChild(doc.createTextNode(ip4));
					peerTag.appendChild(ip4Tag);
				}
			}
			
			for(String ip6 : peer.getIp6s()){
				if (IPUtil.isValidIP6Address(ip6)) {
					Element ip6Tag = doc.createElement("ip6");
					ip6Tag.appendChild(doc.createTextNode(ip6));
					peerTag.appendChild(ip6Tag);
				}
			}
			
			for(UUID machineGuid : peer.getMachineGUIDs()){
				if (machineGuid != null){
					Element machineTag = doc.createElement("machine");
					machineTag.appendChild(doc.createTextNode(machineGuid.toString()));
					peerTag.appendChild(machineTag);
				}
			}
			
			peerTag.appendChild(accountTag);
			peerTag.appendChild(userNameTag);
			peerTag.appendChild(guidTag);
			
			root.appendChild(peerTag);
		}
		
		doc.appendChild(root);
		
		return doc;		
	}
}

package net.subject17.jdfs.client.settings.writer;

import java.nio.file.Path;
import java.util.Collection;

import javax.xml.transform.TransformerException;

import net.subject17.jdfs.client.file.model.WatchList;
import net.subject17.jdfs.client.io.Printer;
import net.subject17.jdfs.client.net.IPUtil;
import net.subject17.jdfs.client.peers.Peer;
import net.subject17.jdfs.client.user.User;

import org.w3c.dom.Document;
import org.w3c.dom.Element;


public class WatchSettingsWriter extends SettingsWriter {
	public void writeWatchSettings(Collection<WatchList> watchLists) {
		writeWatchSettings(userSettingsPath, watchLists);
	}
	public void writeWatchSettings(Path loc, Collection<WatchList> watchLists) {
		try {
			Document doc = getNewDocBuilder();
			doc = createDocument(doc, watchLists);
			
			writeDocument(doc, loc);
			
		} catch (TransformerException e) {
			Printer.logErr("Could not instatiate transformer to write settings file", Printer.Level.Medium);
		} catch (Exception e) {
			Printer.logErr("An unexpected error occured in UserSettingsWriter.writeUserSettings.  Bad filepath?");
		}
	}
	
	private Document createDocument(Document doc, Collection<WatchList> watchLists){
		Element root = doc.createElement("peers");
		
		for (WatchList list : watchLists) { //Note that storing the users is redundant
			//Peer settings
			/*
			Element peerTag = doc.createElement("peer");
			Element accountTag = doc.createElement("accountEmail");
			Element userNameTag = doc.createElement("userName");
			
			accountTag.appendChild(doc.createTextNode(list.getEmail()));
			userNameTag.appendChild(doc.createTextNode(list.getUsername()));

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
			
			peerTag.appendChild(accountTag);
			peerTag.appendChild(userNameTag);
			root.appendChild(peerTag);*/
		}
		
		doc.appendChild(root);
		
		return doc;		
	}
}

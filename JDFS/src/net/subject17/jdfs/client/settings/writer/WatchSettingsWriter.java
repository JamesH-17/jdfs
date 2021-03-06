package net.subject17.jdfs.client.settings.writer;

import java.nio.file.Path;
import java.util.Collection;

import javax.xml.transform.TransformerException;

import net.subject17.jdfs.client.file.model.WatchDirectory;
import net.subject17.jdfs.client.file.model.WatchFile;
import net.subject17.jdfs.client.file.model.WatchList;
import net.subject17.jdfs.client.io.Printer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


public final class WatchSettingsWriter extends SettingsWriter {
	public void writeWatchSettings(Collection<WatchList> watchLists) {
		writeWatchSettings(watchSettingsPath, watchLists);
	}
	public static void writeWatchSettings(Path loc, Collection<WatchList> watchLists) {
		try {
			Printer.log("Write Watchlist to XML at location "+loc);
			
			Document doc = getNewDocBuilder();
			doc = createDocument(doc, watchLists);
			
			writeDocument(doc, loc);
			
		} catch (TransformerException e) {
			Printer.logErr("Could not instatiate transformer to write settings file", Printer.Level.Medium);
		} catch (Exception e) {
			Printer.logErr("An unexpected error occured in UserSettingsWriter.writeUserSettings.  Bad filepath?");
		}
	}
	
	private static Document createDocument(Document doc, Collection<WatchList> watchLists){
		Element root = doc.createElement("watchLists");
		
		for (WatchList list : watchLists) { //Note that storing the users is redundant
			//Peer settings
			Printer.log("Writing watchlist");
			
			Element watchList = doc.createElement("watchList");
			
			Element watchFiles = doc.createElement("watchFiles");
			watchList.appendChild(watchFiles);
			
			Element watchDirectories = doc.createElement("watchDirectories");
			watchList.appendChild(watchDirectories);
			
			Element userGUID = doc.createElement("userGUID");
			userGUID.setTextContent( 
					!(null == list.getUser() || null == list.getUser().getGUID()) ? 
							list.getUser().getGUID().toString() 
							: ""
			);
			watchList.appendChild(userGUID);
			
			for(WatchDirectory directory : list.getDirectories().values()){
				Printer.log("Writing directory "+directory.getGUID());
				
				if (!directory.isEmpty()) {
					Element directoryTag = directory.toElement(doc);
					watchDirectories.appendChild(directoryTag);
				}
			}
			
			for(WatchFile file : list.getFiles().values()){
				Printer.log("Writing file "+file.getGUID());
				
				if (!file.isEmpty()) {
					Element fileTag = file.toElement(doc);
					watchFiles.appendChild(fileTag);
				}
			}
			
			root.appendChild(watchList);
		}
		
		Printer.log("Done preparing watchlist document");
		doc.appendChild(root);
		
		return doc;		
	}
}

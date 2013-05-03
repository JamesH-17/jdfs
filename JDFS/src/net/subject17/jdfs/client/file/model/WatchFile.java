package net.subject17.jdfs.client.file.model;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import net.subject17.jdfs.client.io.Printer;
import net.subject17.jdfs.client.settings.reader.SettingsReader;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class WatchFile {
	private Path file;
	private UUID GUID;
	private int priority;
	
	public WatchFile(Element node){
		
		//We want to throw a nullPtrException if the node doesn't exist.
		file = Paths.get(SettingsReader.GetFirstNodeValue(node, "path"));
		
		try {
			String priorityEle = SettingsReader.GetFirstNodeValue(node, "priority");
			priority = priorityEle.equals("") ? 0 : Integer.parseInt(SettingsReader.GetFirstNodeValue(node, "priority"));
		} catch (NumberFormatException e){
			Printer.logErr(e);
			priority = 0;
		} 
		//Grab this file's uuid, and if it doesn't exist, toss an error
		String guid = SettingsReader.GetFirstNodeValue(node, "guid");
		if ( !(null == guid || guid.equals("")) )
			GUID = UUID.fromString(guid);
		else
			GUID = UUID.randomUUID();
	}
	public WatchFile(Path file) throws IOException {
		this(file, UUID.randomUUID());
	}
	public WatchFile(Path file, String guid) throws IOException {
		this(file, UUID.fromString(guid));
	}
	public WatchFile(Path file, UUID guid) throws IOException {
		this(file, guid, 0);
	}
	public WatchFile(Path file, UUID guid, int priority) throws FileNotFoundException {
		try {
			Printer.log("File: "+file.toRealPath());
			if (Files.exists(file) && !Files.isDirectory(file)) {
				this.file = file;
				this.GUID = guid;
				this.priority = priority;
			}
			else  //Let the null ptr exception happen if someone passed that in
				throw new FileNotFoundException("Invalid file -- either file ["+file+"] is a directory or it doesn't exist");
		} catch (IOException e) {
			throw new FileNotFoundException("Invalid file -- cannot get real path of ["+file+"].");
		}
	}
	
	public Path getPath() {return file;}
	public UUID getGUID() {return GUID;}
	public int getPriority(){return priority;}
	public int setPriority(int newPriority){return priority = newPriority;}
	public void setUUID(UUID newGUID) {this.GUID = newGUID;}
	
	public boolean isEmpty(){ return file == null || !Files.isRegularFile(file); }
	
	public final int hashCode() { return GUID.hashCode(); } //eventually, just make it Uniqe_identifier.hashCode()
	public Element toElement(Document doc) {

		Element fileTag = doc.createElement("file");
		
		Element filePathTag = doc.createElement("path");
		filePathTag.appendChild(doc.createTextNode(file.toString()));
		fileTag.appendChild(filePathTag);
		
		Element guidTag = doc.createElement("guid");
		guidTag.appendChild(doc.createTextNode(GUID.toString()));
		fileTag.appendChild(guidTag);
		
		Element priorityTag = doc.createElement("priority");
		priorityTag.appendChild(doc.createTextNode(priority+""));
		fileTag.appendChild(priorityTag);
		
		return fileTag;
	}
}

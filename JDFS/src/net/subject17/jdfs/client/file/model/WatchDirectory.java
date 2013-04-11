package net.subject17.jdfs.client.file.model;

import java.io.IOException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.UUID;

import net.subject17.jdfs.client.io.Printer;
import net.subject17.jdfs.client.settings.reader.SettingsReader;

import org.w3c.dom.Document;
import org.w3c.dom.Element;


public class WatchDirectory {
	private Path directory;
	private UUID GUID;
	public boolean followSubDirectories;
	
	public  WatchDirectory(Element e) {
		Element pathEle = SettingsReader.GetFirstNode(e, "path");
		Element followEle = SettingsReader.GetFirstNode(e, "followSubDirectories");
		Element guidEle = SettingsReader.GetFirstNode(e, "guid");
		
		String follow = null == followEle ? "false" : followEle.getTextContent().toLowerCase();
		String guid = null == guidEle ? "" : guidEle.getTextContent();
		
		directory = Paths.get(pathEle.getTextContent());
		followSubDirectories = follow.equals("true");
		GUID = guid.equals("") ? UUID.randomUUID() : UUID.fromString(guid);
	}
	public WatchDirectory(Path loc) throws FileSystemException{
		this(loc, false);
	}
	public WatchDirectory(Path path, boolean trackSubdirectories) throws FileSystemException {
		if (Files.isDirectory(path)){
			directory = path;
			followSubDirectories = trackSubdirectories;
			GUID = UUID.randomUUID();
		}
		else throw new FileSystemException("Invalid directory");
	}
	
	//Getters
	public final Path getDirectory() {return directory;}
	public final UUID getGUID(){return GUID;}
	public final boolean followSubdirectories() { return followSubDirectories; }
	
	public final boolean isEmpty() {
		return Files.isDirectory(directory);
	}
	public final void enableSubdirectoryTracking(){followSubDirectories = true;}
	public final void disabeSubdirectoryTracking(){followSubDirectories = false;}
	
	//
	public final HashSet<Path> getFilesToWatch() throws IOException {
		return getFilesToWatch(directory);
	}
	private final HashSet<Path> getFilesToWatch(Path loc) throws IOException { //TODO may have to extend this to take a level parameter
		final HashSet<Path> filesToWatch = new HashSet<Path>();
		//TODO watch out for symlinks!
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(loc)){
			for(Path path : stream){
				if(Files.isRegularFile(path))
					filesToWatch.add(path);
				else if (Files.isDirectory(path) && followSubDirectories)
					filesToWatch.addAll(getFilesToWatch(path)); //recurse
			} 
		} catch(DirectoryIteratorException e) {
			Printer.logErr("Error gettings paths in directory");
			Printer.logErr(e);
		}
		return filesToWatch;
	}

	//Overrides/Class implementation
	public final int hashCode() { return directory.hashCode(); }
	
	public final Element toElement(Document doc){
		Element directoryTag = doc.createElement("directory");
		
		Element directoryPathTag = doc.createElement("path");
		directoryPathTag.appendChild(doc.createTextNode(directory.toString()));
		directoryTag.appendChild(directoryPathTag);
		
		Element guidTag = doc.createElement("guid");
		guidTag.appendChild(doc.createTextNode(GUID.toString()));
		directoryTag.appendChild(guidTag);
		
		Element followSubsTag = doc.createElement("followSubDirectories");
		followSubsTag.appendChild(doc.createTextNode(followSubDirectories ? "true" : "false"));
		directoryTag.appendChild(followSubsTag);
		
		return directoryTag;
	}
}

package com.subject17.jdfs.client.file.model;

import java.io.IOException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;

import org.w3c.dom.Element;

import com.subject17.jdfs.client.io.Printer;
import com.subject17.jdfs.client.settings.reader.SettingsReader;

public class WatchDirectory {
	private Path directory;
	public boolean followSubDirectories;
	public WatchDirectory(Element e) {
		Element ele = SettingsReader.GetFirstNode(e, "");
		//Element ele = SettingsReader.GetFirstNode(e, "");
		directory = Paths.get(e.getTextContent());
		//followSubDirectories = e.l
	}
	public WatchDirectory(Path loc) throws FileSystemException{
		this(loc, false);
	}
	public WatchDirectory(Path path, boolean trackSubdirectories) throws FileSystemException {
		if (Files.isDirectory(path)){
			directory = path;
			followSubDirectories = trackSubdirectories;
		}
		else throw new FileSystemException("Invalid directory");
	}
	public final Path getDirectory() {return directory;}
	
	public final boolean isEmpty() {
		return Files.isDirectory(directory);
	}
	public final void enableSubdirectoryTracking(){followSubDirectories = true;}
	public final void disabeSubdirectoryTracking(){followSubDirectories = false;}
	public final boolean followSubdirectories() { return followSubDirectories; }
	
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
	
	public final int hashCode() { return directory.hashCode(); }
}

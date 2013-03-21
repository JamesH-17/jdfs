package com.subject17.jdfs.client.file.monitor.model;

import java.io.File;
import java.nio.file.FileSystemException;

import org.w3c.dom.Element;

import com.subject17.jdfs.client.file.handler.FileUtils;

public class WatchDirectory {
	File directory;
	boolean followSubDirectories;
	public WatchDirectory(Element e) {
		
		//Parse
	}
	public WatchDirectory(File loc) throws FileSystemException{
		this(loc, false);
	}
	public WatchDirectory(File loc, boolean trackSubdirectories) throws FileSystemException {
		if (!FileUtils.isValidDirectory(loc)) throw new FileSystemException("Invalid directory");
		else {
			directory = loc;
			followSubDirectories = trackSubdirectories;
		}
	}
	public File getDirectory() {return directory;}
	
	public boolean isEmpty() {
		return directory == null || directory.getPath().isEmpty();
	}
	public void enableSubdirectoryTracking(){followSubDirectories = true;}
	public void disabeSubdirectoryTracking(){followSubDirectories = false;}
}

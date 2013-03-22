package com.subject17.jdfs.client.file.monitor.model;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.w3c.dom.Element;

public class WatchFile {
	Path file;
	public WatchFile(Element e){
		//TODO parse element
	}
	public WatchFile(Path file) throws FileNotFoundException {
		if (Files.isRegularFile(file))
			this.file = file;
		else  //Let the null ptr exception happen if someone passed that in
			throw new FileNotFoundException("Invalid file -- either file"+file+" is a directory or it doesn't exist");
	}
	public Path getFile(){return file;}
}

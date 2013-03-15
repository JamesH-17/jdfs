package com.subject17.jdfs.client.file.monitor.model;

import java.io.File;

import org.w3c.dom.Element;

public class WatchFile {
	File file;
	public WatchFile(Element e){
		//TODO parse element
	}
	public WatchFile(File f) {
		file = f;
	}
	public File getFile(){return file;}
}

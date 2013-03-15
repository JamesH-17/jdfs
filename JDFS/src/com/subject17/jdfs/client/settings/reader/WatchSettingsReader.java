package com.subject17.jdfs.client.settings.reader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.subject17.jdfs.client.file.handler.FileUtils;
import com.subject17.jdfs.client.file.monitor.model.WatchList;
import com.subject17.jdfs.client.io.Printer;


/**
 * This class is used in reading in watch data for files	
 */
public class WatchSettingsReader extends SettingsReader {

	private File watchSettingsFile;
	
	private Document watchDoc;
	
	private ArrayList<WatchList> watchLists;
	
	public WatchSettingsReader(String pathloc, String fname) throws Exception {
		watchSettingsFile = new File(pathloc, fname);
		Init();
	}
	
	public WatchSettingsReader(File src) throws Exception {
		watchSettingsFile = src;
		Init();
	}
	
	private void Init() throws Exception {
		FileUtils.checkIfFileReadable(watchSettingsFile);
		watchDoc = getWatchDocument();
		readWatchLists();
	}
	
	//Getters
	public ArrayList<WatchList> getWatchLists() { return watchLists; }
	
	//
	private void readWatchLists() {
		watchLists = new ArrayList<WatchList>();
		
		NodeList lst = watchDoc.getElementsByTagName("watchList");
		for (int i = 0; i < lst.getLength(); ++i) {
			try {
				Element watchList = (Element)lst.item(i);
				WatchList watchLst = new WatchList(watchList);
				watchLists.add(watchLst);
			}
			catch (Exception e){
				Printer.logErr("Could not read Wath List, number "+i+" in list.");
				Printer.logErr(e);
			}
		}
	}
	
	//Utilities
	private Document getWatchDocument() throws ParserConfigurationException, SAXException, IOException {
		return GetDocument(watchSettingsFile);
	}

}

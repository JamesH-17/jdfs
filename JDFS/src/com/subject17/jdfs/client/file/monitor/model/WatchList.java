package com.subject17.jdfs.client.file.monitor.model;

import java.io.File;
import java.nio.file.FileSystemException;
import java.util.ArrayList;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.subject17.jdfs.client.account.AccountManager;
import com.subject17.jdfs.client.io.Printer;
import com.subject17.jdfs.client.user.User;

public class WatchList {
	private ArrayList<WatchDirectory> directories;
	private ArrayList<WatchFile> files;
	private User user;
	
	public WatchList(Element watchEle){
		files = new ArrayList<WatchFile>();
		directories = new ArrayList<WatchDirectory>();
		
		readDirectories(watchEle.getElementsByTagName("directory"));
		readFiles(watchEle.getElementsByTagName("file"));
		
		setUser(watchEle.getAttribute("account"));
	}
	private void setUser(String account) {
		if (AccountManager.accountExists(account))
			user = AccountManager.getUserByAccount(account);
	}
	public ArrayList<WatchDirectory> getDirectories() {return directories;}
	public ArrayList<WatchFile> getFiles() {return files;}
	public User getUser(){return user;}
	
	private void readDirectories(NodeList directoryNodes){
		for (int i = 0; i < directoryNodes.getLength(); ++i) {
			try {
				Element directoryTag = (Element)directoryNodes.item(i);
				WatchDirectory watchDir = new WatchDirectory(directoryTag);
				directories.add(watchDir);
			}
			catch (Exception e){
				Printer.logErr("Could not read Watch List, number "+i+" in list.");
				Printer.logErr(e);
			}
		}
	}
	
	private void readFiles(NodeList fileNodes){
		for (int i = 0; i < fileNodes.getLength(); ++i) {
			try {
				Element fileTag = (Element)fileNodes.item(i);
				WatchFile watchFile = new WatchFile(fileTag);
				files.add(watchFile);
			}
			catch (Exception e){
				Printer.logErr("Could not read Watch List, number "+i+" in list.");
				Printer.logErr(e);
			}
		}
	}
	
	public void AddDirectory(File directory) throws FileSystemException{
		directories.add(new WatchDirectory(directory));		
	}
	public void AddFile(File file){
		files.add(new WatchFile(file));
	}
}

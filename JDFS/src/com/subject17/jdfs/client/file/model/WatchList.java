package com.subject17.jdfs.client.file.model;

import java.nio.file.FileSystemException;
import java.nio.file.Path;
import java.util.HashSet;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.subject17.jdfs.client.account.AccountManager;
import com.subject17.jdfs.client.io.Printer;
import com.subject17.jdfs.client.user.User;

public class WatchList {
	private HashSet<WatchDirectory> directories;
	private HashSet<WatchFile> files;
	private User user;
	
	public WatchList(Element watchEle){
		resetFilesAndDirectories();
		
		readDirectories(watchEle.getElementsByTagName("directory"));
		readFiles(watchEle.getElementsByTagName("file"));
		
		setUser(watchEle.getAttribute("account"));
	}		
	public WatchList(User newUser) {
		resetFilesAndDirectories();
		user = newUser;
	}

	public final HashSet<WatchDirectory> getDirectories() {return directories;}
	public final HashSet<WatchFile> getFiles() {return files;}
	public final User getUser() {return user;}
	
	private final void setUser(String account) {
		if (AccountManager.accountExists(account))
			user = AccountManager.getUserByAccount(account);
	}
	
	private final void resetFilesAndDirectories(){
		files = new HashSet<WatchFile>();
		directories = new HashSet<WatchDirectory>();
	}
	private final void readDirectories(NodeList directoryNodes){
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
	private final void readFiles(NodeList fileNodes){
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
	
	public final boolean AddDirectory(Path directory, boolean trackSubdirectories) throws FileSystemException {
		return directories.add(new WatchDirectory(directory, trackSubdirectories));		
	}
	public final boolean AddFile(Path path) {
		try {
			return files.add(new WatchFile(path));
		} catch(Exception e){
			Printer.logErr("File not added");
			Printer.logErr(e);
			return false;
		}
	}
	
	public final boolean isEmpty(){ return !(hasWatchDirectories() || hasWatchFiles());  }
	public final boolean hasWatchDirectories() { return !(directories == null || directories.isEmpty());  }
	public final boolean hasWatchFiles() { return !(files == null || files.isEmpty()); }
}

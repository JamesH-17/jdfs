package net.subject17.jdfs.client.file.model;

import java.nio.file.FileSystemException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.UUID;

import net.subject17.jdfs.client.account.AccountManager;
import net.subject17.jdfs.client.io.Printer;
import net.subject17.jdfs.client.settings.reader.SettingsReader;
import net.subject17.jdfs.client.user.User;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


public class WatchList {
	private HashSet<WatchDirectory> directories;
	private HashSet<WatchFile> files;
	private User user;
	
	public WatchList(Element watchEle){
		resetFilesAndDirectories();
		
		readDirectories(watchEle.getElementsByTagName("directory"));
		readFiles(watchEle.getElementsByTagName("file"));
		try {
			setUser(UUID.fromString(SettingsReader.GetFirstNodeValue(watchEle,"userGUID")));
		} catch (IllegalArgumentException e){
			Printer.logErr(e);
			Printer.log("Watchlist user absent or invalid.  Setting user to null.");
			user = null;
		}
	}		
	public WatchList(User newUser) {
		resetFilesAndDirectories();
		user = newUser;
	}

	public final HashSet<WatchDirectory> getDirectories() {return directories;}
	public final HashSet<WatchFile> getFiles() {return files;}
	public final User getUser() {return user;}
	public final User setUser(User newUser) {return this.user = newUser;}
	
	private final void setUser(UUID userGUID) {
		if (AccountManager.getInstance().guidExists(userGUID))
			user = AccountManager.getInstance().getUserByGUID(userGUID);
	}
	
	private final void resetFilesAndDirectories(){
		files = new HashSet<WatchFile>();
		directories = new HashSet<WatchDirectory>();
	}
	private final void readDirectories(NodeList directoryNodes){
		for (int i = 0; null != directoryNodes && i < directoryNodes.getLength(); ++i) {
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
		for (int i = 0; null != fileNodes && i < fileNodes.getLength(); ++i) {
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
		} catch(Exception e) {
			Printer.logErr("File not added");
			Printer.logErr(e);
			return false;
		}
	}
	
	public final boolean isEmpty(){ return !(hasWatchDirectories() || hasWatchFiles());  }
	public final boolean hasWatchDirectories() { return !(directories == null || directories.isEmpty());  }
	public final boolean hasWatchFiles() { return !(files == null || files.isEmpty()); }
}
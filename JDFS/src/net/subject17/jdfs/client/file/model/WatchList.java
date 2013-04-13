package net.subject17.jdfs.client.file.model;

import java.nio.file.FileSystemException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

import net.subject17.jdfs.client.account.AccountManager;
import net.subject17.jdfs.client.io.Printer;
import net.subject17.jdfs.client.settings.reader.SettingsReader;
import net.subject17.jdfs.client.user.User;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


public class WatchList {
	//This is quite possibly one of the stupidest things I've ever seen in the Java API
	//Why isn't there a .get(Object o) method for HashSet?
	//Do I *really* have to wrap every single insertion of an Object into the set (logically, i want a set)
	//with "if (!set.containsKey(new Integer(o.hashCode())) set.put(new Integer(o.hashCode()), o);" ? 
	private HashMap<Integer, WatchDirectory> directories;
	private HashMap<Integer, WatchFile> files;
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

	public final HashMap<Integer, WatchDirectory> getDirectories() {return directories;}
	public final HashMap<Integer, WatchFile> getFiles() {return files;}
	public final User getUser() {return user;}
	public final User setUser(User newUser) {return this.user = newUser;}
	
	private final void setUser(UUID userGUID) {
		if (AccountManager.getInstance().guidExists(userGUID))
			user = AccountManager.getInstance().getUserByGUID(userGUID);
	}
	
	private final void resetFilesAndDirectories(){
		files = new HashMap<Integer, WatchFile>();
		directories = new HashMap<Integer, WatchDirectory>();
	}
	private final void readDirectories(NodeList directoryNodes){
		for (int i = 0; null != directoryNodes && i < directoryNodes.getLength(); ++i) {
			try {
				Element directoryTag = (Element)directoryNodes.item(i);
				WatchDirectory watchDir = new WatchDirectory(directoryTag);
				directories.put(new Integer(watchDir.hashCode()), watchDir);
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
				files.put(new Integer(watchFile.hashCode()), watchFile);
			}
			catch (Exception e){
				Printer.logErr("Could not read Watch List, number "+i+" in list.");
				Printer.logErr(e);
			}
		}
	}
	
	public final void AddDirectory(Path directory, boolean trackSubdirectories) throws FileSystemException {
		WatchDirectory temp = new WatchDirectory(directory, trackSubdirectories);
		directories.put(new Integer(temp.hashCode()), temp);		
	}
	public final boolean AddFile(Path path) {
		try {
			WatchFile temp = new WatchFile(path);
			files.put(new Integer(temp.hashCode()), temp);
			return true;
		} catch(Exception e) {
			Printer.logErr("File not added");
			Printer.logErr(e);
			return false;
		}
	}
	
	public final boolean isEmpty(){ return !(hasWatchDirectories() || hasWatchFiles());  }
	public final boolean hasWatchDirectories() { return !(directories == null || directories.isEmpty());  }
	public final boolean hasWatchFiles() { return !(files == null || files.isEmpty()); }
	
	public boolean setPriority(Path p, int newPriority) {
		WatchDirectory temp = directories.get(new Integer(p.hashCode()));
		if (null != temp) {
			temp.priority = newPriority;
			return true;
		}
		return false;
	}
}
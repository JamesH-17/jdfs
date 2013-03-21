package com.subject17.jdfs.client.file.monitor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.StandardWatchEventKinds;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.subject17.jdfs.client.file.handler.FileUtils;
import com.subject17.jdfs.client.file.monitor.model.WatchDirectory;
import com.subject17.jdfs.client.file.monitor.model.WatchList;
import com.subject17.jdfs.client.settings.reader.WatchSettingsReader;
import com.subject17.jdfs.client.user.User;

public class FileWatcher {

	private static File watchSettingsFile;
	private static WatchSettingsReader watchSettingsReader;
	
	private static User activeUser;
	private static WatchList activeWatchList;
	
	private static HashMap<User,WatchList> watchLists;
	private static ConcurrentHashMap<Path, WatchKey> watchKeys;
	
	private static WatchService watcher;
	
	
	public static void setWatchSettingsFile(File target) throws FileNotFoundException, IOException, ParserConfigurationException, SAXException {
		FileUtils.checkIfFileReadable(target);
		watchSettingsFile = target;
		watchSettingsReader = new WatchSettingsReader(watchSettingsFile);
		watchLists = watchSettingsReader.getAllWatchLists();
	}
	
	public static WatchList getWatchListByUser(User user){
		return getWatchListByUser(watchLists,user);
	}
	public static WatchList getWatchListByUser(HashMap<User,WatchList> haystack,User key){
		return haystack.get(key);
	}
	
	public static WatchList setActiveWatchList(User user){
		return activeWatchList = watchLists.get(activeUser = user);
	}
	
	public static boolean addWatchList(WatchList lst, User usr){
		if (!(lst == null || lst.isEmpty() || usr == null || usr.isEmpty())){
			watchLists.put(usr, lst);
			return true;
		} else return false;
	}
	
	//Add files to active watch list
	public static boolean addFileToWatchList(File file) {
		return addFileToWatchList(watchLists, activeUser, file);
	}
	public static boolean addFileToWatchList(User user, File file) {
		return addFileToWatchList(watchLists, user, file);
	}
	
	public static boolean addFileToWatchList(HashMap<User,WatchList> haystack,User user, File file){
		if 	(haystack==null || haystack.isEmpty() || user==null || user.isEmpty() || !haystack.containsKey(user))
			return false;
		else return haystack.get(user).AddFile(file); //TODO check out the reference tracking here
	}
	
	//Add files to active watch list
	public static boolean addDirectoryToWatchList(File directory) throws FileSystemException {
		return addDirectoryToWatchList(directory, false);
	}
	public static boolean addDirectoryToWatchList(File directory, boolean trackSubdirectories) throws FileSystemException { 
		return addDirectoryToWatchList(activeUser, directory, trackSubdirectories);
	}
	private static boolean addDirectoryToWatchList(User user, File directory, boolean trackSubdirectories) throws FileSystemException {
		return addDirectoryToWatchList(watchLists, user, directory, trackSubdirectories);
	}
	
	private static boolean addDirectoryToWatchList(HashMap<User,WatchList> haystack, User user, File directory, boolean trackSubdirectories) throws FileSystemException{
		if 	(haystack==null || haystack.isEmpty() || user==null || user.isEmpty() || !haystack.containsKey(user))
			return false;
		else return haystack.get(user).AddDirectory(directory, trackSubdirectories); //TODO check out the reference tracking here
	}
	
	private void initWatchService() throws IOException{
		watcher = FileSystems.getDefault().newWatchService();
	}
		
	private void registerAllFilesToWatchService() {
		assert(activeWatchList != null);
		for(WatchDirectory directory : activeWatchList.getDirectories()){
			Path p = FileSystems.getDefault().getPath(directory.getDirectory().getPath()); //TODO make directory take in a path
			p.register(watcher, ENTRY_CREATE,ENTRY_DELETE,ENTRY_MODIFY);
		}
	}
	
}

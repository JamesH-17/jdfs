package com.subject17.jdfs.client.file.monitor;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.subject17.jdfs.client.file.model.WatchDirectory;
import com.subject17.jdfs.client.file.model.WatchFile;
import com.subject17.jdfs.client.file.model.WatchList;
import com.subject17.jdfs.client.settings.reader.WatchSettingsReader;
import com.subject17.jdfs.client.settings.writer.WatchSettingsWriter;
import com.subject17.jdfs.client.user.User;
import com.subject17.jdfs.client.user.UserUtil;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

public class FileWatcher {

	private static volatile boolean isRunning = false; 
	private static Path watchSettingsFile;
	private static WatchSettingsReader watchSettingsReader;
	
	private static User activeUser;
	private static WatchList activeWatchList;
	
	private static HashMap<User,WatchList> watchLists; //I'm considering making a superclass that handles what to do with switching watchlists
														//Actually, it would be pretty easy to just put a watch file location for each user, and pass in the list here
	private static ConcurrentHashMap<WatchKey,Path> watchKeys; //The event we get back is a watchkey, so we index by that
	
	private static WatchService watcher;
	
	
	public final static void setWatchSettingsFile(Path target) throws FileNotFoundException, IOException, ParserConfigurationException, SAXException {
		if (!Files.isReadable(target)) {
			try {
				//TODO set up a writer in the watch reader class
			} catch(Exception e) {
				
			}
		}
		watchSettingsFile = target;
		watchSettingsReader = new WatchSettingsReader(watchSettingsFile);
		watchLists = watchSettingsReader.getAllWatchLists();		
	}
	
	public final static WatchList getWatchListByUser(User user){
		return getWatchListByUser(watchLists,user);
	}
	public final static WatchList getWatchListByUser(HashMap<User,WatchList> haystack,User key){
		return haystack.get(key);
	}
	
	public final static boolean setActiveWatchList(User user) throws IOException{ /* ----------------This is the magic function that inits everything else--------------------*/
		if (!UserUtil.isEmptyUser(user)){
			commitChangesToWatchlist();
			
			activeWatchList = watchLists.get(activeUser = user); //could be set to null here
			if (activeWatchList == null) { //This user doesn't have a watchlist!  Make a default!
				activeWatchList = new WatchList(user);
				watchLists.put(activeUser,activeWatchList);
			}
			initWatchService();
			registerAllFilesToWatchService();
			return true;
		}
		else return false;
		
	}
	
	public final static boolean addWatchList(WatchList lst, User usr){
		if (!(lst == null || lst.isEmpty() || usr == null || usr.isEmpty())){
			watchLists.put(usr, lst);
			return true;
		} else return false;
	}
	
	//Add files to active watch list
	public final static boolean addFileToWatchList(Path file) {
		return addFileToWatchList(watchLists, activeUser, file);
	}
	public final static boolean addFileToWatchList(User user, Path file) {
		return addFileToWatchList(watchLists, user, file);
	}
	
	public final static boolean addFileToWatchList(HashMap<User,WatchList> haystack,User user, Path file){
		if 	(haystack==null || haystack.isEmpty() || user==null || user.isEmpty() || !haystack.containsKey(user))
			return false;
		else return haystack.get(user).AddFile(file); //TODO check out the reference tracking here
	}
	
	//Add files to active watch list
	public final static boolean addDirectoryToWatchList(Path directory) throws FileSystemException {
		return addDirectoryToWatchList(directory, false);
	}
	public final static boolean addDirectoryToWatchList(Path directory, boolean trackSubdirectories) throws FileSystemException { 
		return addDirectoryToWatchList(activeUser, directory, trackSubdirectories);
	}
	private final static boolean addDirectoryToWatchList(User user, Path directory, boolean trackSubdirectories) throws FileSystemException {
		return addDirectoryToWatchList(watchLists, user, directory, trackSubdirectories);
	}
	
	private final static boolean commitChangesToWatchlist(){
		if(isRunning){
			WatchSettingsWriter writer = new WatchSettingsWriter();
			writer.writeWatchSettings(watchSettingsFile, watchLists.values());
			return true;
		}
		else return true;
	}
	
	private final static boolean addDirectoryToWatchList(HashMap<User,WatchList> haystack, User user, Path directory, boolean trackSubdirectories) throws FileSystemException{
		if 	(haystack==null || haystack.isEmpty() || user==null || user.isEmpty() || !haystack.containsKey(user))
			return false;
		else return haystack.get(user).AddDirectory(directory, trackSubdirectories); //TODO check out the reference tracking here
	}
	
	private final static void initWatchService() throws IOException {
		watcher = FileSystems.getDefault().newWatchService();
	}
		
	private final static void registerAllFilesToWatchService() throws IOException {
		assert(activeWatchList != null);
		
		//This function only handles the current watchlist
		
		//Register directories
		for(WatchDirectory directory : activeWatchList.getDirectories()){
			//First, put the directory on. (Keep in mind, we need to handle if new files are added to the directory, if the directory is deleted, or if it is moved)
			watchKeys.put(
					directory.getDirectory().register(watcher, ENTRY_CREATE,ENTRY_DELETE,ENTRY_MODIFY),
					directory.getDirectory()
			);
			for (Path path : directory.getFilesToWatch()){
				watchKeys.put(
						path.register(watcher, ENTRY_CREATE,ENTRY_DELETE,ENTRY_MODIFY),
						path
				);
			}
		}
		
		//Register files
		for(WatchFile file : activeWatchList.getFiles()){
			watchKeys.put(
					file.getFile().register(watcher, ENTRY_CREATE,ENTRY_DELETE,ENTRY_MODIFY),
					file.getFile()
			);
		}
	}
	
}

package net.subject17.jdfs.client.file.monitor;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.parsers.ParserConfigurationException;

import net.subject17.jdfs.JDFSUtil;
import net.subject17.jdfs.client.account.AccountManager;
import net.subject17.jdfs.client.file.db.DBManager;
import net.subject17.jdfs.client.file.db.DBManager.DBManagerFatalException;
import net.subject17.jdfs.client.file.model.WatchDirectory;
import net.subject17.jdfs.client.file.model.WatchFile;
import net.subject17.jdfs.client.file.model.WatchList;
import net.subject17.jdfs.client.io.Printer;
import net.subject17.jdfs.client.settings.reader.WatchSettingsReader;
import net.subject17.jdfs.client.settings.writer.WatchSettingsWriter;
import net.subject17.jdfs.client.user.User;
import net.subject17.jdfs.client.user.UserUtil;

import org.xml.sax.SAXException;


/**
 * 
 * @author James
 *	This class is responsible for managing the paths we're watching.
 *	Calling to the settings readers to read/export xml data,  adding
 *	new directories and paths, removing things from and adding them to
 *	the watch service are all handled here.
 *
 *	Actually processing any events that occur are handled by the 
 *	WatchEventDispatcher thread, which this class starts.
 */
public final class FileWatcher {

	private static volatile boolean isRunning = false; 
	private static Path watchSettingsFile;
	private static WatchSettingsReader watchSettingsReader;
	
	private static User activeUser;
	private static WatchList activeWatchList;
	
	//Keep in mind each watchList actually tracks some user data
	private static HashMap<User,WatchList> watchLists; //I'm considering making a superclass that handles what to do with switching watchlists
														//Actually, it would be pretty easy to just put a watch file location for each user, and pass in the list here
	private static ConcurrentHashMap<WatchKey,Path> watchKeys; //The event we get back is a watchkey, so we index by that
	
	private static WatchService watcher;
	private static Thread watchDispatcherThread; 
	
	
	public final static void setWatchSettingsFile(Path target) throws FileNotFoundException, IOException, ParserConfigurationException, SAXException {
		if (!Files.isReadable(target)) {
			try {
				WatchSettingsWriter.writeWatchSettings(target, watchLists.values());
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
	public final static void modifyUser(User oldUser, User newUser){
		watchLists.get(oldUser).setUser(newUser);
		WatchList temp = watchLists.remove(oldUser);
		watchLists.put(newUser, temp);
		
		if (activeUser.equals(oldUser))
			activeUser = newUser;
	}
	
	public final static boolean setActiveWatchList(User user) throws IOException{ /* ----------------This is the magic function that inits everything else--------------------*/
		if (!UserUtil.isEmptyUser(user)){
			commitChangesToWatchlist();
			
			activeWatchList = watchLists.get(activeUser = user); //could be set to null here
			AccountManager.getInstance().setActiveUser(user);
			
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
	
	public final static User getActiveUser(){
		return activeUser;
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
	
	private final static void commitChangesToWatchlist(){
		WatchSettingsWriter.writeWatchSettings(watchSettingsFile, watchLists.values());
	}
	
	private final static boolean addDirectoryToWatchList(HashMap<User,WatchList> haystack, User user, Path directory, boolean trackSubdirectories) throws FileSystemException{
		if 	(haystack==null || haystack.isEmpty() || user==null || user.isEmpty() || !haystack.containsKey(user))
			return false;
		else { 
			haystack.get(user).AddDirectory(directory, trackSubdirectories); //TODO check out the reference tracking here
			return true;
		}
	}
	
	private final static void initWatchService() throws IOException {
		watcher = FileSystems.getDefault().newWatchService();
	}
		
	private final static void registerAllFilesToWatchService() throws IOException, SQLException, DBManagerFatalException {
		assert(activeWatchList != null);
		
		//This function only handles the current watchlist
		
		//Register directories
		for(WatchDirectory directory : activeWatchList.getDirectories().values()){
			//First, put the directory on. (Keep in mind, we need to handle if new files are added to the directory, if the directory is deleted, or if it is moved)
			HashSet<Path> directoriesToWatch = directory.getDirectoriesToWatch();
			for (Path path : directoriesToWatch) {
				watchKeys.put(
						path.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY),
						path
				);
			}
			
			registerPathToDB(directoriesToWatch, directory);
		}
		
		//Register files
		for(WatchFile file : activeWatchList.getFiles().values()){
			watchKeys.put(
					file.getPath().register(watcher, ENTRY_CREATE,ENTRY_DELETE,ENTRY_MODIFY),
					file.getPath()
			);
			registerPathToDB(file);
		}	
	}
	
	private static void registerPathToDB(HashSet<Path> directoriesToWatch, WatchDirectory directories) throws SQLException, DBManagerFatalException {
		
		HashSet<Path> relativePathsToAdd = new HashSet<Path>(directoriesToWatch.size());
		
		for (Path canidatePath : directoriesToWatch) {
			relativePathsToAdd.add(directories.getDirectory().relativize(canidatePath));
		}
		
		
		try (ResultSet pathsFound = DBManager.getInstance().select("SELECT * FROM UserFiles "+
				//"INNER JOIN UserFileLinks ON UserFileLinks.UserFilePK = UserFiles.UserFilePK "+
			"WHERE UserFiles.ParentGUID LIKE '"+directories.getGUID()+"'")
		) {
			while (pathsFound.next()) {
				Path rowPath = Paths.get(pathsFound.getString("RelativeParentPath"));
				
				if (relativePathsToAdd.contains(rowPath)) {
					if (pathsFound.getInt("priority") != directories.priority) {
						DBManager.getInstance().upsert("UPDATE UserFiles SET priority = "+directories.priority+
								" WHERE UserFilePK LIKE '"+pathsFound.getString("UserFilePK")+"'"
						);
					}
					
					if (!pathsFound.getString("LocalFileName").equals(directories.getDirectory().resolve(rowPath).getFileName().toString())) {
						DBManager.getInstance().upsert("UPDATE UserFiles SET LocalFileName = '"+directories.getDirectory().resolve(rowPath).getFileName().toString()+"'"+
								" WHERE UserFilePK LIKE '"+pathsFound.getString("UserFilePK")+"'"
						);
					}
					
					if (!pathsFound.getString("LocalFilePath").equals(directories.getDirectory().resolve(rowPath).toString())) {
						DBManager.getInstance().upsert("UPDATE UserFiles SET LocalFilePath = '"+directories.getDirectory().resolve(rowPath).toString()+"'"+
								" WHERE UserFilePK LIKE '"+pathsFound.getString("UserFilePK")+"'"
						);
					}
					
					//Since it already exists, remove it from the set of paths to insert
					relativePathsToAdd.remove(rowPath);
				}
			}
		}
		
		for (Path pathToAdd : relativePathsToAdd) {
			try {
				DBManager.getInstance().upsert("INSERT INTO UserFiles(FileGUID, LocalFileName, LocalFilePath, LastUpdatedLocal, ParentGUID, RelativeParentPath, Priority) "+
					"VALUES ("+
						(pathToAdd.equals("") ? "'"+directories.getGUID()+"'," : "'',")+ // Don't give it a fileGuid unless it's the actual directory itself
						"'"+pathToAdd.getFileName()+ "',"+
						"'"+directories.getDirectory().resolve(pathToAdd)+ "',"+
						"'"+Files.getLastModifiedTime(pathToAdd, LinkOption.NOFOLLOW_LINKS).toString()+"',"+
						"'"+directories.getGUID()+"',"+
						"'"+pathToAdd+"',"+
						directories.priority+
					")"
				);
				
			} catch(IOException e){
				Printer.logErr("Exception encountered when registering path to db");
				Printer.logErr(e);
			}
		}
	}

	private static void registerPathToDB(WatchFile file) {
		try (ResultSet pathsFound = DBManager.getInstance().select("SELECT * FROM UserFiles "+
				//"INNER JOIN UserFileLinks ON UserFileLinks.UserFilePK = UserFiles.UserFilePK "+
			"WHERE UserFiles.FileGUID LIKE '"+file.getGUID()+"' AND (COALESCE(IV,'') LIKE '')")
		) {
			if (pathsFound.next()) {
				if (pathsFound.getInt("priority") != file.getPriority()) {
					DBManager.getInstance().upsert("UPDATE UserFiles SET priority = "+file.getPriority()+
							" WHERE UserFilePK LIKE '"+pathsFound.getString("UserFilePK")+"'"
					);
				}
			}
		}
		
		for (Path pathToAdd : relativePathsToAdd) {
			try {
				DBManager.getInstance().upsert("INSERT INTO UserFiles(FileGUID, LocalFileName, LocalFilePath, LastUpdatedLocal, ParentGUID, RelativeParentPath, Priority) "+
					"VALUES ("+
						(pathToAdd.equals("") ? "'"+directories.getGUID()+"'," : "'',")+ // Don't give it a fileGuid unless it's the actual directory itself
						"'"+pathToAdd.getFileName()+ "',"+
						"'"+directories.getDirectory().resolve(pathToAdd)+ "',"+
						"'"+Files.getLastModifiedTime(pathToAdd, LinkOption.NOFOLLOW_LINKS).toString()+"',"+
						"'"+directories.getGUID()+"',"+
						"'"+pathToAdd+"',"+
						directories.priority+
					")"
				);
				
			} catch(IOException e){
				Printer.logErr("Exception encountered when registering path to db");
				Printer.logErr(e);
			}
		}
		
	}

	public void removeWatchedPath(Path pathToRemove) {
		for (WatchKey key : JDFSUtil.getKeysByValue(watchKeys, pathToRemove)) {
			key.cancel();
			watchKeys.remove(key);
		}
	}
	
	public void startWatchEventDispatcher() {
		watchDispatcherThread = new Thread(new WatchEventDispatcher(watcher));
	}
	
	public void cleanUp() {
		if (watchDispatcherThread != null) {
			//watchDi
		}
	}
}

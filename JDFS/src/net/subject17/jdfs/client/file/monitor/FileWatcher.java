package net.subject17.jdfs.client.file.monitor;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;
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
import net.subject17.jdfs.client.settings.Settings;
import net.subject17.jdfs.client.settings.reader.WatchSettingsReader;
import net.subject17.jdfs.client.settings.writer.WatchSettingsWriter;
import net.subject17.jdfs.client.user.User;
import net.subject17.jdfs.client.user.User.UserException;
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
 *
 *  This class should reallllly be a singleton.  So much has to happen
 *  in the right order to avoid null pointers, state integrity, etc.
 *  
 *  
 *  Good luck if you're here.  This class is a friggin mess.
 */
public final class FileWatcher {

	private static volatile boolean isRunning = false; 
	private static Path watchSettingsFile;
	private static WatchSettingsReader watchSettingsReader;
	
	private static User activeUser;
	private static int userPK;
	private static WatchList activeWatchList;
	private static int machinePK = Settings.GetMachinePK();
	
	//Keep in mind each watchList actually tracks some user data
	private static HashMap<User,WatchList> watchLists; //I'm considering making a superclass that handles what to do with switching watchlists
														//Actually, it would be pretty easy to just put a watch file location for each user, and pass in the list here
	private static HashMap<WatchKey,Path> watchKeys; //The event we get back is a watchkey, so we index by that
	private static HashSet<Path> directoriesWithWatchedFile;
	private static HashSet<Path> watchedDirectories;
	private static HashSet<Path> watchedFiles;
	
	
	private static WatchService watcher;
	private static Thread watchDispatcherThread;
	private static WatchEventDispatcher watchDispatcher; 
	
	
	public final static void setWatchSettingsFile(Path target) throws FileNotFoundException, IOException, ParserConfigurationException, SAXException {
		if (!Files.isReadable(target)) {
			try {
				WatchSettingsWriter.writeWatchSettings(target, watchLists.values());
			} catch(Exception e) {
				Printer.log("File was not readable and we could not create a blank one");
				Printer.logErr(e);
			}
		}
		watchSettingsFile = target;
		watchSettingsReader = new WatchSettingsReader(watchSettingsFile);
		watchLists = watchSettingsReader.getAllWatchLists();
		
		//registerAllWatchlistsToDB();  //Should be handled in each watchlist now
		
		Printer.log("FileWatcher reset to values found at path "+target);
		Printer.log("Number of watchLists found: "+(null == watchLists ? "!!!!Null!!!!" : watchLists.size()));
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
	
	////////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * 
	 * @param user Watchlist to 
	 * @return
	 * @throws IOException
	 * @throws DBManagerFatalException
	 */
	public final static boolean setActiveWatchList(User user) throws IOException, DBManagerFatalException {
		/* ----------------This is the magic function that inits everything else--------------------*/
		Printer.log("Changing watchLists");
		if (!UserUtil.isEmptyUser(user)){
			deregisterWatchKeys();
			commitChangesToWatchlist();
			
			activeWatchList = watchLists.get(activeUser = user); //could be set to null here
			Printer.log("activeWatchList:"+activeWatchList);
			
			if (null == activeWatchList) { //This user doesn't have a watchlist!  Make a default!
				Printer.log("Initializing default watchlist for user");
				activeWatchList = new WatchList(user);
				watchLists.put(activeUser,activeWatchList);
			}
			
			setUserPK();
			removeDbEntriesForUser();
			AccountManager.getInstance().setActiveUser(user);
			
			initWatchService();
			registerAllFilesToWatchService();
			return true;
		}
		else return false;	
	}
	
	public final static User getActiveUser(){
		return activeUser;
	}
	
	public final static int setUserPK() throws DBManagerFatalException{
		return setUserPK(activeUser);
	}
	public final static int setUserPK(User user) throws DBManagerFatalException {
		
		userPK = -1;
		
		try (ResultSet pk = DBManager.getInstance().select(
				"SELECT UserPK FROM Users WHERE Users.UserGUID LIKE '"+user.getGUID()+"'"+
				" AND Users.UserName LIKE '"+user.getUserName()+"'"+
				" AND Users.AccountEmail LIKE '"+user.getAccountEmail()+"'"
			)
		) {
			if (pk.next()) {
				userPK = pk.getInt("UserPK");
				Printer.log("User PK set to "+userPK+" in FileWatcher");
				
				if (pk.next()) {
					Printer.logErr("Warning [in FileWatcher]:  More than one user UserPK exists for {GUID:"+
								user.getGUID()+", UserName:"+user.getUserName()+
								", Account:"+user.getAccountEmail()+"}",
								Printer.Level.Low
					);
				}
			}
			else {
				Printer.logErr("Warning [in FileWatcher]: user PK not found in db, continuing anyway, setting default to 1", Printer.Level.High);
			}
		} catch (SQLException e) {
			Printer.logErr("Failed to set user PK in file watcher, using default of -1");
			Printer.logErr(e);
		}
		 
		 return userPK;
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
	
	public final static boolean addFileToWatchList(HashMap<User,WatchList> haystack,User user, Path file) {
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
	
	private final static boolean addDirectoryToWatchList(HashMap<User,WatchList> haystack, User user, Path directory, boolean trackSubdirectories) throws FileSystemException{
		if 	(haystack==null || haystack.isEmpty() || user==null || user.isEmpty() || !haystack.containsKey(user))
			return false;
		else { 
			haystack.get(user).AddDirectory(directory, trackSubdirectories); //TODO check out the reference tracking here
			return true;
		}
	}
	
	private final static void commitChangesToWatchlist() {
		WatchSettingsWriter.writeWatchSettings(watchSettingsFile, watchLists.values());
	}
	
	private final static void initWatchService() throws IOException {
		watcher = FileSystems.getDefault().newWatchService();
	}
	/*  This is being implemented in each watchlist instead
	private final static void registerAllWatchlistsToDB() {				
		try {
			for (WatchList watchList : watchLists.values()) {
				
				for(WatchDirectory directory : watchList.getDirectories().values()){
					//First, put the directory on.
					HashSet<Path> directoriesToWatch = directory.getDirectoriesToWatch();
						registerPathToDB(directoriesToWatch, directory);
				}
				
				//Register files
				for(WatchFile file : watchList.getFiles().values()){
					registerPathToDB(file);
				}	
			}
		} catch (DBManagerFatalException e) {
			Printer.logErr("Error putting watchlists in DB");
			Printer.logErr(e);
		}
	}*/
		
	private final static void registerAllFilesToWatchService() throws IOException, DBManagerFatalException {
		assert(activeWatchList != null);
		
		//This function only handles the current watchlist
		Printer.log("Registering files to watch service");
		//Register directories
		
		try {
			for(WatchDirectory directory : activeWatchList.getDirectories().values()){
				//First, put the directory on. (Keep in mind, we need to handle if new files are added to the directory, if the directory is deleted, or if it is moved)
	
				//TODO this may fix our assumption of always track if we also register any returned subs
				//Well, not fix, but suck less
				ensureDirectoryRegistered(directory);
				
				for (Path path : directory.getOnlyDirectoriesToWatch()) {
					if (!directoriesWithWatchedFile.contains(path)) {
						watchKeys.put(
								path.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY),
								path
						);
						directoriesWithWatchedFile.add(path);
					}
					watchedDirectories.add(path);
				}
				
				registerPathToDB(directory.getAllFilesToWatch(), directory);
				//TODO was directory.getDirectoriesToWatch()
			}
		} catch(Exception e) {
			Printer.logErr("Error registering watchlist directories");
			Printer.logErr(e);
		}
		
		//Register files
		try {
			for(WatchFile file : activeWatchList.getFiles().values()){
				Path loc = file.getPath();
				if (!Files.isDirectory(loc))
					loc = loc.getParent();
				
				if (!directoriesWithWatchedFile.contains(file.getPath().getParent())) {
					watchKeys.put(
							loc.register(watcher, ENTRY_CREATE,ENTRY_DELETE,ENTRY_MODIFY),
							loc
					);
					directoriesWithWatchedFile.add(loc);
				}
				
				watchedFiles.add(file.getPath());
				
				registerPathToDB(file);
			}	
		} catch (Exception e) {
			Printer.logErr("Error registering watchlist files");
			Printer.logErr(e);
		}
	}
	
	private static void ensureDirectoryRegistered(WatchDirectory directory) {
		//Ensure integrity
		try (ResultSet pathsFound = DBManager.getInstance().select("SELECT TOP 1 DISTINCT * FROM UserFiles "+
				//"INNER JOIN UserFileLinks ON UserFileLinks.UserFilePK = UserFiles.UserFilePK "+
			"WHERE UserFiles.ParentGUID LIKE '"+directory.getGUID()+"'"+
			"AND UserFiles.RelativeParentPath LIKE ''"
			)
		) {
			if (pathsFound.next()) { //Ensure data integrity
				if (!directory.equals( Paths.get( pathsFound.getString("LocalFilePath") ) )) {
					//fix entry
					DBManager.getInstance().upsert("UPDATE UserFiles SET "+
							"LocalFilePath = '"+directory.getDirectory()+"', "+
							"LocalFileName = '', "+
							"Priority = "+directory.priority+" "+
							"WHERE UserFilePK = "+pathsFound.getInt("UserFilePK")
					);
				}
			}
			else {
				try (ResultSet keys =  DBManager.getInstance().upsert("INSERT INTO UserFiles(FileGUID, LocalFileName, LocalFilePath, LastUpdatedLocal, ParentGUID, RelativeParentPath, Priority) "+
						"VALUES ("+
							"'"+directory.getGUID()+"',"+ // Don't give it a fileGuid unless it's the actual directory itself
							"'',"+
							"'"+directory.getDirectory()+ "',"+
							"'',"+
							"'"+directory.getGUID()+"',"+
							"'',"+
							directory.priority+
						")"
					)
				) {
					if (keys.next()) {
						ensureUserFilePKLinked(keys.getInt("UserFilePK"));
					}
					else {
						Printer.logErr("Warning [in FileWatcher]:For some reason, no key added after successful insert to db");
					}
				}
				catch (SQLException e) {
					Printer.logErr("SQLException encountered when directory path to db");
					Printer.logErr("Path:  ["+directory.getDirectory()+"]");
					Printer.logErr(e, Printer.Level.High);
				}
			}
		}
		catch (SQLException | DBManagerFatalException e) {
			Printer.logErr(e);
		}
		
	}

	private final static void registerPathToDB(HashSet<Path> directoriesToWatch, WatchDirectory directories) throws DBManagerFatalException {
		
		HashSet<Path> relativePathsToAdd = new HashSet<Path>(directoriesToWatch.size());
		
		for (Path canidatePath : directoriesToWatch) {
			relativePathsToAdd.add(directories.getDirectory().relativize(canidatePath));
		}
		
		
		//Ensure integrity
		try (ResultSet pathsFound = DBManager.getInstance().select("SELECT DISTINCT * FROM UserFiles "+
				//"INNER JOIN UserFileLinks ON UserFileLinks.UserFilePK = UserFiles.UserFilePK "+
			"WHERE UserFiles.ParentGUID LIKE '"+directories.getGUID()+"'")
		) {
			while (pathsFound.next()) {
				Path rowPath = Paths.get(pathsFound.getString("RelativeParentPath"));
				
				if (relativePathsToAdd.contains(rowPath)) {
					try {
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
	
						ensureUserFilePKLinked(pathsFound.getInt("UserFilePK"));
						
						//Since it already exists, remove it from the set of paths to insert
						relativePathsToAdd.remove(rowPath);
					}
					catch (Exception e) {
						Printer.logErr("Error in file integrity check");
						Printer.logErr(e);
					}
				}
			}
		} catch (SQLException e) {
			Printer.logErr("SQLException encountered in FileWatcher");
			Printer.logErr(e);
		}
		
		for (Path pathToAdd : relativePathsToAdd) {
			Printer.log("Checking path "+pathToAdd);
			try (ResultSet keys =  DBManager.getInstance().upsert("INSERT INTO UserFiles(FileGUID, LocalFileName, LocalFilePath, LastUpdatedLocal, ParentGUID, RelativeParentPath, Priority) "+
					"VALUES ("+
						(pathToAdd.equals("") ? "'"+directories.getGUID()+"'," : "'',")+ // Don't give it a fileGuid unless it's the actual directory itself
						"'"+pathToAdd.getFileName()+ "',"+
						"'"+directories.getDirectory().resolve(pathToAdd)+ "',"+
						"'"+Files.getLastModifiedTime(pathToAdd).toString()+"',"+
						"'"+directories.getGUID()+"',"+
						"'"+pathToAdd+"',"+
						directories.priority+
					")"
				)
			) {
				if (keys.next()) {
					ensureUserFilePKLinked(keys.getInt("UserFilePK"));
				}
				else {
					Printer.logErr("Warning [in FileWatcher]:For some reason, no key added after successful insert to db");
				}
			}
			catch(IOException e){
				Printer.logErr("IOException encountered when registering path to db (probably from get file time)");
				Printer.logErr("Path:  ["+pathToAdd+"]");
				Printer.logErr(e);
			}
			catch (SQLException e) {
				Printer.logErr("SQLException encountered when registering path to db");
				Printer.logErr("Path:  ["+pathToAdd+"]");
				Printer.logErr(e, Printer.Level.High);
			}
		}
	}

	private static void registerPathToDB(WatchFile file) throws DBManagerFatalException {
		try (ResultSet pathsFound = DBManager.getInstance().select("SELECT DISTINCT * FROM UserFiles "+
				//"INNER JOIN UserFileLinks ON UserFileLinks.UserFilePK = UserFiles.UserFilePK "+
			"WHERE UserFiles.FileGUID LIKE '"+file.getGUID()+"' AND (COALESCE(IV,'') LIKE '')")
		) {
			if (pathsFound.next()) {
				if (pathsFound.getInt("priority") != file.getPriority()) {
					DBManager.getInstance().upsert("UPDATE UserFiles SET priority = "+file.getPriority()+
							" WHERE UserFilePK = "+pathsFound.getString("UserFilePK")
					);
				}
				ensureUserFilePKLinked(pathsFound.getInt("UserFilePK"));
			}
			else {
				Path pathToAdd = file.getPath();
				
				try (ResultSet newFilePK = DBManager.getInstance().upsert("INSERT INTO UserFiles(FileGUID, LocalFileName, LocalFilePath, LastUpdatedLocal, ParentGUID, RelativeParentPath, Priority) "+
						"VALUES ("+
							"'"+file.getGUID()+"'"+ // Don't give it a fileGuid unless it's the actual directory itself
							"'"+pathToAdd.getFileName()+ "',"+
							"'"+pathToAdd+"',"+
							"'"+Files.getLastModifiedTime(pathToAdd).toString()+"',"+
							"'',"+
							"'',"+
							file.getPriority()+
						")"
					)
				) {
					ensureUserFilePKLinked(pathsFound.getInt("UserFilePK"));
				}
			}
		}
		catch (SQLException e) {
			Printer.logErr("SQLException encountered while registering single file to watchlist database (update or insert)");
			Printer.logErr(e);
		}
		catch (IOException e) {
			Printer.logErr("IOException encountered while registering single file to watchlist database (insert)");
			Printer.logErr(e);
		}
	}
	
	private static void ensureUserFilePKLinked(int filePK) throws DBManagerFatalException {
		try (ResultSet linkedFiles = DBManager.getInstance().select("SELECT DISTINCT * FROM UserFileLinks "+
				"WHERE UserFileLinks.UserPK = "+userPK+" AND UserFileLinks.MachinePK = "+machinePK+
				" AND UserFileLinks.UserFilePK ="+filePK
			)
		) {
			
			if (!linkedFiles.next()) {
				//insert it
				DBManager.getInstance().upsert("INSERT INTO UserFileLinks(UserPK, UserFilePK, MachinePK) VALUES ("+userPK+","+filePK+","+machinePK+")");
				//not confirming if correct
			}
			
		} catch (SQLException e) {
			Printer.logErr("SQLException encountered in FileWatcher whilst ensuring user file pk is linked");
			Printer.logErr(e);
		}
	}	
	
	private static void removeAllFileLinks() throws SQLException, DBManagerFatalException {
		DBManager.getInstance().select("TRUNCATE TABLE UserFileLinks");
	}
	private static void removeDbEntriesForUser() throws DBManagerFatalException {
		removeDbEntriesForUser(userPK);
	}
	private static void removeDbEntriesForUser(int UserPK) throws DBManagerFatalException {
		try (ResultSet toDelete = DBManager.getInstance().delete(
				"SELECT * FROM UserFileLinks WHERE UserFilePK = "+UserPK
			)
		){
			while(toDelete.next()) {
				toDelete.deleteRow();
			}
		} catch (SQLException e) {
			Printer.logErr("Error encountered deleting file links for user with PK "+UserPK);
			Printer.logErr(e);
		}
	}

	public static void removeWatchedPath(Path pathToRemove) {
		for (WatchKey key : JDFSUtil.getKeysByValue(watchKeys, pathToRemove)) {
			key.cancel();
			watchKeys.remove(key);
		}
	}
	
	public static void startWatchEventDispatcher() {
		Printer.log("Dispatching watch service");
		
		if (null != watchDispatcherThread) {
			synchronized(watchDispatcherThread) {
				Printer.log("Overwriting old service");
				
				cleanUp();
				watchDispatcherThread = new Thread(watchDispatcher = new WatchEventDispatcher(watcher, activeUser, directoriesWithWatchedFile, watchedFiles, watchedDirectories));
				watchDispatcherThread.setDaemon(true);
				watchDispatcherThread.start();
			}
		}
		else {
			Printer.log("Starting new service");
			 
			watchDispatcherThread = new Thread(watchDispatcher = new WatchEventDispatcher(watcher, activeUser, directoriesWithWatchedFile, watchedFiles, watchedDirectories));
			watchDispatcherThread.setDaemon(true);
			watchDispatcherThread.start();
		}
	}
	
	public static void cleanUp() {
		if (watchDispatcherThread != null) {
			synchronized(watchDispatcherThread) {
				
			}
			watchDispatcherThread = null;
		}
		
		if (null != watchDispatcher) {
			synchronized(watchDispatcher) {
				watchDispatcher.stop();
				watchDispatcher = null;
			}
		}
	}

	public static void writeWatchListsToFile(Path watchSettingsPath) {
		try {
			Printer.log("Writing watch settings to file");
			WatchSettingsWriter.writeWatchSettings(
				watchSettingsPath,
				getAllWatchListsFromDB()
			);
		} catch (DBManagerFatalException e) {
			WatchSettingsWriter.writeWatchSettings(
				watchSettingsPath,
				watchLists.values()
			);
		}
	}
	
	public static HashSet<WatchList> getAllWatchListsFromDB() throws DBManagerFatalException {
		
		HashSet<WatchList> watchListsFound = new HashSet<WatchList>();
		
		try (ResultSet users = DBManager.getInstance().select("SELECT Distinct Users.* FROM Users")) {
			Printer.log("DB Grab");
			while (users.next()) {
				Printer.log("Next user grab");
				try {
					
					User user = new User(
							users.getString("UserName"),
							users.getString("AccountEmail"),
							UUID.fromString(users.getString("UserGUID"))
					);
					
					Printer.log("Currently on user "+user.getGUID());
					
					HashMap<Integer, WatchFile> watchFiles = getWatchFilesFromDBForUser(users.getInt("UserPK"));
					HashMap<Integer, WatchDirectory> watchDirs = getWatchDirectoriesFromDBForUser(users.getInt("UserPK"));
					
					Printer.log("About to write "+watchFiles.size()+" watchFiles and "+watchDirs.size()+" watchDirs for user "+user.getUserName());
					
					watchListsFound.add(new WatchList(user, watchFiles, watchDirs));
				}
				catch (UserException e) {
					
				}
			}
		} catch (SQLException e) {
			Printer.logErr(e);
		}
		
		return watchListsFound;
	}
	
	private static HashMap<Integer, WatchDirectory> getWatchDirectoriesFromDBForUser(int userPK) throws DBManagerFatalException {
			HashMap<Integer, WatchDirectory> userWatchDirectories = new HashMap<Integer, WatchDirectory>();
			try (ResultSet watchFiles = DBManager.getInstance().select(
					"SELECT Distinct UserFiles.* FROM UserFiles "+
					"INNER JOIN UserFileLinks ON UserFiles.UserFilePK = UserFileLinks.UserFilePK "+
					"WHERE UserFileLinks.UserPK = "+userPK+
					" AND COALESCE(UserFiles.ParentGUID,'') NOT LIKE '' "+
					" AND COALESCE(RelativeParentPath,'') LIKE ''"
				)
			) {
				Printer.log("directories db grab");
				while (watchFiles.next()) {
					Printer.log("next");
					WatchDirectory watchDir = new WatchDirectory(
							Paths.get( watchFiles.getString("LocalFilePath") ),
							UUID.fromString( watchFiles.getString("ParentGUID") ),
							watchFiles.getInt("priority"),
							true//Yep, we always assume they want it tracked.  Could search entire db for paths, and see if a subdirectory was added, but whatevs
						);
					
					Integer key = watchDir.getDirectory().hashCode();
					
					if (!userWatchDirectories.containsKey(key))
						userWatchDirectories.put( key, watchDir );
					
				}
				Printer.log("DB grab done");
			}
			catch (SQLException e) {
				Printer.logErr("Error getting watch directory from db for user w/ PK "+userPK);
				Printer.logErr(e);
			}
			return userWatchDirectories;
		}

	private static HashMap<Integer, WatchFile> getWatchFilesFromDBForUser(int userPK) throws DBManagerFatalException {
		
		HashMap<Integer, WatchFile> userWatchFiles = new HashMap<Integer, WatchFile>();
		
		try (ResultSet watchFiles = DBManager.getInstance().select(
				"SELECT Distinct UserFiles.* FROM UserFiles "+
				"INNER JOIN UserFileLinks ON UserFiles.UserFilePK = UserFileLinks.UserFilePK "+
				"WHERE UserFileLinks.UserPK = "+userPK+
				" AND COALESCE(UserFiles.ParentGUID,'') LIKE ''"
			)
		) {
			Printer.log("DB Files grab for user with PK "+userPK);
			while (watchFiles.next()) {
				Printer.log("next");
				try {
					WatchFile watchFile = new WatchFile(
						Paths.get( watchFiles.getString("LocalFilePath") ),
						UUID.fromString( watchFiles.getString("FileGUID") ),
						watchFiles.getInt("priority")
					);
					
					Integer key = watchFile.hashCode();
					
					if (!userWatchFiles.containsKey(key))
						userWatchFiles.put(key, watchFile);
					
				}
				catch (FileNotFoundException e) {
					Printer.logErr("Could not find file, so not adding watch file: "+watchFiles.getString("LocalFilePath"));
					Printer.logErr(e);
				}
			}
			Printer.log("DB file grab end");
		}
		catch (SQLException e) {
			Printer.logErr("Error getting watch files from db for user w/ PK "+userPK);
			Printer.logErr(e);
		}
		return userWatchFiles;
	}
	
	private static void deregisterWatchKeys() {
		if (watchKeys != null) {
			for (WatchKey key : watchKeys.keySet()) {
				key.cancel();
				watchKeys.remove(key);
			}
		}
		watchKeys = new HashMap<WatchKey,Path>();
		
		directoriesWithWatchedFile = new HashSet<Path>();
		watchedDirectories = new HashSet<Path>();
		watchedFiles = new HashSet<Path>();
	}

	public static void stopWatchEventDispatcher() {
		// TODO Auto-generated method stub
		if (null != watchDispatcher) {
			watchDispatcher.stop();
		}
		if (null != watchDispatcherThread) {
			watchDispatcherThread.interrupt();
		}
	}
}

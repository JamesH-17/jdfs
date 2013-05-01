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
	private static int userPK;
	private static WatchList activeWatchList;
	private static int machinePK = GetMachinePK();
	
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
	
	private static int GetMachinePK() {
		UUID machineGUID = Settings.getMachineGUIDSafe();
		machinePK = -1;
		Printer.log("Getting machine PK");
		try (ResultSet machinePKs = DBManager.getInstance().select("Select Distinct * FROM Machines WHERE Machines.MachineGUID LIKE '"+machineGUID+"'")) {
			if (machinePKs.next()) {
				machinePK = machinePKs.getInt("MachinePK");
				
				if (machinePKs.next()) {
					Printer.logErr("Warning [in FileWatcher]:  Multiple entries exist for MachinePK  for machine {GUID:"+machineGUID+"}.  Continuing with value of "+machinePK, Printer.Level.Low);
				}
			}
			else { //Key for this machine does not yet exist, so add it
				//machinePKs.close();
				try (ResultSet newMachinePK = DBManager.getInstance().upsert(
						"INSERT INTO Machines(MachineGUID) VALUES('"+machineGUID+"')"
					)
				){
					Printer.log("newMAch is closed"+newMachinePK.isClosed());
					//newMachinePK.beforeFirst();
					if (true) {//newMachinePK.next()) {
						//machinePK = newMachinePK.getInt("MachinePK");
						machinePK = DBManager.getInstance().upsert2(
								"INSERT INTO Machines(MachineGUID) VALUES('"+machineGUID+"')"
							);
					}
					else {
						Printer.logErr("There is an error in the program logic for adding the machine key.", Printer.Level.Extreme);
						Printer.logErr("Call to add machine PK ran without exception, yet no value for PK returned.", Printer.Level.Extreme);
						Printer.logErr("Forcibly closing program.", Printer.Level.Extreme);
						System.exit(-1);
					}
				} catch (SQLException e) {
					Printer.log("hurr");
					Printer.logErr("Warning [in FileWatcher]: SQLException encountered when grabbing PK for our machine {GUID:"+machineGUID+"}.  Potentially invalid program state", Printer.Level.High);
					Printer.logErr(e);
				}
			}
		} catch (SQLException e) {
			Printer.logErr("Warning [in FileWatcher]: SQLException encountered when grabbing PK for our machine {GUID:"+machineGUID+"}.  Potentially invalid program state", Printer.Level.High);
			Printer.logErr(e);
		} catch (DBManagerFatalException e) {
			Printer.logErr("Fatal exception encountered running DB.  Terminating program", Printer.Level.Extreme);
			Printer.logErr(e);
			System.exit(-1);
		} 
		
		return machinePK;
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
	public final static boolean setActiveWatchList(User user) throws IOException, DBManagerFatalException {
		/* ----------------This is the magic function that inits everything else--------------------*/
		if (!UserUtil.isEmptyUser(user)){
			commitChangesToWatchlist();
			
			activeWatchList = watchLists.get(activeUser = user); //could be set to null here
			setUserPK();
			removeDbEntriesForUser();
			AccountManager.getInstance().setActiveUser(user);
			
			if (null == activeWatchList) { //This user doesn't have a watchlist!  Make a default!
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
	
	public final static int setUserPK() throws DBManagerFatalException{
		return setUserPK(activeUser);
	}
	public final static int setUserPK(User user) throws DBManagerFatalException {
		
		userPK = -1;
		
		try (ResultSet pk = DBManager.getInstance().select(
				"SELECT UserPK FROM Users WHERE User.GUID LIKE '"+user.getGUID()+"'"+
				" AND User.UserName LIKE '"+user.getUserName()+"'"+
				" AND User.UserName LIKE '"+user.getAccountEmail()+"'"
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
		
	private final static void registerAllFilesToWatchService() throws IOException, DBManagerFatalException {
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
	
	private final static void registerPathToDB(HashSet<Path> directoriesToWatch, WatchDirectory directories) throws DBManagerFatalException {
		
		HashSet<Path> relativePathsToAdd = new HashSet<Path>(directoriesToWatch.size());
		
		for (Path canidatePath : directoriesToWatch) {
			relativePathsToAdd.add(directories.getDirectory().relativize(canidatePath));
		}
		
		
		try (ResultSet pathsFound = DBManager.getInstance().select("SELECT DISTINCT * FROM UserFiles "+
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

					ensureUserFilePKLinked(pathsFound.getInt("UserFilePK"));
					
					//Since it already exists, remove it from the set of paths to insert
					relativePathsToAdd.remove(rowPath);
				}
			}
		} catch (SQLException e) {
			Printer.logErr("SQLException encountered in FileWatcher");
			Printer.logErr(e);
		}
		
		for (Path pathToAdd : relativePathsToAdd) {
			try (ResultSet keys =  DBManager.getInstance().upsert("INSERT INTO UserFiles(FileGUID, LocalFileName, LocalFilePath, LastUpdatedLocal, ParentGUID, RelativeParentPath, Priority) "+
					"VALUES ("+
						(pathToAdd.equals("") ? "'"+directories.getGUID()+"'," : "'',")+ // Don't give it a fileGuid unless it's the actual directory itself
						"'"+pathToAdd.getFileName()+ "',"+
						"'"+directories.getDirectory().resolve(pathToAdd)+ "',"+
						"'"+Files.getLastModifiedTime(pathToAdd, LinkOption.NOFOLLOW_LINKS).toString()+"',"+
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
							" WHERE UserFilePK LIKE '"+pathsFound.getString("UserFilePK")+"'"
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
							"'"+Files.getLastModifiedTime(pathToAdd, LinkOption.NOFOLLOW_LINKS).toString()+"',"+
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
				DBManager.getInstance().upsert("INSERT INTO UserFileLinks(UserPK, UserFilePK) VALUES ("+userPK+","+filePK+","+machinePK+")");
				//not confirming if correct
			}
			
		} catch (SQLException e) {
			Printer.logErr("SQLException encountered in FileWatcher whilst ensuring user file pk is linked");
			Printer.logErr(e);
		}
	}
	
	private static void removeAllFileLinks() throws SQLException, DBManagerFatalException {
		//DBManager.getInstance().select("TRUNCATE TABLE UserFileLinks");
	}
	private static void removeDbEntriesForUser() throws DBManagerFatalException {
		removeDbEntriesForUser(userPK);
	}
	private static void removeDbEntriesForUser(int UserPK) throws DBManagerFatalException {
		try (ResultSet toDelete = DBManager.getInstance().select(
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

	public void removeWatchedPath(Path pathToRemove) {
		for (WatchKey key : JDFSUtil.getKeysByValue(watchKeys, pathToRemove)) {
			key.cancel();
			watchKeys.remove(key);
		}
	}
	
	public void startWatchEventDispatcher() {
		synchronized(watchDispatcherThread) {
			cleanUp();
			watchDispatcherThread = new Thread(new WatchEventDispatcher(watcher, activeUser));
		}
	}
	
	public void cleanUp() {
		synchronized(watchDispatcherThread) {
			if (watchDispatcherThread != null) {
				//clean up?
				watchDispatcherThread.stop();
				
				watchDispatcherThread = null;
			}
		}
	}
}

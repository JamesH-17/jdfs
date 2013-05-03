package net.subject17.jdfs.client.file.model;

import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.UUID;

import net.subject17.jdfs.client.account.AccountManager;
import net.subject17.jdfs.client.file.db.DBManager;
import net.subject17.jdfs.client.file.db.DBManager.DBManagerFatalException;
import net.subject17.jdfs.client.file.monitor.FileWatcher;
import net.subject17.jdfs.client.io.Printer;
import net.subject17.jdfs.client.settings.Settings;
import net.subject17.jdfs.client.settings.reader.SettingsReader;
import net.subject17.jdfs.client.user.User;
import net.subject17.jdfs.client.user.UserUtil;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


public final class WatchList {
	//This is quite possibly one of the stupidest things I've ever seen in the Java API
	//Why isn't there a .get(Object o) method for HashSet?
	//Do I *really* have to wrap every single insertion of an Object into the set (logically, i want a set)
	//with "if (!set.containsKey(new Integer(o.hashCode())) set.put(new Integer(o.hashCode()), o);" ? 
	private HashMap<Integer, WatchDirectory> directories;
	private HashMap<Integer, WatchFile> files;
	private User user;
	
	public WatchList(Element watchEle) throws DBManagerFatalException {
		resetFilesAndDirectories();
		
		//Grab user info
		try {
			String userGUID = SettingsReader.GetFirstNodeValue(watchEle, "userGUID");
			String userName = SettingsReader.GetFirstNodeValue(watchEle, "userAccount");
			String userAccount = SettingsReader.GetFirstNodeValue(watchEle, "userName");
			
			if (!userGUID.equals("")) {
				Printer.log("User GUID for watchlist: "+userGUID);
				setUser(UUID.fromString(userGUID));	
			}
			else if (!userAccount.equals("")) {
				Printer.log("User Account for watchlist: "+userGUID);
				setUser(AccountManager.getInstance().getUserByAccount(userAccount));
			}
			else if (!userName.equals("")) {
				Printer.log("User Name for watchlist: "+userGUID);
				setUser(AccountManager.getInstance().getUserByUserName(userAccount));
			}
			else user = null;
			
		} catch (IllegalArgumentException e){
			Printer.logErr(e);
			Printer.log("Watchlist user absent or invalid.  Setting user to null.");
			
			System.exit(-1);
			user = null;
		}
		
		
		if (!AccountManager.getInstance().ensureAccountExists(user)) {
			Printer.logErr("Could not ensure user exists.  User info not provided?", Printer.Level.High);
		}
		else {
			//Can't add directories and Files since they insert to the DB
			//get watch directories and files
			Printer.log("Reading files and directories from tag");
			
			readDirectories(watchEle.getElementsByTagName("directory"));
			readFiles(watchEle.getElementsByTagName("file"));
		}
		
		
		Printer.log("Directories found: "+directories.size(), Printer.Level.VeryLow);
		Printer.log("Files found: "+files.size(), Printer.Level.VeryLow);
	}
	
	public WatchList(User newUser) throws DBManagerFatalException {
		resetFilesAndDirectories();
		user = newUser;
		AccountManager.getInstance().ensureAccountExists(user);
	}

	public WatchList(User user, HashMap<Integer, WatchFile> watchFiles,
			HashMap<Integer, WatchDirectory> watchDirs) {
		this.directories = watchDirs;
		this.files = watchFiles;
		this.user = user;
	}
	
	public final HashMap<Integer, WatchDirectory> getDirectories() {return directories;}
	public final HashMap<Integer, WatchFile> getFiles() {return files;}
	public final User getUser() {return user;}
	public final User setUser(User newUser) {return this.user = newUser;}
	
	private final void setUser(UUID userGUID) {
		if (AccountManager.getInstance().guidExists(userGUID))
			user = AccountManager.getInstance().getUserByGUID(userGUID);
		else {
			Printer.logErr("User does not exist");
		}
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
				
				addDirectoryToDB(watchDir);
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
				addFileToDB(watchFile);
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
		
		addDirectoryToDB(temp);
	}
	private final boolean addDirectoryToDB(WatchDirectory temp) {
		
		int directoryPK;
		
		//First, make sure the parent/main directory exists in userfiles
		try (ResultSet existingDirectories = DBManager.getInstance().select("SELECT DISTINCT * FROM UserFiles WHERE UserFiles.ParentGUID LIKE '"+temp.getGUID()+"' AND UserFiles.RelativeParentPath LIKE ''")) {
			
			if (existingDirectories.next()) { //It's in here, just get the PK
				directoryPK = existingDirectories.getInt("UserFilePK");
			}
			else {
				//Gotta add it
				try (ResultSet resset = DBManager.getInstance().upsert("INSERT INTO "+
					"UserFiles(FileGUID, LocalFileName, LocalFilePath, ParentGUID, RelativeParentPath, Priority) "+
						"VALUES('','','"+
							temp.getDirectory()+"','"+
							temp.getGUID()+"','"+
							temp.getDirectory().relativize(temp.getDirectory())+ //Should be simply '' (empty string)
							"',"+temp.priority+
						")"
					)
				){
					resset.next();
					directoryPK = resset.getInt("UserFilePK");
				}
			}
			
			ensureUserLinkedToFile(directoryPK);
		}
		catch (SQLException | DBManagerFatalException e) {
			Printer.logErr(e);
			return false;
		}

		HashMap<Path, Path> relativeAndAbsolutePathsToAdd = new HashMap<Path, Path>();
		
		//Step 2:  Get relative paths, find ones that need to be added, preform integrity check on those that already exist
		try {
			
			for (Path path: temp.getAllFilesToWatch()) {
				Path relative = temp.getDirectory().relativize(path);
				if (!relativeAndAbsolutePathsToAdd.containsKey(relative) && !Files.isDirectory(path))
					relativeAndAbsolutePathsToAdd.put(relative, path);
			}
			
			//for (Path path : relativeAndAbsolutePathsToAdd.keySet()) {
				try (ResultSet existingPaths = DBManager.getInstance().select("SELECT DISTINCT * FROM UserFiles "+
					"WHERE UserFiles.ParentGUID LIKE '"+temp.getGUID()+"' ")//+
					//"AND UserFiles.RelativeParentPath LIKE '"+temp.getDirectory().relativize(path)+"'")
				) {
					//relative pa
					while(existingPaths.next()) {
						Path resultPath = Paths.get(existingPaths.getString("RelativeParentPath"));
						
						if (relativeAndAbsolutePathsToAdd.containsKey(resultPath)) {
							
							//Not doing an integrity check on LocalFileName column
							try {
								if (!relativeAndAbsolutePathsToAdd.get(resultPath).equals(Paths.get(existingPaths.getString("LocalFilePath")))) {
									//Gotta update DB
									DBManager.getInstance().upsert("UPDATE UserFiles SET LocalFilePath = '"+relativeAndAbsolutePathsToAdd.get(resultPath)+
											"', LocalFileName = '"+relativeAndAbsolutePathsToAdd.get(resultPath).getFileName()+
											"' WHERE UserFiles.UserFilePK = "+existingPaths.getInt("UserFilePK"));
								}
								
								
								ensureUserLinkedToFile(existingPaths.getInt("UserFilePK"));
								
								
								relativeAndAbsolutePathsToAdd.remove(resultPath);
							}
							catch(Exception e) {
								Printer.logErr(e);
							}
						}
					}
					
				}
				catch (SQLException | DBManagerFatalException e) {
					Printer.logErr(e);
				}
			//}
		} catch (IOException e) {
			Printer.logErr(e);
			return false;
		}
		
		for (Path relative : relativeAndAbsolutePathsToAdd.keySet()) {
			Path nonRelative = relativeAndAbsolutePathsToAdd.get(relative);
			
			try (ResultSet insertedFile = DBManager.getInstance().upsert(
					"INSERT INTO UserFiles(FileGUID, LocalFileName, LocalFilePath, LastUpdatedLocal, ParentGUID, RelativeParentPath, Priority) "+
					"VALUES('','"+
					nonRelative.getFileName()+"','"+
					nonRelative+"',"+
					getLastModifiedSafe(nonRelative)+
					",'"+
					temp.getGUID()+"','"+
					relative+"',"+
					temp.priority+
				")")
			) {
				insertedFile.next();
				linkFilePKToUser(insertedFile.getInt("UserFilePK"));
			}
			catch (SQLException | DBManagerFatalException e) {
				Printer.logErr(e);
				return false;
			}
		}
		
		return true;
	}
	
	public final boolean AddFile(Path path) {
		try {
			WatchFile temp = new WatchFile(path);
			files.put(new Integer(temp.hashCode()), temp);
			
			addFileToDB(temp);
			return true;
		} catch(Exception e) {
			Printer.logErr("File not added");
			Printer.logErr(e);
			return false;
		}
	}
	
	private final void addFileToDB(WatchFile temp) throws DBManagerFatalException {
		int filePK;
		
		try (ResultSet existingFiles = DBManager.getInstance().select("SELECT Distinct * FROM UserFiles WHERE UserFiles.FileGUID LIKE '"+temp.getGUID()+"'")) {
			if (existingFiles.next()) {
				filePK = existingFiles.getInt("UserFilePK");
			}
			else {
				
				try (ResultSet insertedFile = DBManager.getInstance().upsert(
					"INSERT INTO UserFiles(FileGUID, LocalFileName, LocalFilePath, LastUpdatedLocal, ParentGUID, RelativeParentPath, Priority) "+
					"VALUES('"+temp.getGUID()+"','"+
						temp.getPath().getFileName()+"','"+
						temp.getPath()+"',"+
						getLastModifiedSafe(temp.getPath())+
						",'','',"+
						temp.getPriority()+
					")"
				)) {
					insertedFile.next();
					filePK = insertedFile.getInt("UserFilePK");
				}
			}
			

			ensureUserLinkedToFile(filePK);
		}
		catch (SQLException e) {
			Printer.logErr("Error adding watchfile to db");
			Printer.logErr(e);
		}
	}
	
	/**
	 * @param p Path to get last modified time
	 * @return Formatted string -- "null" or "'YYYY-MM-DD HH:MM:SS'"
	 */
	private final String getLastModifiedSafe(Path p) {
		Timestamp lastMod = null;
		try {
			FileTime ft = Files.getLastModifiedTime(p);
			lastMod = new Timestamp(ft.toMillis());
		} catch (IOException e) {
			Printer.logErr("Error adding watchfile to db");
			Printer.logErr(e);
		}
		return (null == lastMod ? "null" : "'"+lastMod.toString()+"'");
	}
	
	private final void linkFilePKToUser(int userFilePK) throws SQLException, DBManagerFatalException {
		int userPK = UserUtil.getUserPK(user);
		assert(userPK >= 0);
		
		DBManager.getInstance().upsert("INSERT INTO UserFileLinks (UserFilePK, UserPK, MachinePK) VALUES ("+userFilePK+","+userPK+",'"+Settings.GetMachinePK()+"')");
	}
	
	private final void ensureUserLinkedToFile(int userFilePK) throws SQLException, DBManagerFatalException {
		try (ResultSet linkedFiles = DBManager.getInstance().select(
				"SELECT DISTINCT UserFiles.UserFilePK "+
				"FROM UserFiles "+
				"INNER JOIN UserFileLinks ON UserFiles.UserFilePK = UserFileLinks.UserFilePK "+
				"INNER JOIN Users ON Users.UserPK = UserFileLinks.UserPK "+
				"WHERE Users.UserGUID LIKE '"+user.getGUID()+"' "+
				"AND UserFiles.UserFilePK = "+userFilePK
			)
		) {
			if (!linkedFiles.next()) {
				linkFilePKToUser(userFilePK);
			}
		}
	}
	
	public final boolean isEmpty(){ return !(hasWatchDirectories() || hasWatchFiles());  }
	public final boolean hasWatchDirectories() { return !(directories == null || directories.isEmpty());  }
	public final boolean hasWatchFiles() { return !(files == null || files.isEmpty()); }
	
	public final boolean setPriority(Path p, int newPriority) {
		
		WatchFile tempFile = files.get(new Integer(p.hashCode()));
		
		if (null != tempFile) {
			tempFile.setPriority(newPriority);
			return true;
		}
		
		//else...
		WatchDirectory temp = directories.get(new Integer(p.hashCode()));
		
		if (null != temp) {
			temp.priority = newPriority;
			return true;
		}
		
		//Can't find, return false
		return false;
	}
}
package net.subject17.jdfs.client.file.model;

import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

import net.subject17.jdfs.client.account.AccountManager;
import net.subject17.jdfs.client.file.db.DBManager;
import net.subject17.jdfs.client.file.db.DBManager.DBManagerFatalException;
import net.subject17.jdfs.client.io.Printer;
import net.subject17.jdfs.client.settings.reader.SettingsReader;
import net.subject17.jdfs.client.user.User;
import net.subject17.jdfs.client.user.UserUtil;

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
		AccountManager.getInstance().ensureAccountExists(user);
	}		
	public WatchList(User newUser) {
		resetFilesAndDirectories();
		user = newUser;
		AccountManager.getInstance().ensureAccountExists(user);
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
		
		addDirectoryToDB(temp);
	}
	private boolean addDirectoryToDB(WatchDirectory temp) {
		
		int directoryPK;
		
		try (ResultSet existingDirectories = DBManager.getInstance().select("SELECT DISTINCT * FROM UserFiles WHERE UserFiles.ParentGUID LIKE '"+temp.getGUID()+"' AND UserFiles.RelativeParentPath LIKE ''")) {
			
			if (existingDirectories.next()) {
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
			
		}
		catch (SQLException | DBManagerFatalException e) {
			Printer.logErr(e);
			return false;
		}
		
		try {
			HashMap<Path, Path> relativeAndAbsolutePathsToAdd = new HashMap<Path, Path>();
			
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
							if (!relativeAndAbsolutePathsToAdd.get(resultPath).equals(Paths.get(existingPaths.getString("LocalFilePath")))) {
								//Gotta update DB
								DBManager.getInstance().upsert("UPDATE UserFiles SET LocalFilePath = '"+relativeAndAbsolutePathsToAdd.get(resultPath)+
										"', LocalFileName = '"+relativeAndAbsolutePathsToAdd.get(resultPath).getFileName()+
										"' WHERE UserFiles.UserFilePK = "+existingPaths.getInt("UserFilePK"));
							}
							
							
							try (ResultSet linkedFiles = DBManager.getInstance().select(
									"SELECT DISTINCT UsersFiles.UserFilePK "+
									"FROM UserFiles "+
									"INNER JOIN UserFileLinks ON UserFiles.UserFilePK = UserFileLinks.UserFilePK "+
									"INNER JOIN Users ON Users.UserPK = UserFileLinks.UserPK "+
									"WHERE Users.UserGUID LIKE '"+user.getGUID()+"' "+
									"AND UserFiles.UserFilePK = "+existingPaths.getInt("UserFilePK")
								)
							) {
								if (!linkedFiles.next()) {
									linkFilePKToUser(existingPaths.getInt("UserFilePK"));
								}
							}
							
							
							relativeAndAbsolutePathsToAdd.remove(resultPath);
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
		
		return true;
	}
	private void linkFilePKToUser(int userFilePK) throws SQLException, DBManagerFatalException {
		int userPK = UserUtil.getUserPK(user);
		assert(userPK >= 0);
		
		DBManager.getInstance().upsert("INSERT INTO UserFileLinks (UserFilePK, UserPK) VALUES ("+userFilePK+","+userPK+")");
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
	
	private void addFileToDB(WatchFile temp) {
		// TODO Auto-generated method stub
		
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
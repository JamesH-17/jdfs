package net.subject17.jdfs.client.settings;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.UUID;

import net.subject17.jdfs.JDFSUtil;

public abstract class Settings {
	protected static UUID MachineGUID = null;
	
	protected static final String defaultSettingsPathName = "settings.conf";
	//protected static final String defaultSettingsDirectory = System.getProperty("user.dir"); //Gets current directory
	protected static final String defaultSettingsDirectory = JDFSUtil.defaultDirectory; //Gets current directory
	protected static final String defaultPeersPathName = "Peers.xml";
	protected static final String defaultUserPathName = "Users.xml";
	protected static final String defaultWatchPathName = "FileWatch.xml";
	protected static final String defaultStorageDirectory = Paths.get(defaultSettingsDirectory,"storage").toString();
	
	protected Path settingsPath = Paths.get(defaultSettingsDirectory, defaultSettingsPathName);
	protected Path peerSettingsPath = Paths.get(defaultSettingsDirectory, defaultPeersPathName);
	protected Path userSettingsPath = Paths.get(defaultSettingsDirectory, defaultUserPathName);
	protected Path watchSettingsPath = Paths.get(defaultSettingsDirectory, defaultWatchPathName);
	protected Path storageDirectory = Paths.get(defaultStorageDirectory);
	
	
	public final void setAllPaths(HashMap<String,Path> mapping) throws IOException {
		settingsPath = (mapping.get("settingsPath") != null) ?
			 mapping.get("settingsPath") : Paths.get(defaultSettingsDirectory, defaultSettingsPathName);
			 
		setDefaultPathLocations();
		if (mapping.get("peerSettingsPath")!=null)
			peerSettingsPath = mapping.get("peerSettingsPath");
		if (mapping.get("userSettingsPath")!=null)
			userSettingsPath = mapping.get("userSettingsPath");
		if (mapping.get("watchSettingsPath")!=null)
			watchSettingsPath = mapping.get("watchSettingsPath");
		if (mapping.get("storageDirectory")!=null)
			storageDirectory = mapping.get("storageDirectory");
		setPaths(mapping);
	}
	
	public final void setDefaultPathLocations() throws IOException {
		String settingsPath = this.settingsPath.getParent().toString();
		setPeersPath(Paths.get(settingsPath, defaultPeersPathName));
		setUsersPath(Paths.get(settingsPath, defaultUserPathName));
		setWatchPath(Paths.get(settingsPath, defaultWatchPathName));
		setStorageDirectory(defaultStorageDirectory);
	}
	
	public final void setPaths(HashMap<String,Path> mapping) {
		if (mapping.get("peerSettingsPath")!=null)
			peerSettingsPath = mapping.get("peerSettingsPath");
		if (mapping.get("userSettingsPath")!=null)
			userSettingsPath = mapping.get("userSettingsPath");
		if (mapping.get("watchSettingsPath")!=null)
			watchSettingsPath = mapping.get("watchSettingsPath");
		if (mapping.get("storageDirectory") != null)
			storageDirectory = mapping.get("storageDirectory");
	}
	
	//Setters	
	//Set Path Names

	protected final void setSettingsPath(Path f) { settingsPath = f; }
	protected final void setPeersPath(Path f) { peerSettingsPath = f; }
	protected final void setWatchPath(Path f) { watchSettingsPath = f; }
	protected final void setUsersPath(Path f) { userSettingsPath = f; }	
	
	protected final void setSettingsFileName(String fname) throws IOException {settingsPath = setFileName(settingsPath,fname);}
	protected final void setUsersFileName(String fname) throws IOException {userSettingsPath = setFileName(userSettingsPath,fname);}
	protected final void setPeersFileName(String fname) throws IOException {peerSettingsPath = setFileName(peerSettingsPath,fname);}
	protected final void setWatchFileName(String fname) throws IOException {watchSettingsPath = setFileName(watchSettingsPath,fname);}
	
	protected final void setSettingsDirectory(String loc) throws IOException {settingsPath = setDirectory(settingsPath, loc);}
	protected final void setPeersDirectory(String loc) throws IOException {peerSettingsPath = setDirectory(peerSettingsPath, loc);}
	protected final void setUsersDirectory(String loc) throws IOException {userSettingsPath = setDirectory(userSettingsPath, loc);}
	protected final void setWatchDirectory(String loc) throws IOException {watchSettingsPath = setDirectory(watchSettingsPath, loc);}
	protected final void setStorageDirectory(String path) throws IOException {storageDirectory = Paths.get(path);}
	
	//Utilities
	private final Path setFileName(Path currFile, String newName) {
		return currFile.resolveSibling(newName);
	}
	private final Path setDirectory(Path file, String newPath) {
		return Paths.get(newPath).resolve(file.getFileName());
	}
	private final Path changeDirectory(Path oldPath, Path newPath) throws IOException {
		return oldPath.resolveSibling(newPath);
	}
	
	//Getters
	public final Path getSettingsPath() { return settingsPath; }
	public final Path getPeerSettingsPath() { return peerSettingsPath; }
	public final Path getUserSettingsPath() { return userSettingsPath; }
	public final Path getWatchSettingsPath() { return watchSettingsPath; }
	public final Path getStorageDirectory() { return storageDirectory; }
	
	public final UUID setMachineGUID(String guid){
		return setMachineGUID(UUID.fromString(guid));
	}
	public final UUID setMachineGUID(UUID guid){
		return MachineGUID = guid;
	}
	public static final UUID getMachineGUID(){return MachineGUID;}
}
package com.subject17.jdfs.client.settings;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public abstract class Settings {
	protected static final String defaultSettingsFileName = "settings.conf";
	protected static final String defaultSettingsFilePath = System.getProperty("user.dir"); //Gets current directory
	protected static final String defaultPeersFileName = "Peers.xml";
	protected static final String defaultUserFileName = "Users.xml";
	protected static final String defaultWatchFileName = "FileWatch.xml";
	protected static final String defaultStorageDirectory = new File(System.getProperty("user.dir"),"storage/").getPath();
	
	protected File settingsFile = new File(defaultSettingsFilePath, defaultSettingsFileName);
	protected File peerSettingsFile = new File(defaultSettingsFilePath, defaultPeersFileName);
	protected File userSettingsFile = new File(defaultSettingsFilePath, defaultUserFileName);
	protected File watchSettingsFile = new File(defaultSettingsFilePath, defaultWatchFileName);
	protected File storageDirectory = new File(defaultStorageDirectory);
	
	
	public void setAllFiles(HashMap<String,File> mapping) throws IOException {
		settingsFile = (mapping.get("settingsFile") != null) ?
			 mapping.get("settingsFile") : new File(defaultSettingsFilePath, defaultSettingsFileName);
			 
		setDefaultFileLocations();
		if (mapping.get("peerSettingsFile")!=null)
			peerSettingsFile = mapping.get("peerSettingsFile");
		if (mapping.get("userSettingsFile")!=null)
			userSettingsFile = mapping.get("userSettingsFile");
		if (mapping.get("watchSettingsFile")!=null)
			watchSettingsFile = mapping.get("watchSettingsFile");
		if (mapping.get("storageDirectory")!=null)
			storageDirectory = mapping.get("storageDirectory");
		setFiles(mapping);
	}
	
	public void setDefaultFileLocations() throws IOException {
		String settingsPath = settingsFile.getParent();
		setPeersFile(new File(settingsPath, defaultPeersFileName));
		setUsersFile(new File(settingsPath, defaultUserFileName));
		setWatchFile(new File(settingsPath, defaultWatchFileName));
		setStorageDirectory(defaultStorageDirectory);
	}
	
	public void setFiles(HashMap<String,File> mapping) {
		if (mapping.get("peerSettingsFile")!=null)
			peerSettingsFile = mapping.get("peerSettingsFile");
		if (mapping.get("userSettingsFile")!=null)
			userSettingsFile = mapping.get("userSettingsFile");
		if (mapping.get("watchSettingsFile")!=null)
			watchSettingsFile = mapping.get("watchSettingsFile");
		if (mapping.get("storageDirectory") != null)
			storageDirectory = mapping.get("storageDirectory");
	}
	
	//Setters	
	//Set File Names
	public void setSettingFilename(String fname) throws IOException {
		settingsFile = setFileName(settingsFile, fname);
	}
	protected void setPeersFilename(String fname) throws IOException {
		peerSettingsFile = setFileName(peerSettingsFile, fname);
	}

	public void setSettingsFile(File f) { settingsFile = f; }
	public void setPeersFile(File f) { peerSettingsFile = f; }
	public void setWatchFile(File f) { watchSettingsFile = f; }
	public void setUsersFile(File f) { userSettingsFile = f; }	
	
	public void setSettingsFileName(String fname) throws IOException {settingsFile = setFileName(settingsFile,fname);}
	public void setUsersFileName(String fname) throws IOException {userSettingsFile = setFileName(userSettingsFile,fname);}
	public void setPeersFileName(String fname) throws IOException {peerSettingsFile = setFileName(peerSettingsFile,fname);}
	public void setWatchFileName(String fname) throws IOException {watchSettingsFile = setFileName(watchSettingsFile,fname);}
	
	public void setSettingsDirectory(String loc) throws IOException {settingsFile = setFilePath(settingsFile, loc);}
	public void setPeersDirectory(String loc) throws IOException {peerSettingsFile = setFilePath(peerSettingsFile, loc);}
	public void setUsersDirectory(String loc) throws IOException {userSettingsFile = setFilePath(userSettingsFile, loc);}
	public void setWatchDirectory(String loc) throws IOException {watchSettingsFile = setFilePath(watchSettingsFile, loc);}
	public void setStorageDirectory(String path) throws IOException {storageDirectory = new File(path);}
	
	//Utilities
	public File setFileName(File file, String newName) {
		file = file.getParentFile();
		return new File(file, newName);
	}
	
	public File setFilePath(File file, String newPath) {
		return new File(newPath,file.getName());
	}
	
	public File setFileNameDefunct(File file, String newName) throws IOException {
		if (!file.isDirectory())
			file = file.getParentFile();
		if (!file.isDirectory())
			throw new IOException("Error:  no directory parent for file");
		return new File(file, newName);
	}
		
	public File setFilePathDefunct(File file, String newPath) throws IOException {
		String fname = (file == null || file.isDirectory()) ? "" : file.getName(); 
		file = new File(newPath);
		if (file.isDirectory())
			file = new File(file.getParent());
		if(!fname.equals(""))
			file = new File(file, fname);
		return file;
	}
	
	public File setDirectoryPath(File oldPath, File newPath) throws IOException {
		return new File(oldPath.getParentFile(),newPath.getPath());
	}
	
	//Getters
	public File getSettingsFile() { return settingsFile; }
	public File getPeerSettingsFile() { return peerSettingsFile; }
	public File getUserSettingsFile() { return userSettingsFile; }
	public File getWatchSettingsFile() { return watchSettingsFile; }
	public File getStorageDirectory() { return storageDirectory; }
	
}
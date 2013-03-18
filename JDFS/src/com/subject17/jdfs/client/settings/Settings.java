package com.subject17.jdfs.client.settings;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import com.subject17.jdfs.client.io.Printer;

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
	public void setStorageDirectory(String path) throws IOException {storageDirectory = setFilePath(storageDirectory, path);}
	
	//Utilities
	public File setFileName(File file, String newName) throws IOException {
		if (!file.isDirectory())
			file = file.getParentFile();
		if (!file.isDirectory())
			throw new IOException("Error:  no directory parent for file");
		return new File(file, newName);
	}
		
	public File setFilePath(File file, String newPath) throws IOException {
		Printer.println(defaultStorageDirectory);
		Printer.println("hr");
		Printer.println(file.getPath());
		Printer.println(newPath);
		String fname = (file == null || file.isDirectory()) ? "" : file.getName(); 
		file = new File(newPath);
		Printer.println(file.getPath());
		Printer.println("fname:"+fname);
		if (file.isDirectory())
			file = new File(file.getParent());
		Printer.println("getparent:"+file.getPath());
		if(!fname.equals(""))
			file = new File(file, fname);
		Printer.println(file.getPath());
		return file;
	}
	
	//Getters
	public File getSettingsFile() { return settingsFile; }
	public File getPeerSettingsFile() { return peerSettingsFile; }
	public File getUserSettingsFile() { return userSettingsFile; }
	public File getWatchSettingsFile() { return watchSettingsFile; }
	public File getStorageDirectory() { return storageDirectory; }
	
}
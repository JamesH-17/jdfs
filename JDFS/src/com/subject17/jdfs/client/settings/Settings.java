package com.subject17.jdfs.client.settings;

import java.io.File;

public abstract class Settings {
	protected final String defaultSettingsFileName = "settings.conf";
	protected final String defaultSettingsFilePath = "~/";
	protected final String defaultPeersFileName = "Peers.xml";
	protected final String defaultUserFileName = "Users.xml";
	protected final String defaultWatchFileName = "FileWatch.xml";
	protected final String defaultStorageDirectory = "~/prog/school/Senior Project/storage";
	
	protected static File settingsFile;
	protected static File peerSettingsFile;
	protected static File userSettingsFile;
	protected static File watchSettingsFile;
	protected static File storageDirectory;
	
	
	//Setters
	protected static void setPeersFile(File f){
		peerSettingsFile = f;
	}
	protected static void setUsersFile(File f){
		userSettingsFile = f;
	}
	protected static void setWatchFile(File f){
		watchSettingsFile = f;
	}
	//Set File Names
	protected static void setPeersFilename(String fname){
		peerSettingsFile = setFileName(peerSettingsFile, fname);
	}
	protected static void setUsersFilename(String fname){
		userSettingsFile = setFileName(userSettingsFile, fname);
	}
	protected static void setWatchFilename(String fname){
		watchSettingsFile = setFileName(watchSettingsFile, fname);
	}
	//Set File Locations
	protected static void setPeersDirectory(String loc){
		peerSettingsFile = setFilePathSafe(peerSettingsFile, loc);
	}
	protected static void setUsersDirectory(String loc){
		userSettingsFile = setFilePathSafe(userSettingsFile, loc);
	}
	protected static void setWatchDirectory(String loc){
		watchSettingsFile = setFilePathSafe(watchSettingsFile, loc);
	}
	protected static void setStorageDirectory(String path){
		storageDirectory = setFilePathSafe(storageDirectory, path);
	}
	
	//Utilities
	private static File setFileName(File file, String newName) {
		return new File(file.getPath(), newName);
	}
	
	private static File setFilePathSafe(File file, String newPath) {
		newPath = new File(newPath).getPath();
		if (new File(newPath).isDirectory()) {
			return file.isDirectory() ?
				new File(newPath) : 
				new File(newPath, file.getName());
		}
		else return file;
	}
	
	//Getters
	public static File getSettingsFile() {
		return settingsFile;
	}
	public static File getPeerSettingsFile() {
		return peerSettingsFile;
	}
	public static File getUserSettingsFile() {
		return userSettingsFile;
	}
	public static File getWatchSettingsFile() {
		return watchSettingsFile;
	}
	public static File getStorageDirectory() {
		return storageDirectory;
	}
}

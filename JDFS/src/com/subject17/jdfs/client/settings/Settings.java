package com.subject17.jdfs.client.settings;

import java.io.File;
import java.util.ArrayList;

public abstract class Settings {
	protected String settingsFileName;
	
	protected ArrayList<String> users;
	protected String currentUser;
	
	protected String peerFileLocation;
	
	protected boolean setSettingsFile(String newSettingsFilename) {
		try {
			if ((new File(newSettingsFilename)).exists()) {
				settingsFileName = newSettingsFilename;
				return true;
			}
			else return false;
		} catch (Exception e) {e.printStackTrace(); return false;}
	}
	
	protected boolean setPeerFile(String newPeerFilename) {
		try {
			if ((new File(newPeerFilename)).exists()) {
				peerFileLocation = newPeerFilename;
				return true;
			}
			else return false;
		} catch (Exception e) {e.printStackTrace(); return false;}
	}
}

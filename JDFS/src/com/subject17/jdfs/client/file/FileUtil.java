package com.subject17.jdfs.client.file;

import java.io.File;
import java.io.IOException;

import com.subject17.jdfs.JDFSUtil;

public class FileUtil {
	/**
	 * @param path the path to be evaluated
	 * @return returns true if the string represents a valid directory
	 * Returns true if the string represents a valid directory path on windows, unix, and mac
	 */
	public boolean isValidDirectory(String path){
		try {
			path = new File(path).getCanonicalPath();
			
			return isValidCrossPlatformDirectory(path);
		} catch(IOException e){return false;}
	}
	
	public boolean isValidDirectoryHelper(String path){
		switch (JDFSUtil.getOS()){
			case Linux: return isValidLinuxDirectory(path);
			case Windows: return isValidWindowsDirectory(path);
			case MAC: return isValidMacDirectory(path);
			default: return isValidCrossPlatformDirectory(path);
		}
	}

	private boolean isValidCrossPlatformDirectory(String path) {
		return path.matches("/*"); //TODO Wait, canonical will eff it up
	}

	private boolean isValidMacDirectory(String path) {
		// TODO Auto-generated method stub
		return false;
	}

	private boolean isValidWindowsDirectory(String path) {
		// TODO Auto-generated method stub
		return false;
	}

	private boolean isValidLinuxDirectory(String path) {
		// TODO Auto-generated method stub
		return false;
	}
}

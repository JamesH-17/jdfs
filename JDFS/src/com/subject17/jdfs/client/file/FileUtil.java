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
		boolean ret = true;
		try {
			path = new File(path).getCanonicalPath().toLowerCase();
			String[] paths = path.split(java.io.File.separator);
			
			for(String pth : paths){
				ret &= isValidXplatformName(pth);
			}
			
		} catch(IOException e){
			ret =  false;
		}
		return ret;
	}
	
	private boolean isValidXplatformName(String pth) {
		return !(pth.contains("<") || pth.contains(">") || pth.contains("|") || pth.contains(":") || pth.contains("*") || pth.contains("?"));
		//return pth.matches("/[]");
		///return pth.matches("[^<>\\*:\\|\\?]");
		//return !(pth.matches("<*>*:*\"[*]*|*"));
	}
	
	private boolean isValidRootWindows(String prefix) {
		return prefix.matches("[a-z]+:\\");
	}
}

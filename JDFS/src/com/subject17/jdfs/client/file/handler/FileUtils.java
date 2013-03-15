package com.subject17.jdfs.client.file.handler;

import java.io.File;

public class FileUtils {

	public static void checkIfFileReadable(File toCheck) throws Exception {
		if (!toCheck.isFile() || !toCheck.exists() || toCheck.isDirectory())
			throw new Exception("Provided paramater is not a valid file");
		if (!toCheck.canRead())
			throw new Exception("Cannot read from file "+toCheck.getAbsolutePath()+" for some reason");
	}
	
	public static boolean isValidDirectory(File loc){
		return (loc.isDirectory());
	}
	public static boolean isValidDirectory(String loc){
		return isValidDirectory(new File(loc));
	}
}

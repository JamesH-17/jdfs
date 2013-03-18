package com.subject17.jdfs.client.file.handler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class FileUtils {

	public static void checkIfFileReadable(File toCheck) throws FileNotFoundException, IOException {
		if (!toCheck.isFile() || !toCheck.exists() || toCheck.isDirectory())
			throw new FileNotFoundException("Provided paramater is not a valid file");
		if (!toCheck.canRead())
			throw new IOException("Cannot read from file "+toCheck.getAbsolutePath()+" for some reason");
	}
	
	public static boolean isValidDirectory(File loc){
		return (loc.isDirectory());
	}
	public static boolean isValidDirectory(String loc){
		return isValidDirectory(new File(loc));
	}
}

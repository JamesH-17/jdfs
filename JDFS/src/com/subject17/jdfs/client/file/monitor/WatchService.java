package com.subject17.jdfs.client.file.monitor;

import java.io.File;

public class WatchService {

	private static File watchSettingsFile;
	public static void setWatchSettingsFile(File target) {
		watchSettingsFile = target;
	}
	
}

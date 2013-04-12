package net.subject17.jdfs.client.net.sender;

import java.io.IOException;
import java.nio.file.Path;

import net.subject17.jdfs.client.file.FileUtil;
import net.subject17.jdfs.client.io.Printer;

public final class TalkerPooler {
	
	private static TalkerPooler _instance = null;
	
	private TalkerPooler(){}
	
	public static TalkerPooler getInstance() {
		if (null == _instance){
			synchronized(TalkerPooler.class){
				if (null == _instance){
					_instance = new TalkerPooler(); //Can't do easy instantiation since I wanna be able to throw that exception
				}
			}
		}
		return _instance;
	}
	
	
	//
	public String[] grabIP4s(){
		return new String[]{""};
	}
	
	
	
	private String jsonifyFileData(String pathGUID){
		//Connect to db, 
		return null;
	}



	public void UpdatePath(Path context) {
		//TODO Use DB to find every peer that has this file
		//Then use file sender to send it to them
		Printer.log("Path:");
		Printer.log("Got here");
	}
}

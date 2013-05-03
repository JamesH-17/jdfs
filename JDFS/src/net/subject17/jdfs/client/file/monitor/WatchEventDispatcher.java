package net.subject17.jdfs.client.file.monitor;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.StandardWatchEventKinds;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;

import javax.crypto.NoSuchPaddingException;

import net.subject17.jdfs.client.io.Printer;
import net.subject17.jdfs.client.net.sender.TalkerPooler;
import net.subject17.jdfs.client.user.User;
import net.subject17.jdfs.client.user.User.UserException;

public class WatchEventDispatcher implements Runnable {
		
		private static boolean run = true;
		private final static long timeBetweenHandlesInMillis = 1000;
		
		private final WatchService watcher;
		private final User user;
		
		private final HashSet<Path> directoriesWithWatchedFile;
		private final HashSet<Path> watchedDirectories;
		private final HashSet<Path> watchedFiles;
		
		
		public WatchEventDispatcher(WatchService watcher, User user, HashSet<Path> directoriesWithWatchedFile, HashSet<Path> watchedFiles, HashSet<Path> watchedDirectories) {
			this.watcher = watcher;
			this.user = user;
			this.directoriesWithWatchedFile = directoriesWithWatchedFile;
			this.watchedFiles = watchedFiles;
			this.watchedDirectories = watchedDirectories;
		}

		@Override
		public void run() {
		    WatchKey key;
			while (run) {
				// wait for key to be signaled
			    try {
			    	//TODO may want to make this poll every 60 seconds or something.  Alternatively, place thread.sleep at end of loop
			        key = watcher.take();
			        Path dir = (Path)key.watchable();
			        
			        if (run && key.isValid()) {
					    for (WatchEvent<?> event: key.pollEvents()) {
					        WatchEvent.Kind<?> kind = event.kind();
		
					        // This key is registered only
					        // for ENTRY_CREATE events,
					        // but an OVERFLOW event can
					        // occur regardless if events
					        // are lost or discarded.
					        if (kind == StandardWatchEventKinds.OVERFLOW) {
					        	Printer.log("Encountered OVERFLOW kind in watch service"+event);
					            continue;
					        }
		
					        // The filename is the
					        // context of the event.
					        @SuppressWarnings("unchecked")
							WatchEvent<Path> ev = (WatchEvent<Path>)event;
					        
					        try {
					        	
					        	//This is all this service ever does!
					        	if (!directoriesWithWatchedFile.contains(dir)) {
					        		//Can this ever happen?
					        	}
					        	
					        	if (watchedFiles.contains(ev.context()) || watchedDirectories.contains(dir)) {
					        		if (watchedDirectories.contains(dir)) {
					        			FileWatcher.addFileToWatchList(ev.context());
					        		}
					        	
					        	
					        		TalkerPooler.getInstance().UpdatePath(ev.context(), user);
					        	}
					        	//Else:  It shares a directory with a watched file, but it is not a child of a watched directory
							}
					        catch (InvalidKeyException
									| NoSuchAlgorithmException
									| NoSuchPaddingException | IOException
									| UserException e) {
								Printer.logErr("Error encountered in dispatching modified file ["+ev.context().toString()+"]",Printer.Level.High);
								Printer.logErr(e);
							}
					    }
		
					    // Reset the key -- this step is critical if you want to
					    // receive further watch events.  If the key is no longer valid,
					    // the directory is inaccessible so exit the loop.
					    boolean valid = key.reset();
					    Printer.log("Valid:"+valid, Printer.Level.VeryLow);
					    
					    if (!key.isValid()) {
					    	directoriesWithWatchedFile.remove(dir);
					    	removeAllFilesWatchedInDir(dir);
					    }
				    }
		        
					Thread.sleep(timeBetweenHandlesInMillis);
   
			    } catch (InterruptedException e) {
			    	Printer.logErr(e);
			    	
			        return; //really, we want return?
			    }
			}
		}
		
		private void removeAllFilesWatchedInDir(Path dir) {
			for (Path path : watchedDirectories)	{
				
				//There is no "canRelativize" functional equivalent in Path,
				//So I'm more or less forced to use exception handling for 
				//program control here
				
				try { 
					dir.relativize(path);
					watchedDirectories.remove(path);
				}
				catch (IllegalArgumentException e) {}
			}
			
			for (Path path : watchedFiles)	{
				
				//There is no "canRelativize" functional equivalent in Path,
				//So I'm more or less forced to use exception handling for 
				//program control here
				
				try { 
					dir.relativize(path);
					watchedFiles.remove(path);
				}
				catch (IllegalArgumentException e) {}
			}
			
			for(Path path : directoriesWithWatchedFile) {
				//There is no "canRelativize" functional equivalent in Path,
				//So I'm more or less forced to use exception handling for 
				//program control here
				
				try { 
					dir.relativize(path);
					directoriesWithWatchedFile.remove(path);
				}
				catch (IllegalArgumentException e) {}
			}
		}

		public void stop() {
			run = false;
		}
}

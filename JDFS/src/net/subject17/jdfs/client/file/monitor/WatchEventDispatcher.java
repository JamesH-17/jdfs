package net.subject17.jdfs.client.file.monitor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.StandardWatchEventKinds;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.NoSuchPaddingException;

import net.subject17.jdfs.client.io.Printer;
import net.subject17.jdfs.client.net.sender.TalkerPooler;
import net.subject17.jdfs.client.user.User.UserException;

public class WatchEventDispatcher implements Runnable {
		private final WatchService watcher;
		private boolean run = true;
		private final static long timeBetweenHandlesInMillis = 1000;
		
		WatchEventDispatcher(WatchService watcher) {
			this.watcher = watcher;
		}

		@Override
		public void run() {
		    WatchKey key;
			while (run) {
				// wait for key to be signaled
			    try {
			    	//TODO may want to make this poll every 60 seconds or something.  Alternatively, place thread.sleep at end of loop
			        key = watcher.take();
			        
			        if (key.isValid()) {
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
								TalkerPooler.getInstance().UpdatePath(ev.context());
							} catch (InvalidKeyException
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
				    }
		        
					Thread.sleep(timeBetweenHandlesInMillis);
   
			    } catch (InterruptedException e) {
			    	Printer.logErr(e);
			    	
			        return; //really, we want return?
			    }
			}
		}
		
		public void stop() {
			run = false;
		}
}

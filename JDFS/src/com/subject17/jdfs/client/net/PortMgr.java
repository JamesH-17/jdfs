package com.subject17.jdfs.client.net;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

public class PortMgr {
	
	public final static int defaultStartingPort = 1000;
	public final static int defaultLastPort = 65534;
	
	/**
	 * @author <a href="http://stackoverflow.com/users/92937/twentymiles">TwentyMiles</a>
	 * @see Code adpated from <a href="http://stackoverflow.com/a/13826145/1466964">Stack Overflow Comment</a>, liscensed under <a href="http://creativecommons.org/licenses/by-sa/3.0/">cc-wiki</a>
	 * @param port port to try
	 * @return true if port is available to use, false otherwise.
	 * @throws Throws IOException if closing the socket somehow fails
	 */
	public static boolean portIsAvailable(int port) {
	    System.out.println("--------------Testing port " + port);
	    Socket s = null;
	    try {
	        s = new Socket("localhost", port);

	        // If the code makes it this far without an exception it means
	        // something is using the port and has responded.
	        System.out.println("--------------Port " + port + " is not available");
	        return false;
	    } catch (IOException e) {
	        System.out.println("--------------Port " + port + " is available");
	        return true;
	    } finally {
	        if( s != null){
	            try {
	                s.close();
	            } catch (IOException e) {
	                throw new RuntimeException("You should handle this error." , e);
	            }
	        }
	    }
	}
	
	public static int getNextAvailablePort() throws Exception {
		return getNextAvailablePort(defaultStartingPort, defaultLastPort);
	}
	
	public static int getNextAvailablePort(int firstPortToTry, int lastPortToTry) throws Exception {
		for (int port=firstPortToTry; port<=lastPortToTry; ++port) {
			if (portIsAvailable(port))
				return port;
		}
		throw new Exception("Could not find port in range ["+firstPortToTry+"-"+lastPortToTry+"]");
	}
	
	public static int getNextAvailablePort(ArrayList<Integer> canidates) throws Exception {
		
		for(Integer canidate : canidates) {
			if (portIsAvailable(canidate))
				return canidate;
		}
		throw new Exception("Could not find port in list");
	}
	
	public static int getRandomPort() throws Exception {
		ArrayList<Integer> lst = new ArrayList<Integer>();
		for (int i = defaultStartingPort; i < defaultLastPort; ++i){
			lst.add(i); //I miss functional programming =[
		}
		java.util.Collections.shuffle(lst); //Modifies lst (lst is referenced)
		
		return getNextAvailablePort(lst);
	}
}
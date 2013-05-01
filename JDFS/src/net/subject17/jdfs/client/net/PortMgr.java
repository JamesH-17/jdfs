package net.subject17.jdfs.client.net;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

import net.subject17.jdfs.client.io.Printer;


public final class PortMgr {
	public final static int defaultStartingPort = 1000;
	public final static int defaultLastPort = 65534;
	private final static int defaultServerPort = 42419; //(First five digits of e+pi-phi)+1
	private static int serverPort = defaultServerPort;
	
	/**
	 * Code adapted from <a href="http://stackoverflow.com/a/13826145/1466964">Stack Overflow Comment</a>, liscensed under <a href="http://creativecommons.org/licenses/by-sa/3.0/">cc-wiki</a>
	 * @author <a href="http://stackoverflow.com/users/92937/twentymiles">TwentyMiles</a>
	 * @Liscence Share-Alike
	 * @param port port to try
	 * @return true if port is available to use, false otherwise.
	 * @throws Throws IOException if closing the socket somehow fails
	 */
	public static boolean portIsAvailable(int port) {
	    System.out.println("--------------Testing port " + port);
	    try (Socket s = new Socket("localhost", port))
	    {
	        // If the code makes it this far without an exception it means
	        // something is using the port and has responded.
	        Printer.log("--------------Port " + port + " is not available");
	        return false;
	    } catch (IOException e) {
	    	Printer.log("--------------Port " + port + " is available");
	        return true;
	    }
	}
	
	public static int getNextAvailablePort() throws PortMgrException {
		return getNextAvailablePort(defaultStartingPort, defaultLastPort);
	}
	
	public static int getNextAvailablePort(int firstPortToTry, int lastPortToTry) throws PortMgrException {
		for (int port = firstPortToTry; port <= lastPortToTry; ++port) {
			if (portIsAvailable(port))
				return port;
		}
		throw new PortMgrException("Could not find port in range ["+firstPortToTry+"-"+lastPortToTry+"]");
	}
	
	public static int getNextAvailablePort(ArrayList<Integer> canidates) throws PortMgrException {
		
		for(Integer canidate : canidates) {
			if (portIsAvailable(canidate))
				return canidate;
		}
		throw new PortMgrException("Could not find port in list");
	}
	
	public static int getRandomPort() throws PortMgrException {
		ArrayList<Integer> lst = new ArrayList<Integer>();
		for (int i = defaultStartingPort; i < defaultLastPort; ++i){
			lst.add(i); //I miss functional programming =[
		}
		java.util.Collections.shuffle(lst); //Modifies lst (lst is referenced)
		
		return getNextAvailablePort(lst);
	}
	
	public static int getServerPort() {
		return serverPort;
	}

	public static void setDefaultPort(int customDefaultPort) {
		serverPort = customDefaultPort;
	}
	
}

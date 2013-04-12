package net.subject17.jdfs.client.io;

/**
 * @author james
 * Abstracts the printing process, since this app can run in gui or console mode<br />
 * Also provides direct lines to each mode if needed
 */
public class Printer {
	
		public static void print(Object o) { print(o.toString()); }
		public static void print(String s) { System.out.print(s); }
		public static void println(String s) { System.out.println(s); }
		
		public static void log(Object o) { log(o.toString()); }
		public static void log(Object o, Level i) { log(o.toString(),i); }
		public static void log(String s) { log(s,Level.Medium); }
		public static void log(String s, Level i) {
			System.out.println(s);
		}

		public static void logErr(Object o) { logErr(o.toString()); }
		public static void logErr(Object o, Level i) { logErr(o.toString(), i); }
		public static void logErr(Exception e) {
			e.printStackTrace();
			logErr(e.getMessage());
		}
		public static void logErr(String s) { logErr(s, Level.Medium); }
		public static void logErr(String s, Level i) {
			System.err.println(s);
		}
		
		//Very low:  logs to file only
		//Low: Log only
		//Medium:  Non-blocking print to console (user can see as well)
		//High: Non-blocking print to console, print to new window
		//Extreme:  Blocking print to console, print to new window, or show stopping bug
		public enum Level {VeryLow, Low, Medium, High, Extreme}
}

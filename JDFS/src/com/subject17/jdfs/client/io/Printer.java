package com.subject17.jdfs.client.io;


/**
 * 
 * @author james
 * Abstracts the printing process, since this app can run in gui or console mode<br />
 * Also provides direct lines to each mode if needed
 */
public class Printer {
		
		public static void print(String s) {
			System.out.print(s);
		}
		public static void println(String s) {
			System.out.println(s);
		}
		
		public static void log(String s) {
			System.out.println(s);
		}
		public static void logErr(String s) {
			System.err.println(s);
		}
		public static void logErr(Exception e) {
			logErr(e.getMessage());
		}
}

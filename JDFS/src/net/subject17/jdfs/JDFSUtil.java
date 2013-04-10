package net.subject17.jdfs;

public class JDFSUtil {
	public enum OS {Windows, MAC, Linux, BSD, Unknown}
	public static OS getOS() {
		String os = System.getProperty("os.name");
		if (os.startsWith("Windows")) return OS.Windows;
		else if (os.startsWith("Mac")) return OS.MAC;
		else if (os.startsWith("Linux")) return OS.Linux;
		else if (os.startsWith("OpenBSD")||os.startsWith("NetBSD")) return OS.BSD;
		else return OS.Unknown;
		
	}
}

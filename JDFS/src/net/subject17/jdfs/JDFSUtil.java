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
	
	public static String stringJoin(Object[] Parts) {
		return stringJoin(Parts,",");
	}
	public static String stringJoin(Object[] objs, Object seperator) {
		StringBuilder builder = new StringBuilder();
		for(Object part : objs){
			builder.append(part.toString());
			builder.append(seperator.toString());
		}
		return builder.toString();
	}
	public static String stringJoin(Iterable<?> Parts) {
		return stringJoin(Parts,",");
	}
	public static String stringJoin(Iterable<?> Parts, Object seperator){
		StringBuilder builder = new StringBuilder();
		for(Object part : Parts){
			builder.append(part.toString());
			builder.append(seperator.toString());
		}
		return builder.toString();
	}
}

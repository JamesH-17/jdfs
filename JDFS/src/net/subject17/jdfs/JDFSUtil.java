package net.subject17.jdfs;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * 
 * @author James
 *	This class mostly contains stuff I wished the java API had written for me
 */
public class JDFSUtil {
	//public static final String defaultDirectory = "C:\\Users\\James\\Documents\\GitHub\\jdfs\\JDFS";
	public static final String defaultDirectory = System.getProperty("user.dir");
	
	public enum OS {Windows, MAC, Linux, BSD, Unknown}
	public static OS getOS() {
		String os = System.getProperty("os.name");
		if (os.startsWith("Windows")) return OS.Windows;
		else if (os.startsWith("Mac")) return OS.MAC;
		else if (os.startsWith("Linux")) return OS.Linux;
		else if (os.startsWith("OpenBSD")||os.startsWith("NetBSD")) return OS.BSD;
		else return OS.Unknown;
		
	}
	
	//String joins
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
	
	/*Inverse mappings for hash set
	 * Courtesy of the following stack overflow user:
	 * http://stackoverflow.com/users/288671/vitalii-fedorenko
	*/
	public static <T, E> Set<T> getKeysByValue(Map<T, E> map, E value) {
	     Set<T> keys = new HashSet<T>();
	     for (Entry<T, E> entry : map.entrySet()) {
	         if (value.equals(entry.getValue())) {
	             keys.add(entry.getKey());
	         }
	     }
	     return keys;
	}
	
	public static <T, E> T getKeyByValue(Map<T, E> map, E value) {
	    for (Entry<T, E> entry : map.entrySet()) {
	        if (value.equals(entry.getValue())) {
	            return entry.getKey();
	        }
	    }
	    return null;
	}
	
	public String toJSON(Object obj) throws JsonGenerationException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		return mapper.writeValueAsString(obj);
	}
	
	public static Class<?> fromJson(String json, Class<?> toValueType){
		ObjectMapper mapper = new ObjectMapper();
		return (Class<?>) mapper.convertValue(json, toValueType);
	}
	
	public static Object fromJsonClass(String json, Class<?> toValueType){
		ObjectMapper mapper = new ObjectMapper();
		return mapper.convertValue(json, toValueType);
	}
}

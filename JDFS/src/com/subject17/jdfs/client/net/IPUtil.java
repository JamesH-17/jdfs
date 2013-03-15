package com.subject17.jdfs.client.net;

public class IPUtil {
	public static boolean isValidIP6Address(String ip6){
		return !(ip6 == null || ip6.equals(""));
	}
	
	public static boolean isValidIP4Address(String ip4){
		return !(ip4 == null || ip4.equals(""));
	}
}

package net.subject17.jdfs.client.net;

public class IPUtil {
	
	//TODO extend this with regex
	
	public final static int ip6_num_bytes = 16;		//number of bytes to store the bits of "pure" ip6 address
	public final static int ip6_txt_no_colons = 32;		//
	public final static int ip6_txt_with_colons = 39;
	public final static int ip6_txt_with_ip4_tunnel = 45;
	
	public final static int ip4_num_bytes = 4;
	public final static int ip4_txt_no_dots = 12;
	public final static int ip4_txt_with_dots = 15;
	
	
	//This isn't a simple regex due to notations like ::ffff:127.0.0.1
	public static boolean isValidIP6Address(String ip6) {
		return !(ip6 == null || ip6.equals("")) && ip6.length() >= ip6_num_bytes;
	}
	
	public static boolean isValidIP4Address(String ip4){
		return !(ip4 == null || ip4.equals("")) && ip4.length() <= ip4_txt_with_dots;
	}
}

package com.subject17.jdfs.client.net;

public class LanguageProtocol {
	//General connection settings
	public final static String SYN="jdfs-Attempt-Connect";
	public final static String ACK="jdfs-Acknowledge-Connect";
	public final static String UNKNOWN="jdfs-Unknown-Communication";
	public final static String CLOSE="jdfs-Close-Connection";
	
	//Peer handling
	
	//Account querying
	public final static String REQUEST_ADD_ACCOUNT = "jdfs-Request-Add-My-Account";
	public final static String CONFIRM_ADD_ACCOUNT = "jdfs-Confirm-Your-Account-Added";
	public final static String DENY_ADD_ACCOUNT = "jdfs-Reject-Account-Add-Request";
	public final static String QUERY_ACCOUNT_EXISTS = "jdfs-Confirm-Account-Exists-On-Server";
	
	public static String handleResponse(String s) {
		switch (s) {
			case SYN: return ACK;
			default: return UNKNOWN;
		}
	}
}
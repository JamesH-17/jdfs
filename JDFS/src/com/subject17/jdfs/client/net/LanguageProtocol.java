package com.subject17.jdfs.client.net;

public class LanguageProtocol {
	//General connection settings
	public final static String SYN="jdfs-Attempt-Connect";
	public final static String ACK="jdfs-Acknowledge-Connect";
	public final static String UNKNOWN="jdfs-Unknown-Communication";
	public final static String CLOSE="jdfs-Close-Connection";
	public final static String ERROR="jdfs-Invalid-Response";
	
	public final static String RESEND_LAST="jdfs-Resend-Last-Transmission";
	
	//Peer handling
	
	//Account querying
	public final static String REQUEST_ADD_ACCOUNT = "jdfs-Request-Add-My-Account";
	public final static String CONFIRM_ADD_ACCOUNT = "jdfs-Confirm-Your-Account-Added";
	public final static String DENY_ADD_ACCOUNT = "jdfs-Reject-Account-Add-Request";
	public final static String QUERY_ACCOUNT_EXISTS = "jdfs-Confirm-Account-Exists-On-Server";
	
	//File Handling
	public final static String INIT_FILE_TRANS="jdfs-Initialize-File-Transfer";
	public final static String REFUSE_FILE_TRANS="jdfs-Refuse-File-Transfer";
	public final static String ACCEPT_FILE_TRANS="jdfs-Accept-File-Transfer";
	public final static String FILE_SIZE="jdfs-Initialize-File-Transfer";
	
	public static String handleResponse(String s) {
		if (s == null) return ERROR;
		
		switch (s) {
			case SYN: return ACK;
			default: return UNKNOWN;
		}
	}
}
package net.subject17.jdfs.client.net;

public class LanguageProtocol {
	//General connection settings
	public final static String SYN = "jdfs-Attempt-Connect";
	public final static String ACK = "jdfs-Acknowledge-Connect";
	public final static String UNKNOWN = "jdfs-Unknown-Communication";
	public final static String UNSUPPORTED = "jdfs-Unsupported-Communication";
	public final static String CLOSE = "jdfs-Close-Connection";
	public final static String ERROR = "jdfs-Invalid-Response";
	public final static String SKIPPED = "jdfs-Skipped-Operation";
	
	public final static String RESEND_LAST = "jdfs-Resend-Last-Transmission";
	
	//Peer handling
	
	//Account querying
	public final static String REQUEST_ADD_ACCOUNT = "jdfs-Request-Add-My-Account";
	public final static String CONFIRM_ADD_ACCOUNT = "jdfs-Confirm-Your-Account-Added";
	public final static String DENY_ADD_ACCOUNT = "jdfs-Reject-Account-Add-Request";
	public final static String QUERY_ACCOUNT_EXISTS = "jdfs-Confirm-Account-Exists-On-Server";
	public final static String LIST_FILES_STORED = "jdfs-Request-Files-Stored";
	
	//File Handling
	public final static String INIT_FILE_TRANS = "jdfs-Initialize-File-Transfer";
	public final static String REFUSE_FILE_TRANS = "jdfs-Refuse-File-Transfer";
	public final static String ACCEPT_FILE_TRANS = "jdfs-Accept-File-Transfer";
	public final static String FILE_SIZE = "jdfs-Initialize-File-Transfer";
	public final static String FILE_RECV_SUCC = "jdfs-File-Recieved-Successfully";
	public final static String FILE_RECV_FAIL = "jdfs-File-Recieved-Unsuccessfully";
	
	public final static String INIT_FILE_RETRIEVE = "jdfs-Initialize-File-Retrieve";
	public static final String FILE_SEND_FAIL = "jdfs-File-Send-Failure";
	public static final String FILE_SEND_SUCC = "jdfs-File-Send-Success";
	
	public static String handleResponse(String s) {
		if (s == null) return ERROR;
		
		switch (s) {
			case SYN: return ACK;
			default: return UNKNOWN;
		}
	}
	
	public static boolean keepGoing(String s){
		return !(null == s || s.equals("") || s.equals(LanguageProtocol.CLOSE));
	}
}
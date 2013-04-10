package net.subject17.jdfs.client.net;

public final class PortMgrException extends Exception {
	private static final long serialVersionUID = -4042928803471999027L;
	
	public PortMgrException(String msg) {
		super(msg);
	}
	public PortMgrException(String msg, Throwable thr) {
		super(msg,thr);
	}
}
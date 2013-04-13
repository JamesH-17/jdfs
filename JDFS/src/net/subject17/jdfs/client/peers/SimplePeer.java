package net.subject17.jdfs.client.peers;

import org.codehaus.jackson.annotate.JsonIgnore;

import net.subject17.jdfs.client.net.PortMgr;

public final class SimplePeer {
	public final String ip; //ip4 or 6
	public final int port;
	
	@JsonIgnore
	public SimplePeer(String ip){
		this.ip = ip;
		this.port = PortMgr.getServerPort();
	} 
	
}

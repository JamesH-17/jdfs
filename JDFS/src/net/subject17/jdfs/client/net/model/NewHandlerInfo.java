package net.subject17.jdfs.client.net.model;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

public class NewHandlerInfo {
	public String servername = ""; //Don't really know how to get our external ip.  Even then.... LAN?
	public final int port;
	
	@JsonIgnore
	public NewHandlerInfo(int port){
		this.port=port;
	}
	
	@JsonIgnore
	public String toJSON() throws JsonGenerationException, JsonMappingException, IOException{
		ObjectMapper mapper = new ObjectMapper();
		return mapper.writeValueAsString(this);
	}
}

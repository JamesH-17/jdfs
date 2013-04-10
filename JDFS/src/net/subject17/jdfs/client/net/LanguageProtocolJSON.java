package net.subject17.jdfs.client.net;

import java.io.IOException;

import net.subject17.jdfs.client.user.User;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;



public class LanguageProtocolJSON {
	private static ObjectMapper mapper = new ObjectMapper();
	
	public String jsonifyUser(User user) throws JsonGenerationException, JsonMappingException, IOException{
		return mapper.writeValueAsString(user);
	}
	
	public static String jsonify(Object obj) throws JsonGenerationException, JsonMappingException, IOException{
		return mapper.writeValueAsString(obj);
	}
}

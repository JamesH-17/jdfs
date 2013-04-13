package net.subject17.jdfs.client.net.sender;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import net.subject17.jdfs.client.account.AccountManager;
import net.subject17.jdfs.client.settings.Settings;
import net.subject17.jdfs.client.user.User;

public class MachineInfo {
	public final ArrayList<User> users;
	public final UUID MachineGUID;
	
	@JsonIgnore
	public MachineInfo(){
		users = AccountManager.getInstance().getUsers();
		MachineGUID = Settings.getMachineGUIDSafe();
	}
	@JsonIgnore
	public String toJSON() throws JsonGenerationException, JsonMappingException, IOException{
		ObjectMapper mapper = new ObjectMapper();
		return mapper.writeValueAsString(this);
	}
}

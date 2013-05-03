package net.subject17.jdfs.test.client.file;

import java.io.IOException;
import java.util.UUID;

import net.subject17.jdfs.client.io.Printer;
import net.subject17.jdfs.client.net.model.MachineInfo;
import net.subject17.jdfs.client.user.User;
import net.subject17.jdfs.client.user.User.UserException;

import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonMethod;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;

public class JAxonTEst {

	@Test
	public void test() {
		try {
			User temp = new User("test", "Test@test.com", UUID.randomUUID());
			
			Printer.log(temp);
			
			ObjectMapper mapper = new ObjectMapper().setVisibility(JsonMethod.FIELD, Visibility.ANY);;
			mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			String jsonString = mapper.writeValueAsString(temp);
			Printer.log(jsonString);
			
			User temp2 = mapper.readValue(jsonString, User.class);
			Printer.log(temp2);
			
			MachineInfo tempInfo = new MachineInfo();
			
			Printer.log(tempInfo);
			
			jsonString = mapper.writeValueAsString(temp);
			Printer.log(jsonString);
			
			MachineInfo tempInfo2 = mapper.readValue(jsonString, MachineInfo.class);
			Printer.log(tempInfo2);
			
		} catch (UserException | IOException e) {
			Printer.logErr(e);
		}
		
	}

}

package net.subject17.jdfs.client.file.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.UUID;

import net.subject17.jdfs.client.io.Printer;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

public final class FileRetrieverInfo {
	
	//For decryption
	public final String AESInitializationVector;
	
	//Identification
	@JsonIgnore
	public final Path fileLocation; //TODO we may want to store the sending machine version of this as well in case client wants default...?
	public final Path defaultLocation;
	
	public final UUID fileGuid;
	public final UUID userGuid;
	public final UUID sendingMachineGuid;
	
	//Meta data
	public final Date lastUpdatedDate;
	public final int priority;
	public final int size;
	public final String Checksum;
	
	//For directories only
	public final UUID parentGUID;
	public final Path parentLocation; //Resolved against TLD of watched dir 

	@JsonIgnore
	public FileRetrieverInfo(ResultSet fileFound) throws SQLException, IOException {
		fileLocation = Paths.get(fileFound.getString("LocalFilePath"));
		defaultLocation = Paths.get(fileFound.getString("PathOnClient"));
		
		size = (int) Files.size(fileLocation);
		
		
		String fileGUID = fileFound.getString("FileGUID");
		fileGuid = null == fileGUID || fileGUID.equals("") ? null : UUID.fromString(fileGUID);
		
		lastUpdatedDate = fileFound.getDate("UpdatedDate");
		//AESInitializationVector = ByteUtils.fromHexString(fileFound.getString("IV"));
		AESInitializationVector = fileFound.getString("IV");
		userGuid = UUID.fromString(fileFound.getString("UserGUID"));
		
		String machineGuid = fileFound.getString("MachineGUID");
		UUID tempMachineUUID = null;
		
		if (!( null == machineGuid || machineGuid.equals("") )) {
			try {
				tempMachineUUID = UUID.fromString(machineGuid);
			}
			catch (IllegalArgumentException e) {
				Printer.logErr("Server sending file:  Could not convert ["+machineGuid+"] to UUID.  Setting value to null and proceeding");
				Printer.logErr(e);
				tempMachineUUID = null;
			}
		}
		
		sendingMachineGuid = tempMachineUUID;
		
		String parentGuid = fileFound.getString("ParentGUID");
		UUID tempParentGuid = null;
		
		if (!( null == machineGuid || machineGuid.equals("") )) {
			try {
				tempParentGuid = UUID.fromString(parentGuid);
			} catch (IllegalArgumentException e) {
				Printer.logErr("Server sending file:  Could not convert ["+parentGuid+"] to UUID.  Setting value to null and proceeding");
				Printer.logErr(e);
				tempParentGuid = null;
			}
		}
		
		parentGUID = tempParentGuid;
		parentLocation = Paths.get(fileFound.getString("ParentLocation"));
		
		//Checksum = ByteUtils.fromHexString(fileFound.getString("CheckSum"));
		Checksum = fileFound.getString("CheckSum");
		priority = fileFound.getInt("Priority");
	}

	/*
	public FileRetrieverInfo(String json) {
		ObjectMapper mapper = new ObjectMapper();
		this = mapper.readValue(json, FileRetrieverInfo.class);
	}
*/
	@JsonIgnore
	public String toJSON() throws JsonGenerationException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		return mapper.writeValueAsString(this);
	}

}

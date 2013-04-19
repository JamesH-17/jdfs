package net.subject17.jdfs.client.file.model;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Date;
import java.util.UUID;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

public final class FileSenderInfo {
	//For decrypting.  THE MOST IMPORTANT VARIABLE HERE!
	public final byte[] AESInitializationVector;
	
	//Identification
	public final Path fileLocation;
	public final UUID fileGuid;
	public final UUID userGuid;
	public final UUID sendingMachineGuid;
	
	//Meta data
	public final Date lastUpdatedDate;
	public final int priority;
	public final int size;
	public final byte[] Checksum;
	
	//For directories only
	public final UUID parentGUID;
	public final Path parentLocation; //Resolved against TLD of watched dir 
	
	@JsonIgnore
	public final Path encryptedFileLocation;
	
	@JsonIgnore
	public FileSenderInfo(	EncryptedFileInfoStruct info, 
							Path fileLocation,
							UUID fileGuid,
							UUID userGuid,
							UUID sendingMachineGuid,
							Date lastUpdatedDate,
							int priority,
							byte[] CheckSum
	){
		this.AESInitializationVector = info.IV;
		this.fileLocation = fileLocation;
		this.fileGuid = fileGuid;
		this.userGuid = userGuid;
		this.sendingMachineGuid = sendingMachineGuid;
		this.lastUpdatedDate = lastUpdatedDate;
		this.priority=priority;
		this.size = info.size;
		this.Checksum = CheckSum;
		this.parentGUID = null;
		this.parentLocation = fileLocation;
		this.encryptedFileLocation = info.fileLocation;
	}
	
	@JsonIgnore
	public FileSenderInfo(	EncryptedFileInfoStruct info, 
							Path fileLocation,
							UUID fileGuid,
							UUID userGuid,
							UUID sendingMachineGuid,
							Date lastUpdatedDate,
							int priority,
							byte[] CheckSum,
							UUID parentGUID,
							Path parentLocation
	){
		this.AESInitializationVector = info.IV;
		this.fileLocation = fileLocation;
		this.fileGuid = fileGuid;
		this.userGuid = userGuid;
		this.sendingMachineGuid = sendingMachineGuid;
		this.lastUpdatedDate = lastUpdatedDate;
		this.priority=priority;
		this.size = info.size;
		this.Checksum = CheckSum;
		this.parentGUID = parentGUID;
		this.parentLocation = parentLocation;
		this.encryptedFileLocation = info.fileLocation;
	}
	
	@JsonIgnore
	public String getAsJSON() throws JsonGenerationException, JsonMappingException, IOException {
	    ObjectMapper mapper = new ObjectMapper();
	    return mapper.writeValueAsString(this) ; 
	}
}

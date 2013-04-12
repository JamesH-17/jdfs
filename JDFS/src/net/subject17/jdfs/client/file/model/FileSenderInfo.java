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
	public final byte[] AESInitializationVector;
	public final Path fileLocation;
	public final UUID fileGuid;
	public final UUID userGuid;
	public final UUID sendingMachineGuid;
	public final Date lastUpdatedDate;
	public final int priority;
	public final int size;
	public final byte[] Checksum;
	
	
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
	}
	@JsonIgnore
	public FileSenderInfo(	byte[] AESInitializationVector, 
							Path fileLocation,
							UUID fileGuid,
							UUID userGuid,
							UUID sendingMachineGuid,
							Date lastUpdatedDate,
							int priority,
							int fileSize,
							byte[] CheckSum
	){
		this.AESInitializationVector = AESInitializationVector;
		this.fileLocation = fileLocation;
		this.fileGuid = fileGuid;
		this.userGuid = userGuid;
		this.sendingMachineGuid = sendingMachineGuid;
		this.lastUpdatedDate = lastUpdatedDate;
		this.priority=priority;
		this.size = fileSize;
		this.Checksum = CheckSum;
	}
	
	@JsonIgnore
	public String getAsJSON() throws JsonGenerationException, JsonMappingException, IOException {
	    ObjectMapper mapper = new ObjectMapper();
	    return mapper.writeValueAsString(this) ; 
	}
}

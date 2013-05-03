package net.subject17.jdfs.client.file.model;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Date;
import java.util.UUID;

import org.bouncycastle.pqc.math.linearalgebra.ByteUtils;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

public final class FileSenderInfo {
	//For decrypting.  THE MOST IMPORTANT VARIABLE HERE!
	public final String AESInitializationVector;
	
	//Identification
	public final String fileLocation;
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
	public final String locationRelativeToParent; //Resolved against TLD of watched dir 
	
	@JsonIgnore
	public final String encryptedFileLocation; //Used by sender
	
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
		this.AESInitializationVector = ByteUtils.toHexString(info.IV);
		this.fileLocation = fileLocation.toString();
		this.fileGuid = fileGuid;
		this.userGuid = userGuid;
		this.sendingMachineGuid = sendingMachineGuid;
		this.lastUpdatedDate = lastUpdatedDate;
		this.priority=priority;
		this.size = info.size;
		this.Checksum = ByteUtils.toHexString(CheckSum);
		this.parentGUID = null;
		this.locationRelativeToParent = null;
		this.encryptedFileLocation = info.fileLocation.toString();
	}
	
	@JsonIgnore
	public FileSenderInfo(	EncryptedFileInfoStruct info, 
							Path fileLocation,
							UUID userGuid,
							UUID sendingMachineGuid,
							Date lastUpdatedDate,
							int priority,
							byte[] CheckSum,
							UUID parentGUID,
							Path locationRelativeToParent
	){
		this.AESInitializationVector = ByteUtils.toHexString(info.IV);
		this.fileLocation = fileLocation.toString();
		this.fileGuid = null;
		this.userGuid = userGuid;
		this.sendingMachineGuid = sendingMachineGuid;
		this.lastUpdatedDate = lastUpdatedDate;
		this.priority = priority;
		this.size = info.size;
		this.Checksum = ByteUtils.toHexString(CheckSum);
		this.parentGUID = parentGUID;
		this.locationRelativeToParent = locationRelativeToParent.toString();
		this.encryptedFileLocation = info.fileLocation.toString();
	}
	
	@JsonIgnore
	public String getAsJSON() throws JsonGenerationException, JsonMappingException, IOException {
	    ObjectMapper mapper = new ObjectMapper();
	    return mapper.writeValueAsString(this) ; 
	}
}

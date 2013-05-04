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
	public String AESInitializationVector;
	
	//Identification
	public String fileLocation;
	public UUID fileGuid;
	public UUID userGuid;
	public UUID sendingMachineGuid;
	
	//Meta data
	public Date lastUpdatedDate;
	public int priority;
	public int size;
	public String Checksum;
	
	//For directories only
	public UUID parentGUID;
	public String locationRelativeToParent; //Resolved against TLD of watched dir 
	
	@JsonIgnore
	public String encryptedFileLocation; //Used by sender
	
	public FileSenderInfo(){}
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

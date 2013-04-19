package net.subject17.jdfs.client.file.model;

import java.nio.file.Path;
import java.util.Date;
import java.util.UUID;

import org.codehaus.jackson.annotate.JsonIgnore;

public final class FileRetrieverRequest {
	//Identification
	public final UUID fileGuid;
	public final UUID userGuid;
	public final UUID sendingMachineGuid;

	public final Date lastUpdatedDate;
	
	public final String comparison;
	
	//For directories only
	public final UUID parentGUID;
	public final Path parentLocation; //Resolved against TLD of watched dir
	
	@JsonIgnore
	public FileRetrieverRequest(UUID fileGuid, UUID userGuid, UUID sendingMachineGuid, Date lastUpdated, String comparison, UUID parentGUID, Path parentLocation) {
		this.fileGuid = fileGuid;
		this.userGuid = userGuid;
		this.sendingMachineGuid = sendingMachineGuid;
		this.lastUpdatedDate = lastUpdated;
		
		if (null == comparison)
			comparison = "=";
		else {
			comparison = comparison.trim();
			if (!(comparison.equals(">=") || comparison.equals(">") || comparison.equals("<") || comparison.equals("<=")))
				comparison = "=";
		}
		this.comparison = comparison;
		this.parentGUID = parentGUID;
		this.parentLocation = parentLocation;
	}
	
	@JsonIgnore
	public FileRetrieverRequest(UUID fileGuid, UUID userGuid, Date lastUpdated, String comparison, UUID parentGUID, Path parentLocation) {
		this.fileGuid = fileGuid;
		this.userGuid = userGuid;
		this.sendingMachineGuid = null;
		this.lastUpdatedDate = lastUpdated;
		
		if (null == comparison)
			comparison = "=";
		else {
			comparison = comparison.trim();
			if (!(comparison.equals(">=") || comparison.equals(">") || comparison.equals("<") || comparison.equals("<=")))
				comparison = "=";
		}
		this.comparison = comparison;
		this.parentGUID = parentGUID;
		this.parentLocation = parentLocation;
	}
	
	@JsonIgnore
	public FileRetrieverRequest(UUID fileGuid, UUID userGuid, Date lastUpdated, String comparison) {
		this.fileGuid = fileGuid;
		this.userGuid = userGuid;
		this.sendingMachineGuid = null;
		this.lastUpdatedDate = lastUpdated;
		
		if (null == comparison)
			comparison = "=";
		else {
			comparison = comparison.trim();
			if (!(comparison.equals(">=") || comparison.equals(">") || comparison.equals("<") || comparison.equals("<=")))
				comparison = "=";
		}
		this.comparison = comparison;
		this.parentGUID = null;
		this.parentLocation = null;
	}
	
	@JsonIgnore
	public FileRetrieverRequest(UUID fileGuid, UUID userGuid, UUID parentGUID, Path parentLocation) {
		this.fileGuid = fileGuid;
		this.userGuid = userGuid;
		this.sendingMachineGuid = null;
		this.lastUpdatedDate = null;
		this.comparison = null;
		this.parentGUID = parentGUID;
		this.parentLocation = parentLocation;
	}
	
	@JsonIgnore
	public FileRetrieverRequest(UUID fileGuid, UUID userGuid, UUID sendingMachineGuid, Date lastUpdated, String comparison) {
		this.fileGuid = fileGuid;
		this.userGuid = userGuid;
		this.sendingMachineGuid = sendingMachineGuid;
		this.lastUpdatedDate = lastUpdated;
		
		if (null == comparison)
			comparison = "=";
		else {
			comparison = comparison.trim();
			if (!(comparison.equals(">=") || comparison.equals(">") || comparison.equals("<") || comparison.equals("<=")))
				comparison = "=";
		}
		this.comparison = comparison;
		this.parentGUID = null;
		this.parentLocation = null;
	}
	
	@JsonIgnore
	public FileRetrieverRequest(UUID fileGuid, UUID userGuid, UUID sendingMachineGuid) {
		this.fileGuid = fileGuid;
		this.userGuid = userGuid;
		this.sendingMachineGuid = sendingMachineGuid;
		this.lastUpdatedDate = null;
		this.comparison = null;
		this.parentGUID = null;
		this.parentLocation = null;
	}
	
	@JsonIgnore
	public FileRetrieverRequest(UUID fileGuid, UUID userGuid) {
		this.fileGuid = fileGuid;
		this.userGuid = userGuid;
		this.sendingMachineGuid = null;
		this.lastUpdatedDate = null;
		this.comparison = null;
		this.parentGUID = null;
		this.parentLocation = null;
	}
}

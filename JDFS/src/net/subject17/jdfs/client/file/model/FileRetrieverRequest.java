package net.subject17.jdfs.client.file.model;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.UUID;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

public final class FileRetrieverRequest {
	
	///
	//Had to remove final modifiers for json parsing
	///
	
	
	
	private final String defaultComparison = ">";
	
	//Identification
	public UUID fileGuid;
	public UUID userGuid;
	public UUID sendingMachineGuid;

	public Timestamp lastUpdatedDate;
	public String comparison;
	
	//For directories only
	public UUID parentGUID;
	public Path relativeParentLoc; //Resolved against TLD of watched dir
	
	
	public FileRetrieverRequest(){}
	
	
	public FileRetrieverRequest(UUID userGuid, UUID sendingMachineGuid, Timestamp lastUpdated, String comparison, UUID parentGUID, Path parentLocation) {
		this.fileGuid = null;
		this.userGuid = userGuid;
		this.sendingMachineGuid = sendingMachineGuid;
		this.lastUpdatedDate = lastUpdated;
		
		if (null == comparison)
			comparison = defaultComparison;
		else {
			comparison = comparison.trim();
			if (!(comparison.equals(">=") || comparison.equals(">") || comparison.equals("<") || comparison.equals("<=")))
				comparison = defaultComparison;
		}
		this.comparison = comparison;
		this.parentGUID = parentGUID;
		this.relativeParentLoc = parentLocation;
	}
	
	
	public FileRetrieverRequest(UUID fileGuid, UUID userGuid, UUID sendingMachineGuid, Timestamp lastUpdated, String comparison) {
		this.fileGuid = fileGuid;
		this.userGuid = userGuid;
		this.sendingMachineGuid = sendingMachineGuid;
		this.lastUpdatedDate = lastUpdated;
		
		if (null == comparison)
			comparison = defaultComparison;
		else {
			comparison = comparison.trim();
			if (!(comparison.equals(">=") || comparison.equals(">") || comparison.equals("<") || comparison.equals("<=")))
				comparison = defaultComparison;
		}
		this.comparison = comparison;
		this.parentGUID = null;
		this.relativeParentLoc = null;
	}
	
	//No machines
	
	public FileRetrieverRequest(UUID fileGuid, UUID userGuid, Timestamp lastUpdated, String comparison) {
		this.fileGuid = fileGuid;
		this.userGuid = userGuid;
		this.sendingMachineGuid = null;
		this.lastUpdatedDate = lastUpdated;
		
		if (null == comparison)
			comparison = defaultComparison;
		else {
			comparison = comparison.trim();
			if (!(comparison.equals(">=") || comparison.equals(">") || comparison.equals("<") || comparison.equals("<=")))
				comparison = defaultComparison;
		}
		this.comparison = comparison;
		this.parentGUID = null;
		this.relativeParentLoc = null;
	}
	
	
	public FileRetrieverRequest(UUID userGuid, Timestamp lastUpdated, String comparison, UUID ParentGUID, Path RelToParent) {
		this.fileGuid = null;
		this.userGuid = userGuid;
		this.sendingMachineGuid = null;
		this.lastUpdatedDate = lastUpdated;
		
		if (null == comparison)
			comparison = defaultComparison;
		else {
			comparison = comparison.trim();
			if (!(comparison.equals(">=") || comparison.equals(">") || comparison.equals("<") || comparison.equals("<=")))
				comparison = defaultComparison;
		}
		this.comparison = comparison;
		this.parentGUID = ParentGUID;
		this.relativeParentLoc = RelToParent;
	}
	
	//No comparisons
	
	public FileRetrieverRequest(UUID fileGuid, UUID userGuid, UUID sendingMachineGuid) {
		this.fileGuid = fileGuid;
		this.userGuid = userGuid;
		this.sendingMachineGuid = sendingMachineGuid;
		this.lastUpdatedDate = null;
		this.comparison = null;
		this.parentGUID = null;
		this.relativeParentLoc = null;
	}
	
	
	public FileRetrieverRequest(UUID userGuid, UUID sendingMachineGuid, UUID ParentGUID, Path RelToParent) {
		this.fileGuid = null;
		this.userGuid = userGuid;
		this.sendingMachineGuid = sendingMachineGuid;
		this.lastUpdatedDate = null;
		this.comparison = null;
		this.parentGUID = ParentGUID;
		this.relativeParentLoc = RelToParent;
	}
	
	//No machine or comparisons
	
	public FileRetrieverRequest(UUID fileGuid, UUID userGuid) {
		this.fileGuid = fileGuid;
		this.userGuid = userGuid;
		this.sendingMachineGuid = null;
		this.lastUpdatedDate = null;
		this.comparison = null;
		this.parentGUID = null;
		this.relativeParentLoc = null;
	}
	
	public FileRetrieverRequest(UUID userGuid, UUID parentGUID, Path parentLocation) {
		this.fileGuid = null;
		this.userGuid = userGuid;
		this.sendingMachineGuid = null;
		this.lastUpdatedDate = null;
		this.comparison = null;
		this.parentGUID = parentGUID;
		this.relativeParentLoc = parentLocation;
	}
	
	//User only
	
	public FileRetrieverRequest(UUID userGuid) {
		this.fileGuid = null;
		this.userGuid = userGuid;
		this.sendingMachineGuid = null;
		this.lastUpdatedDate = null;
		this.comparison = null;
		this.parentGUID = null;
		this.relativeParentLoc = null;
	}
	
	//Utility
	@JsonIgnore
	public String toJSON() throws JsonGenerationException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		return mapper.writeValueAsString(this);
	}
}

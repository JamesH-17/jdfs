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

import org.bouncycastle.pqc.math.linearalgebra.ByteUtils;
import org.codehaus.jackson.annotate.JsonIgnore;

public final class FileRetrieverInfo {
	
	//For decryption
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
	public FileRetrieverInfo(ResultSet filesFound) throws SQLException, IOException {
		fileLocation = Paths.get(filesFound.getString("FilePath"));
		size = (int) Files.size(fileLocation);
		
		fileGuid = UUID.fromString(filesFound.getString("FileGUID"));
		lastUpdatedDate = filesFound.getDate("UpdatedDate");
		AESInitializationVector = ByteUtils.fromHexString(filesFound.getString("IV"));
		userGuid = UUID.fromString(filesFound.getString("UserGUID"));
		
		String machineGuid = filesFound.getString("MachineGUID");
		UUID tempMachineUUID = null;
		
		if (!( null == machineGuid || machineGuid.equals("") )) {
			try {
				tempMachineUUID = UUID.fromString(machineGuid);
			} catch (IllegalArgumentException e) {
				Printer.logErr("Server sending file:  Could not convert ["+machineGuid+"] to UUID.  Setting value to null and proceeding");
				Printer.logErr(e);
				tempMachineUUID = null;
			}
		}
		
		sendingMachineGuid = tempMachineUUID;
		
		
		String parentGuid = filesFound.getString("ParentGUID");
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
		parentLocation = Paths.get(filesFound.getString("ParentLocation"));
		
		Checksum = ByteUtils.fromHexString(filesFound.getString("CheckSum"));
		priority = filesFound.getInt("Priority");
	}

}

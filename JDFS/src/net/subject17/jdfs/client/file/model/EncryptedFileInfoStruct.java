package net.subject17.jdfs.client.file.model;

import java.nio.file.Path;

import javax.crypto.spec.IvParameterSpec;

public class EncryptedFileInfoStruct {
	
	public Path fileLocation;
	public byte[] IV;
	
	public EncryptedFileInfoStruct(Path fileLocation, byte[] IV){
		this.fileLocation = fileLocation;
		this.IV = IV;
	}
	public EncryptedFileInfoStruct(Path fileLocation, IvParameterSpec iv){
		this(fileLocation, iv.getIV());
	}
	
}

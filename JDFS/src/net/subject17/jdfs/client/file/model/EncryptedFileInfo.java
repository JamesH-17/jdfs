package net.subject17.jdfs.client.file.model;

import java.nio.file.Path;

import javax.crypto.spec.IvParameterSpec;

public class EncryptedFileInfo {
	
	public Path fileLocation;
	public byte[] IV;
	
	public EncryptedFileInfo(Path fileLocation, byte[] IV){
		this.fileLocation = fileLocation;
		this.IV = IV;
	}
	public EncryptedFileInfo(Path fileLocation, IvParameterSpec iv){
		this(fileLocation, iv.getIV());
	}
	
}

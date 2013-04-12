package net.subject17.jdfs.client.file.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.crypto.spec.IvParameterSpec;

public class EncryptedFileInfoStruct {
	
	public final Path fileLocation;
	public final byte[] IV;
	public final int size;
	
	public EncryptedFileInfoStruct(Path fileLocation, byte[] IV) throws IOException {
		this.fileLocation = fileLocation;
		this.IV = IV;
		this.size = (int) Files.size(fileLocation);
	}
	public EncryptedFileInfoStruct(Path fileLocation, IvParameterSpec iv) throws IOException {
		this(fileLocation, iv.getIV());
	}
	
}

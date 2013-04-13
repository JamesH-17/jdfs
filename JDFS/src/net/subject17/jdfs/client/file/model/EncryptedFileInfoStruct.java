package net.subject17.jdfs.client.file.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.crypto.spec.IvParameterSpec;

import org.codehaus.jackson.annotate.JsonIgnore;

import net.subject17.jdfs.client.file.FileUtil;

public final class EncryptedFileInfoStruct {
	
	public final Path fileLocation;
	public final byte[] IV;
	public final int size;
	public final byte[] checkSum;
	
	@JsonIgnore
	public EncryptedFileInfoStruct(Path fileLocation, byte[] IV) throws IOException {
		this.fileLocation = fileLocation;
		this.IV = IV;
		this.size = (int) Files.size(fileLocation);
		this.checkSum = FileUtil.getInstance().getMD5Checksum(fileLocation);
	}
	@JsonIgnore
	public EncryptedFileInfoStruct(Path fileLocation, IvParameterSpec iv) throws IOException {
		this(fileLocation, iv.getIV());
	}
	
}

package net.subject17.jdfs.test.client.file;

import static org.junit.Assert.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.NoSuchPaddingException;

import net.subject17.jdfs.client.file.FileUtil;
import net.subject17.jdfs.client.file.model.EncryptedFileInfoStruct;
import net.subject17.jdfs.client.io.Printer;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;


public class FileUtilTest {
	static FileUtil fUtil = null;
	private Path encOut = null;
	

	private String key = "JAMES";
	
	@BeforeClass
	public static void setUpFutil(){
		try {
			fUtil = FileUtil.getInstance();
		} catch (Exception e) {
			Printer.log(e);
			Printer.log("Exception encountered setting up class.  Cannot continue with testing.");
		}
	}
	
	@Test
	public void testCompression() {
		try {
			Path inPath = Paths.get(System.getProperty("user.dir")).resolve("TEST").resolve("compTest.txt");
			
			assertTrue(Files.exists(inPath));
			
			Path outPath = fUtil.compressFile(inPath);
			Printer.println("File created at: "+outPath);
			
		} catch (Exception e){
			Printer.log(e);
			fail("Exception encountered in compressing file");
		}
	}
	
	@Test
	public void testExtraction() {
		try {
			Path inPath = Paths.get(System.getProperty("user.dir")).resolve("temp").resolve("compress").resolve("compTest.txt.xz");
			Path targetPath = Paths.get(System.getProperty("user.dir")).resolve("TEST").resolve("decompressedTest.txt");
			fUtil.decompressFile(inPath, targetPath);
			
		} catch (Exception e){
			Printer.log(e);
			fail("Exception encountered in compressing file");
		}
	}
	@Ignore
	@Test
	public void testLargeCompression() {
		try {
			Path inPath = Paths.get(System.getProperty("user.dir")).resolve("TEST").resolve("loremIpsum.txt");
			
			assertTrue(Files.exists(inPath));
			
			Path outPath = fUtil.compressFile(inPath);
			Printer.println("File created at: "+outPath);
			
		} catch (Exception e){
			Printer.log(e);
			fail("Exception encountered in compressing file");
		}
	}
	@Ignore
	@Test
	public void testHugeCompression() {
		try {
			Path inPath = Paths.get(System.getProperty("user.dir")).resolve("TEST").resolve("Repeated512MIB.txt");
			
			assertTrue(Files.exists(inPath));
			
			Path outPath = fUtil.compressFile(inPath);
			Printer.println("File created at: "+outPath);
			
		} catch (Exception e){
			Printer.log(e);
			fail("Exception encountered in compressing file");
		}
	}
	@Ignore
	@Test
	public void testGIGANTICCompression() {
		try {
			Path inPath = Paths.get(System.getProperty("user.dir")).resolve("TEST").resolve("Repeated1GiB.txt");
			
			assertTrue(Files.exists(inPath));
			
			Path outPath = fUtil.compressFile(inPath);
			Printer.println("File created at: "+outPath);
			
		} catch (Exception e){
			Printer.log(e);
			fail("Exception encountered in compressing file");
		}
	}
	
	@Test
	public void testEncryption() throws InterruptedException{
		String key = "JAMES";
		Path inPath = Paths.get(System.getProperty("user.dir")).resolve("TEST").resolve("loremIpsum.txt");
		
		try {
			encOut = FileUtil.getInstance().encryptFile(inPath, key);
			Printer.log(encOut);
		} catch (InvalidKeyException | NoSuchAlgorithmException
				| NoSuchPaddingException | IOException e) {
			Printer.logErr("Encountered exception!",Printer.Level.High);
			
			Printer.logErr(e);
			
			e.printStackTrace();
			fail();
		}
		testDecryption();
	}
	
	public void testDecryption() throws InterruptedException{
		String key = "JAMES";
		
		int t = 0;
		try {
			while (encOut == null && t++ < 10){
				Thread.sleep(1000);
			}
			Path decOut = FileUtil.getInstance().decryptFile(encOut, key);
			Printer.log(decOut);
		} catch (InvalidKeyException | NoSuchAlgorithmException
				| NoSuchPaddingException | IOException e) {
			Printer.logErr("Encountered exception!",Printer.Level.High);
			
			Printer.logErr(e);
			
			e.printStackTrace();
			fail();
		} catch (InvalidAlgorithmParameterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
	}
	
	@Test
	public void testCompressionEncryptionDecryptionExtraction(){
		EncryptedFileInfoStruct efi = testCompressionEncryption();
		
		if (null == efi)
			fail();
		
		testDecryptionExtraction(efi);
	}
	
	public EncryptedFileInfoStruct testCompressionEncryption(){
		Path inPath = Paths.get(System.getProperty("user.dir")).resolve("TEST").resolve("loremIpsum.txt");
		try {
			return FileUtil.getInstance().compressAndEncryptFile(inPath, key);
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		} catch (NoSuchPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
		return null;
	}
	
	public void testDecryptionExtraction(EncryptedFileInfoStruct efi){
		Path targetPath = efi.fileLocation.getParent().resolve(
				efi.fileLocation.getFileName().toString().replaceAll(".xz", "").replaceAll(".enc", "")+".dec"
		);
		try {
			FileUtil.getInstance().decryptAndExtractFile(efi, targetPath, key);
		} catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException
				| InvalidAlgorithmParameterException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}
	}
}

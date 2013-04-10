package com.subject17.jdfs.test.client.file;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.NoSuchPaddingException;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.subject17.jdfs.client.file.FileUtil;
import com.subject17.jdfs.client.io.Printer;

public class FileUtilTest {
	static FileUtil fUtil = null;
	private Path encOut = null;
	
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
}

package net.subject17.jdfs.test.client.settings.reader;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.text.DateFormat;
import java.sql.Date;
import java.sql.Timestamp;

import net.subject17.jdfs.client.io.Printer;

import org.junit.Ignore;
import org.junit.Test;


public class minitest {

	@Ignore
	@Test
	public void lolol(){
		Printer.print(Integer.parseInt(""));
	}
	@Test
	public void test() {
		Printer.println("cce652c9-bc66-40c3-978d-4dfc60395091".length());
		byte[] bytes = new byte[4];
	}
	
	@Test
	public void testPaths() throws IOException {
		Path loc = Paths.get(System.getProperty("user.dir"));
		
		try (DirectoryStream<Path> canidatePaths = Files.newDirectoryStream(loc)) {
			
			for (Path pathToCheck : canidatePaths) {
				Printer.println(pathToCheck+"");
				Printer.println(loc.resolve(pathToCheck).toString());
				Printer.println(loc.relativize(pathToCheck).toString());
			}
		}
	}
	
	private final int convertBytesToInt(byte[] bytes){
		int num = Integer.parseInt(bytes.toString());
		Printer.log("num:"+num);
		return num;
	}
	
	@Test
	public void testFileTime() throws IOException {
		Path loc = Paths.get(System.getProperty("user.dir"));
		
		try (DirectoryStream<Path> canidatePaths = Files.newDirectoryStream(loc)) {
			
			for (Path pathToCheck : canidatePaths) {
				Printer.println(pathToCheck+"");
				Printer.println(loc.resolve(pathToCheck).toString());
				Printer.println(loc.relativize(pathToCheck).toString());
				
				FileTime ft = Files.getLastModifiedTime(pathToCheck, LinkOption.NOFOLLOW_LINKS);
				Printer.println(ft);
				Timestamp dt = new Timestamp(ft.toMillis());
				
				Printer.println(dt);
				break;
			}
		} catch (Exception e){
			Printer.logErr(e);
		}
	}
	
	@Test
	public void testPathString() {
		Path loc = Paths.get(System.getProperty("user.dir"));
		Printer.println("test relativize");
		Printer.println(loc);
		Printer.println(loc.relativize(loc));
		Printer.println(loc.relativize(loc).toString().length());
	}
	
	@Test
	public void testFilePermissons() {
		String file = "C:\\Users\\James\\My Documents\\School\\test\\test.txt";
	}
	
	@Test
	public void testSym(){
		Path path = Paths.get("C:\\Users\\James\\My Documents\\School\\test");
		Printer.log("testing syms");
		
		Printer.println(path);
		
		Printer.println(Files.isDirectory(path));
		Printer.println(Files.notExists(path));
		Printer.println(Files.isSymbolicLink(path));
		Printer.println(Files.isRegularFile(path));
		
		Printer.log("done testing syms");
	}
}

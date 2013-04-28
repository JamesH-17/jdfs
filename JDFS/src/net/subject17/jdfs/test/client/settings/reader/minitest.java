package net.subject17.jdfs.test.client.settings.reader;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import net.subject17.jdfs.client.io.Printer;

import org.junit.Test;


public class minitest {

	@Test
	public void lolol(){
		Printer.print(Integer.parseInt(""));
	}
	@Test
	public void test() {
		byte[] bytes = new byte[4];
	}
	
	@Test
	public void testPaths() throws IOException{
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
}

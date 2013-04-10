package net.subject17.jdfs.test.client.settings.reader;

import static org.junit.Assert.*;

import net.subject17.jdfs.client.io.Printer;

import org.junit.Test;


public class minitest {

	@Test
	public void test() {
		byte[] bytes = new byte[4];
	}
	
	private final int convertBytesToInt(byte[] bytes){
		int num = Integer.parseInt(bytes.toString());
		Printer.log("num:"+num);
		return num;
	} 
}

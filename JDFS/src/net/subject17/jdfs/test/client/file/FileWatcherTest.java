package net.subject17.jdfs.test.client.file;

import static org.junit.Assert.*;

import java.util.UUID;

import net.subject17.jdfs.client.io.Printer;

import org.junit.Test;

public class FileWatcherTest {

	@Test
	public void test() {
		Printer.log("UUID and length:");
		Printer.log(UUID.randomUUID().toString());
		Printer.log(UUID.randomUUID().toString().length());
		fail("Not yet implemented");
	}

}

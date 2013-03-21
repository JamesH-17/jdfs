package com.subject17.jdfs.test.client.settings.reader;

import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.subject17.jdfs.client.io.Printer;
import com.subject17.jdfs.client.settings.reader.SettingsReader;
import com.subject17.jdfs.client.settings.reader.UserSettingsReader;
import com.subject17.jdfs.client.user.User;
import com.subject17.jdfs.client.user.UserUtil;

public class UserSettingsTest {
	private static final String rootDirectory = System.getProperty("user.dir");
	private static String rootTestDirectory;
	
	private static SettingsReader settingsReader, settingsReaderTest;
	private static UserSettingsReader userSettingsReader, userSettingsReaderTest;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		System.out.println("Root Directory: "+rootDirectory);
		rootTestDirectory = new File(System.getProperty("user.dir"),"TEST").getCanonicalPath();
		System.out.println("Root Test Directory: "+rootTestDirectory);
		
		settingsReader = new SettingsReader();
		settingsReaderTest = new SettingsReader(rootTestDirectory,"SettingsReaderTest.conf");
	}
	@Before
	public void setUp() throws Exception {
		userSettingsReader = new UserSettingsReader(settingsReader.getUserSettingsFile());
		userSettingsReaderTest = new UserSettingsReader(settingsReaderTest.getUserSettingsFile());
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testUserSettingsReaderFile() {
		testGetUsers();
		testGetActiveUser();
	}
	
	@Test
	public void testGetSettingsFile(){
		userSettingsReader.getUserSettingsFile();
		userSettingsReaderTest.getUserSettingsFile();
	}
	@Test
	public void testGetUsers() {
		ArrayList<User> usersNorm = userSettingsReader.getUsers();
		ArrayList<User> usersTest = userSettingsReaderTest.getUsers();
		
		Printer.println("UserNorm");
		for(User user : usersNorm){
			Printer.println("User email:" + user.getAccountEmail()+", name:"+user.getUserName());
		}
		Printer.println("UserTest");
		for(User user : usersTest){
			Printer.println("User email:" + user.getAccountEmail()+", name:"+user.getUserName());
		}
		assertEquals("Default users config not empty",true, usersNorm.isEmpty());
		assertEquals("Example users config empty!",false, usersTest.isEmpty());
	}

	@Test
	public void testGetActiveUser() {
		User usr = userSettingsReader.getActiveUser();
		if (usr != null){
			Printer.println("default user email:"+usr.getAccountEmail());
			Printer.println("default user name:"+usr.getUserName());
		} else {
			Printer.println("user null");
		}
		assertTrue(UserUtil.isEmptyUser(userSettingsReader.getActiveUser()));
		

		User usrA = userSettingsReaderTest.getActiveUser();
		if (usrA != null) {
			Printer.println("default user email:"+usrA.getAccountEmail());
			Printer.println("default user name:"+usrA.getUserName());
		} else {
			Printer.println("userA null");
		}
		assertTrue(!UserUtil.isEmptyUser(userSettingsReaderTest.getActiveUser()));
	}
}

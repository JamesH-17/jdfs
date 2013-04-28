package net.subject17.jdfs.test.client.settings.reader;

import static org.junit.Assert.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.UUID;

import net.subject17.jdfs.JDFSUtil;
import net.subject17.jdfs.client.io.Printer;
import net.subject17.jdfs.client.settings.Settings;
import net.subject17.jdfs.client.settings.reader.SettingsReader;
import net.subject17.jdfs.client.settings.reader.UserSettingsReader;
import net.subject17.jdfs.client.settings.writer.UserSettingsWriter;
import net.subject17.jdfs.client.user.User;
import net.subject17.jdfs.client.user.UserUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;


public class UserSettingsTest {
	//private static final String rootDirectory = System.getProperty("user.dir");
	private static final String rootDirectory = JDFSUtil.defaultDirectory;
	private static Path rootTestDirectory;
	
	private static SettingsReader settingsReader, settingsReaderTest;
	private static UserSettingsReader userSettingsReader, userSettingsReaderTest;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		System.out.println("Root Directory: "+rootDirectory);
		rootTestDirectory = Paths.get(rootDirectory,"TEST");
		System.out.println("Root Test Directory: "+rootTestDirectory);
		
		settingsReader = SettingsReader.getInstance();
		settingsReaderTest = SettingsReader.getInstance().parseAndReadXMLDocument(Paths.get("SettingsReaderTest.conf"));
				
	}
	@Before
	public void setUp() throws Exception {
		userSettingsReader = new UserSettingsReader(settingsReader.getUserSettingsPath());
		userSettingsReaderTest = new UserSettingsReader(settingsReaderTest.getUserSettingsPath());
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testUserSettingsReaderFile() {
		testGetUsersDefault();
		testGetActiveUserDefault();
	}
	
	@Test
	public void testGetSettingsFileDefault(){
		Printer.log(userSettingsReader.getUserSettingsPath());
		Printer.log(userSettingsReaderTest.getUserSettingsPath());
	}
	@Test
	public void testGetSettingsFile() {
		Printer.log(userSettingsReaderTest.getUserSettingsPath());
	}
	@Test
	public void testGetUsersDefault() {
		ArrayList<User> usersNorm = userSettingsReader.getUsers();
		ArrayList<User> usersTest = userSettingsReaderTest.getUsers();
		
		Printer.println("UserNorm");
		for(User user : usersNorm){
			Printer.println("User email:" + user.getAccountEmail()+", name:"+user.getUserName());
		}
		Printer.println("UserTest");
		for(User user : usersTest) {
			Printer.println("User email:" + user.getAccountEmail()+", name:"+user.getUserName());
		}
		assertEquals("Default users config not empty",true, usersNorm.isEmpty());
		assertEquals("Example users config empty!",false, usersTest.isEmpty());
	}

	@Test
	public void testGetActiveUserDefault() {
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
	
	@Test
	public void writeUsersDefault(){
		UserSettingsWriter writer = new UserSettingsWriter();
		Printer.log(writer.getUserSettingsPath());
		
		writer.writeUserSettings(userSettingsReader.getUsers());
		writer.setOutputFile(rootTestDirectory.resolve("UsersWriterTest.xml"));
		
		ArrayList<User> users = userSettingsReaderTest.getUsers();
		if (null != users && !users.isEmpty()) {
			User temp = users.get(0);
			
			UUID tmpUU = Settings.getMachineGUIDSafe();
			temp.registerUserToMachine(tmpUU);
		}
		
		writer.writeUserSettings(users);
	}
}

package net.subject17.jdfs.client.account;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.UUID;

import net.subject17.jdfs.client.settings.reader.UserSettingsReader;
import net.subject17.jdfs.client.settings.writer.UserSettingsWriter;
import net.subject17.jdfs.client.user.User;


public class AccountManager {
	private static Path usersFile = null;
	private static ArrayList<User> users = new ArrayList<User>();	//Using array list here since data integrity is important
																	//(don't trust unique hashCode), we do a lot of lookup based
																	//upon element properties (necessitating a lookup anyway)
																	//and there won't be too many elements in here anyway. 
	private static User activeUser =  null;
	
	public void readUsersFromFile(Path userSettingsFile) throws Exception {
		usersFile = userSettingsFile;
		UserSettingsReader settingsSource = new UserSettingsReader(userSettingsFile);
		
		users.addAll(settingsSource.getUsers());
		
		activeUser = settingsSource.getActiveUser();
	}
	
	public static void setUsersSettingsFile(Path newLocation) {
		usersFile = newLocation;
	}
		
	//ACTIVE USER
	
	public static User getActiveUser() {
		return activeUser;
	}
	
	public static void modifyActiveUser(User modifiedActiveUser){
		if(users.contains(activeUser))
			users.remove(activeUser);
		users.add(modifiedActiveUser);
		activeUser = modifiedActiveUser;
	}
	
	public static void setActiveUser(User newActiveUser) {
		if (!users.contains(newActiveUser))
			users.add(newActiveUser);
		activeUser = newActiveUser;
	}
	
	public static boolean setExistingActiveUserByAccount(String accountEmail){
		User temp = getUserByAccount(accountEmail);
		if (temp != null ){
			activeUser = temp;
			return true;
		} else return false;
	}
	
	public static User setActiveUserByGUID(String guid){
		return activeUser = getUserByGUID(guid);		
	}
	
	public static User setActiveUserByAccount(String accountEmail){
		return activeUser = getUserByAccount(accountEmail);		
	}
	
	public static boolean setExistingActiveUserByUserName(String userName){
		User temp = getUserByUserName(userName);
		if (temp != null ){
			activeUser = temp;
			return true;
		} else return false;
	}
	
	public static User setActiveUserByUserName(String userName){
		return activeUser = getUserByUserName(userName);		
	}
	
	
	
	//All Users
	
	public static void clearUsers() {
		users.clear();
		activeUser = null;
	}
	
	public static void writeUsersToFile() {
		writeUsersToFile(usersFile);
	}
	
	public static void writeUsersToFile(String path, String filename) {
		writeUsersToFile(Paths.get(path, filename));
	}
	
	public static void writeUsersToFile(Path file) {
		UserSettingsWriter writer = new UserSettingsWriter(file);
		writer.writeUserSettings(users, activeUser);
	}

	public static User getUserByAccount(String account) {
		for (User user : users) {
			if (user.getAccountEmail().equals(account))
				return user;
		}
		return null;
	}

	public static boolean accountExists(String account) {
		for (User user : users) {
			if (user.getAccountEmail().equals(account))
				return true;
		}
		return false;
	}
	
	public static User getUserByGUID(String guid) {
		return getUserByGUID(UUID.fromString(guid));
	}
	public static User getUserByGUID(UUID guid) {
		for (User user : users) {
			if (user.getGUID().equals(guid))
				return user;
		}
		return null;
	}
	
	public static User getUserByUserName(String userName) {
		for (User user : users) {
			if (user.getUserName().equals(userName))
				return user;
		}
		return null;
	}

	public static boolean userNameExists(String userName) {
		for (User user : users) {
			if (user.getUserName().equals(userName))
				return true;
		}
		return false;
	}
	
	public static boolean guidExists(String guid){
		return guidExists(guid.toString());
	}
	public static boolean guidExists(UUID guid) {
		for (User user : users) {
			if (user.getGUID().equals(guid))
				return true;
		}
		return false;
	}
}

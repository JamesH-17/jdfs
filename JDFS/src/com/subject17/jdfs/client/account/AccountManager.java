package com.subject17.jdfs.client.account;

import java.io.File;
import java.util.ArrayList;

import com.subject17.jdfs.client.settings.reader.UserSettingsReader;
import com.subject17.jdfs.client.user.User;

public class AccountManager {
	private File usersFile;
	private UserSettingsReader settingsSource;
	private ArrayList<User> users;
	
	private User activeUser;
	
	public AccountManager(File userSettingsFile) throws Exception {
		usersFile = userSettingsFile;
		settingsSource = new UserSettingsReader(userSettingsFile);
		
		users.addAll(settingsSource.getUsers());
		
		activeUser = settingsSource.getActiveUser();
	}
	
	public User getActiveUser() {
		return activeUser;
	}
	
	public void setActiveUser(User newActiveUser) {
		if (!users.contains(newActiveUser))
			users.add(newActiveUser);
		activeUser = newActiveUser;
	}
	
	public void writeUsersToFile() {
		writeUsersToFile(usersFile);
	}
	
	public void writeUsersToFile(String path, String filename) {
		writeUsersToFile(new File(path, filename));
	}
	
	public void writeUsersToFile(File file){
		
	}

	public static User getAccount(User user) {
		// TODO Auto-generated method stub
		return null;
	}

	public static boolean accountExists(String account) {
		for (User user : users) {
			if (user.getAccountEmail().equals(account))
				return true;
		}
		return false;
	}

}

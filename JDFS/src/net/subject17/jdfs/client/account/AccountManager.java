package net.subject17.jdfs.client.account;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

import net.subject17.jdfs.client.file.db.DBManager;
import net.subject17.jdfs.client.file.db.DBManager.DBManagerFatalException;
import net.subject17.jdfs.client.io.Printer;
import net.subject17.jdfs.client.io.UserInput;
import net.subject17.jdfs.client.settings.reader.UserSettingsReader;
import net.subject17.jdfs.client.settings.writer.UserSettingsWriter;
import net.subject17.jdfs.client.user.User;
import net.subject17.jdfs.client.user.User.UserException;
import net.subject17.jdfs.security.JDFSSecurity;


public class AccountManager {
	private static AccountManager _instance = null;
	
	private static Path usersFile;
	private static ArrayList<User> users;	//Using array list here since data integrity is important
																	//(don't trust unique hashCode), we do a lot of lookup based
																	//upon element properties (necessitating a lookup anyway)
																	//and there won't be too many elements in here anyway. 
	private static User activeUser;
	
	private static HashMap<User, byte[]> keys;
	
	private AccountManager(){
		users = new ArrayList<User>();
		keys = new HashMap<User,byte[]>();
		activeUser = null;
		usersFile = null;
	}
	
	public static AccountManager getInstance(){
		if (null == _instance){
			synchronized(AccountManager.class){
				if (null == _instance){
					_instance = new AccountManager();
				}
			}
		}
		return _instance;
	}
	
	
	
	/////////////
	// Seed Users
	//
	
	
	public void readUsersFromFile(Path userSettingsFile) throws Exception {
		usersFile = userSettingsFile;
		UserSettingsReader settingsSource = new UserSettingsReader(userSettingsFile);
		
		users.addAll(settingsSource.getUsers());
		
		updateDatabase(users);
		
		activeUser = settingsSource.getActiveUser();
	}
	
	private void updateDatabase(User user) {
		try (ResultSet usersFound = DBManager.getInstance().select(
				"SELECT DISTINCT * FROM Users "+
				"WHERE Users.UserName LIKE '"+user.getUserName()+"'"+
				"AND Users.AccountEmail LIKE '"+user.getAccountEmail()+"'"
			)
		) {
			if (usersFound.next()) {
				if (!usersFound.getString("UserGUID").equals(user.getGUID().toString())) {
					Printer.log("Updating GUID of user "+user.getUserName());
					Printer.log("Old value:"+usersFound.getString("UserGUID"));
					Printer.log("New value:"+user.getGUID());
					DBManager.getInstance().upsert(
							"UPDATE Users SET UserGUID = '"+user.getGUID()+
							"' WHERE Users.UserPK = "+usersFound.getInt("UserPK")
					);
				}
			}
			else {
				try {
					DBManager.getInstance().upsert(
							"INSERT INTO Users(UserGUID, UserName, AccountEmail) "+
							"VALUES ('"+user.getGUID()+"','"+user.getUserName()+"','"+user.getAccountEmail()+"')"
						);
				} catch (SQLException e) {
					Printer.logErr("An error occured adding the user "+user.getUserName()+" to the database");
					Printer.logErr(e);
				}
			}
			
		} catch (SQLException e) {
			Printer.logErr(e);
		} catch (DBManagerFatalException e) {
			Printer.logErr(e);
		}
	}
	
	private void updateDatabase(ArrayList<User> users) throws DBManagerFatalException {
		
		for (User user : users) {
			updateDatabase(user);
		}
	}
	
	public ArrayList<User> getUsersFromDatabase() throws DBManagerFatalException {
		HashSet<User> users = new HashSet<User>();
		
		try (ResultSet usersFound = DBManager.getInstance().select(
				"SELECT DISTINCT Users.* FROM Users "
			)
		) {
			try {
				users.add(new User(
						usersFound.getString("UserName"),
						usersFound.getString("AccountEmail"),
						UUID.fromString(usersFound.getString("UserGUID"))
				));
			} catch (UserException e) {
				Printer.logErr("Error adding specific user "+usersFound.getString("UserName")+", skipping");
			}
		} catch (SQLException e) {
			Printer.logErr("An error occured grabbing users from the DB");
			Printer.logErr(e);
		}
		
		return new ArrayList<User>(users);
	}

	public void setUsersSettingsFile(Path newLocation) {
		usersFile = newLocation;
	}
		
	//////////////////////////////////////////////////////
	//			Utilities to handle active user			//
	//////////////////////////////////////////////////////
	
	public User getActiveUser() {
		return activeUser;
	}
	
	public void modifyActiveUser(User modifiedActiveUser) {
		if(users.contains(activeUser))
			users.remove(activeUser);
		users.add(modifiedActiveUser);
		activeUser = modifiedActiveUser;
	}
	
	public void setActiveUser(User newActiveUser) {
		if (!users.contains(newActiveUser))
			users.add(newActiveUser);
		activeUser = newActiveUser;
	}
	
	public boolean setExistingActiveUserByAccount(String accountEmail) {
		User temp = getUserByAccount(accountEmail);
		if (temp != null ){
			activeUser = temp;
			return true;
		} else return false;
	}
	
	public User setActiveUserByGUID(String guid) {
		return activeUser = getUserByGUID(guid);		
	}
	
	public User setActiveUserByAccount(String accountEmail) {
		return activeUser = getUserByAccount(accountEmail);		
	}
	
	public boolean setExistingActiveUserByUserName(String userName) {
		User temp = getUserByUserName(userName);
		if (temp != null ){
			activeUser = temp;
			return true;
		} else return false;
	}
	
	public User setActiveUserByUserName(String userName){
		return activeUser = getUserByUserName(userName);		
	}
	
	
	
	//////////////////////////////////////////////////////
	//			Utilities for all users					//
	//////////////////////////////////////////////////////
	
	public void clearUsers() {
		users.clear();
		activeUser = null;
	}
	public ArrayList<User> getUsers(){return users;}
	
	public void writeUsersToFile() {
		writeUsersToFile(usersFile);
	}
	
	/*public void writeUsersToFile(String path, String filename) {
		writeUsersToFile(Paths.get(path, filename));
	}*/
	
	public void writeUsersToFile(Path file) {
		UserSettingsWriter writer = new UserSettingsWriter(file);
		try {
			writer.writeUserSettings(getUsersFromDatabase(), activeUser);
		}
		catch (DBManagerFatalException e) {
			Printer.logErr("Error encountered getting users from DB for xml file creation");
			Printer.logErr("Using internal users structure to write xml file instead");
			Printer.logErr(e);
			
			writer.writeUserSettings(users, activeUser);
		}
	}

	public User getUserByAccount(String account) {
		for (User user : users) {
			if (user.getAccountEmail().equals(account))
				return user;
		}
		return null;
	}

	public boolean accountExists(String account) {
		for (User user : users) {
			if (user.getAccountEmail().equals(account))
				return true;
		}
		return false;
	}
	
	public User getUserByGUID(String guid) {
		return getUserByGUID(UUID.fromString(guid));
	}
	public User getUserByGUID(UUID guid) {
		for (User user : users) {
			if (user.getGUID().equals(guid))
				return user;
		}
		return null;
	}
	
	public User getUserByUserName(String userName) {
		for (User user : users) {
			if (user.getUserName().equals(userName))
				return user;
		}
		return null;
	}

	public boolean userNameExists(String userName) {
		for (User user : users) {
			if (user.getUserName().equals(userName))
				return true;
		}
		return false;
	}
	
	public boolean guidExists(String guid){
		return guidExists(guid.toString());
	}
	public boolean guidExists(UUID guid) {
		for (User user : users) {
			if (user.getGUID().equals(guid))
				return true;
		}
		return false;
	}

	public byte[] getPasswordDigest() throws UserException {
		if (null == activeUser){
			String userName = UserInput.getInstance().getNextString("Please enter a user name");
			String accountEmail = UserInput.getInstance().getNextString("Please Enter an email address");
			users.add(activeUser = new User(userName, accountEmail));
			updateDatabase(activeUser); //Note how there's only one entry in the db here
			Printer.print("An account has been created for you.");
		}
		if (!keys.containsKey(activeUser)) {
			String plaintextPass = UserInput.getInstance().getNextString("Please enter a password to encrypt/decrypt files");
			try {
				keys.put(activeUser, JDFSSecurity.getSecureDigest(plaintextPass));
			}
			catch (NoSuchAlgorithmException e) {
				Printer.logErr("An error was encountered in encrypting your password.", Printer.Level.High);
				Printer.logErr(e);
			}
		}
		return keys.get(activeUser);
	}
}

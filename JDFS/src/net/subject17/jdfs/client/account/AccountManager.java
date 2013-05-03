package net.subject17.jdfs.client.account;

import java.nio.file.Path;
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
import net.subject17.jdfs.client.user.UserUtil;
import net.subject17.jdfs.client.user.User.UserException;
import net.subject17.jdfs.security.JDFSSecurity;


public final class AccountManager {
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
		
		for (UUID machine : user.getRegisteredMachines()) {
			ensureUserLinkedToMachine(user, machine);
		}
	}
	
	private void updateDatabase(ArrayList<User> users) throws DBManagerFatalException {
		
		for (User user : users) {
			updateDatabase(user);
		}
	}
	
	private boolean ensureUserLinkedToMachine(User user, UUID machineGuid) {
		try (ResultSet linksFound = DBManager.getInstance().select("SELECT * FROM Users INNER JOIN MachineUserLinks ON MachineUserLinks.UserPK = Users.UserPK INNER JOIN Machines ON Machines.MachinePK = MachineUserLinks.MachinePK WHERE Users.UserGUID LIKE '"+user.getGUID()+"' AND Machines.MachineGUID LIKE '"+machineGuid+"'")) {
			if (linksFound.next()) {
				return true; //already linked
			}
			else {
				return linkUserToMachine(getUserPKSafe(user), getMachinePKSafe(machineGuid));
			}
		} catch (SQLException | DBManagerFatalException e) {
			Printer.logErr("Error ensuring user linked to machine "+machineGuid);
			Printer.logErr(e);
			return false;
		}
	}
	
	private boolean linkUserToMachine(int userPK, int machinePK) {
		try (ResultSet linkAdded = DBManager.getInstance().upsert("INSERT INTO MachineUserLinks(UserPK, MachinePK) VALUES("+userPK+","+machinePK+")")) {
			return true;
		} catch (SQLException | DBManagerFatalException e) {
			Printer.logErr("Error linking user to machine");
			Printer.logErr(e);
			return false;
		}
	}
	
	private int getUserPKSafe(User user) {
		try (ResultSet userFound = DBManager.getInstance().select("SELECT TOP 1 * FROM Users WHERE Users.UserGUID LIKE '"+user.getGUID()+"' AND Users.AccountEmail LIKE '"+user.getAccountEmail()+"' AND Users.UserName LIKE '"+user.getUserName()+"'")
		) {
			if (userFound.next()) {
				return userFound.getInt("UserPK");
			}
			else {
				try (ResultSet userAdded = DBManager.getInstance().upsert("INSERT INTO Users(UserName, AccountEmail, UserGUID) VALUES('"+user.getUserName()+"','"+user.getAccountEmail()+"','"+user.getGUID()+"')")
				) {
					userAdded.next();
					return userAdded.getInt("UserPK");
				}
			}
		} catch (SQLException | DBManagerFatalException e) {
			Printer.logErr("Error grabbing user PK, returning default of -1");
			Printer.logErr(e);
			return -1;
		}
	}
	//TODO refactor into sperate class
	private int getMachinePKSafe(UUID machineGUID) {
		try (ResultSet machinesFound = DBManager.getInstance().select("SELECT TOP 1 * FROM Machines WHERE Machines.MachineGUID LIKE '"+machineGUID+"'")
		) {
			if (machinesFound.next()) {
				return machinesFound.getInt("MachinePK");
			}
			else {
				try (ResultSet machineAdded = DBManager.getInstance().upsert("INSERT INTO Machines(MachineGUID) VALUES('"+machineGUID+"')")
				) {
					machineAdded.next();
					return machineAdded.getInt("MachinePK");
				}
			}
		} catch (SQLException | DBManagerFatalException e) {
			Printer.logErr("Error grabbing machine PK, returning default of -1");
			Printer.logErr(e);
			return -1;
		}
	}
	//////////////////////////////////////////////////////
	//						XML							//
	//////////////////////////////////////////////////////
	
	public void writeUsersToFile() {
		writeUsersToFile(usersFile);
	}
	
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
		try {
			ensureAccountExists(activeUser = newActiveUser);
		} catch (DBManagerFatalException e) {
			Printer.logErr(e);
		}
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

	
	//Account lookups
	
	/**
	 * Returns the value of the account registered to this user, or null if none are found.
	 * Checks local array first, then DB
	 * @param account 
	 * @return the User represented by this account, or null if one is not found
	 */
	public User getUserByAccount(String account) {
		for (User user : users) {
			if (user.getAccountEmail().equals(account))
				return user;
		}
		return null;
	}
	public User getUserInDbByAccount(String account) {
		try (ResultSet accountsFound = DBManager.getInstance().select(
				"SELECT DISTINCT Users.* FROM Users WHERE Users.AccountEmail LIKE '"+account+"'"
			)
		) {
			if (accountsFound.next()) {
				return new User(accountsFound.getString("UserName"),
						accountsFound.getString("AccountEmail"),
						UUID.fromString(accountsFound.getString("AccountEmail"))
				);
			}
		}
		catch (SQLException | DBManagerFatalException | UserException e) {
			Printer.logErr(e);
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
	
	public boolean accountExistsInDb(String account) {
		try (ResultSet accountsFound = DBManager.getInstance().select(
				"SELECT DISTINCT * FROM Users WHERE Users.AccountEmail LIKE '"+account+"'"
			)
		) {
			return accountsFound.next();
		} catch (SQLException | DBManagerFatalException e) {
			Printer.logErr(e);
			return false;
		}
	}
	
	//Username lookups
	public User getUserByUserName(String userName) {
		for (User user : users) {
			if (user.getUserName().equals(userName))
				return user;
		}
		return null;
	}
	public User getUserInDbByUserName(String userName) {
		try (ResultSet usernamesFound = DBManager.getInstance().select(
				"SELECT DISTINCT Users.* FROM Users WHERE Users.UserName LIKE '"+userName+"'"
			)
		) {
			if (usernamesFound.next()) {
				return new User(usernamesFound.getString("UserName"),
						usernamesFound.getString("AccountEmail"),
						UUID.fromString(usernamesFound.getString("AccountEmail"))
				);
			}
		}
		catch (SQLException | DBManagerFatalException | UserException e) {
			Printer.logErr(e);
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
	public boolean userNameExistsInDb(String userName) {
		try (ResultSet usernamesFound = DBManager.getInstance().select(
				"SELECT DISTINCT * FROM Users WHERE Users.UserName LIKE '"+userName+"'"
			)
		) {
			return usernamesFound.next();
		} catch (SQLException | DBManagerFatalException e) {
			Printer.logErr(e);
			return false;
		}
	}
	
	//GUID lookups

	public User getUserByGUID(String guid) {
		return getUserByGUID(UUID.fromString(guid));
	}
	public User getUserByGUID(UUID guid) {
		for (User user : users) {
			if (user.getGUID().equals(guid))
				return user;
		}
		return getUserInDbByGUID(guid);
	}
	
	public User getUserInDbByGUID(UUID guid) {
		try(ResultSet usersFound = DBManager.getInstance().select("SELECT DISTINCT * FROM Users WHERE Users.UserGUID LIKE '"+guid+"'")) {
			usersFound.next();
			
			User temp = new User(
					usersFound.getString("UserName"),
					usersFound.getString("AccountEmail"),
					UUID.fromString(usersFound.getString("UserGUID"))
			); 
			
			if (!users.contains(temp)) {
				users.add(temp);
			}
			
			return temp;
		}
		catch (Exception e){
			Printer.logErr("Exception encountered getting user from DB");
			Printer.logErr(e);
			return null;
		}
	}

	public boolean guidExists(String guid){
		return guidExists(guid.toString());
	}
	public boolean guidExists(UUID guid) {
		for (User user : users) {
			Printer.log("User: "+user);
			Printer.log("User GUID: "+user.getGUID());
			if (user.getGUID().equals(guid))
				return true;
		}
		return guidExistsInDB(guid);
	}

	
	//Utility/Misc
	
	private boolean guidExistsInDB(UUID guid) {
		try(ResultSet usersFound = DBManager.getInstance().select("SELECT DISTINCT * FROM Users WHERE Users.UserGUID LIKE '"+guid+"'")) {
			return usersFound.next();
		}
		catch (Exception e){
			Printer.logErr("Exception encountered getting user from DB");
			Printer.logErr(e);
			return false;
		}
	}

	public ArrayList<User> getUsersFromDatabase() throws DBManagerFatalException {
		HashSet<User> users = new HashSet<User>();
		
		try (ResultSet usersFound = DBManager.getInstance().select(
				"SELECT DISTINCT Users.* FROM Users "
			)
		) {
			try {
				while (usersFound.next()) {
					users.add(new User(
							usersFound.getString("UserName"),
							usersFound.getString("AccountEmail"),
							UUID.fromString(usersFound.getString("UserGUID"))
					));
				}
			} catch (UserException e) {
				Printer.logErr("Error adding specific user "+usersFound.getString("UserName")+", skipping");
			}
		} catch (SQLException e) {
			Printer.logErr("An error occured grabbing users from the DB");
			Printer.logErr(e);
		}
		
		return new ArrayList<User>(users);
	}
	
	public boolean ensureAccountExists(User user) throws DBManagerFatalException {
		try {
			
			if (null == user)
				return false;
			
			if (!users.contains(user))
				users.add(user);
			
			if (!UserUtil.userExistsInDB(user))
				UserUtil.addUserToDB(user);
			
			return true;
		}
		catch (SQLException e) {
			Printer.logErr("Error: Cannot ensure user "+user.getUserName()+" exists in DB!");
			Printer.logErr(e);
			return false;
		}
	}
	
	
	//Security
	
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

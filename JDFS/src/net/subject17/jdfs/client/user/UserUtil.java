package net.subject17.jdfs.client.user;

import java.sql.ResultSet;
import java.sql.SQLException;

import net.subject17.jdfs.client.file.db.DBManager;
import net.subject17.jdfs.client.file.db.DBManager.DBManagerFatalException;
import net.subject17.jdfs.client.io.Printer;

public class UserUtil {
	
	public static boolean isValidUsername(String usrName) {
		return  !(usrName == null || usrName.trim().isEmpty());
	}

	public static boolean isValidEmail(String email) {
		return !(email == null || email.trim().isEmpty()) && email.matches("[^@]+@[^@]+"); //We're unrestrictive as possible here
	}

	public static boolean isEmptyUser(User usr) {
		return usr == null || !isValidUsername(usr.getUserName()) || !isValidEmail(usr.getAccountEmail()); 
	}
	
	public static int getUserPK(User user) {
		try (ResultSet userPK = DBManager.getInstance().select("SELECT TOP 1 UserPK FROM Users WHERE User WHERE User.UserGUID LIKE '"+user.getGUID()+"'")) {
			if (userPK.next())
				return userPK.getInt("UserPK");
		}
		catch (SQLException | DBManagerFatalException e) {
			Printer.logErr("Error getting user PK");
			Printer.logErr(e);
		}
		return -1;
	}
	
	public static boolean userExistsInDB(User user) throws SQLException, DBManagerFatalException {
		try (ResultSet userMatch = DBManager.getInstance().select("SELECT DISTINCT Users.* FROM Users WHERE Users.UserGUID LIKE '"+user.getGUID()+"'")) {
			return userMatch.next();
		}
	}

	public static void addUserToDB(User user) throws SQLException, DBManagerFatalException {
		DBManager.getInstance().upsert("INSERT INTO Users(UserName, AccountEmail, UserGUID) VALUES ('"+user.getUserName()+"','"+user.getAccountEmail()+"','"+user.getGUID()+"')").next();
	}
}

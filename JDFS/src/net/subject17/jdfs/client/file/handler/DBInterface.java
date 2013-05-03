package net.subject17.jdfs.client.file.handler;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import net.subject17.jdfs.client.file.db.DBManager;
import net.subject17.jdfs.client.file.db.DBManager.DBManagerFatalException;
import net.subject17.jdfs.client.io.Printer;
import net.subject17.jdfs.client.user.User;

public class DBInterface {
	public static int getUserPKSafe(User user) {
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
	
	public static int getMachinePKSafe(UUID machineGUID) {
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
}

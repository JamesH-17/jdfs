package net.subject17.jdfs.client.account;

public class UserUtil {
	
	public static boolean isValidUsername(String usrName) {
		return  !(usrName == null || usrName.isEmpty());
	}

	public static boolean isValidEmail(String email) {
		return !email.isEmpty() && email.matches("[^@]+@[^@]+"); //We're unrestrictive as possible here
	}
	
}

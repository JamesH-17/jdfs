package net.subject17.jdfs.client.user;

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
}

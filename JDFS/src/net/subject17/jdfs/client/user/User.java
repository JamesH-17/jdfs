package net.subject17.jdfs.client.user;

import java.util.UUID;

import net.subject17.jdfs.client.io.Printer;

import org.w3c.dom.Element;
public class User {
	public static class UserException extends Exception {
		private static final long serialVersionUID = 1L;
		UserException(String msg){super(msg);}
		UserException(String msg, Throwable thrw){super(msg,thrw);}
	}
	private String username;
	private String account;
	private UUID userGUID;
	private static final char seperatorChar = '\n';
	
	public User(String name, String email) throws UserException {
		if (UserUtil.isValidEmail(email) && UserUtil.isValidUsername(email)) {
			username = name;
			account = email;
		} else {
			if (email == null) email = new String("null");
			if (name == null) name = new String("null");
			throw new UserException("Invalid data for user -- provided email:["+email+"], name: ["+name+"]");
		}
	}
	public User(Element node) throws UserException {
		if (node == null || !node.getTagName().equals("user"))
			throw new UserException("Invalid data for element " + node == null ? "[null]" : node.toString());
		username = node.getElementsByTagName("userName").item(0).getTextContent();
		account = node.getElementsByTagName("email").item(0).getTextContent();
		try {
			userGUID = UUID.fromString(node.getElementsByTagName("guid").item(0).getTextContent());	
		} catch(Exception e){
			Printer.log("Error grabbing uuid for user "+username+", generating default");
			userGUID = UUID.randomUUID();
		}
	}
	
	@Override
	public boolean equals(Object cmp) {
		return cmp != null 
				&& cmp instanceof User 
				&& this.username.equals(((User)cmp).username)
				&& this.account.equals(((User)cmp).account);
	}
	
	@Override
	public int hashCode() {
		return (username+seperatorChar+account).hashCode();
	} 
	
	public String getUserName() {return username;}
	public String getAccountEmail() {return account;}
	public UUID getUUID() {return userGUID;}
	public boolean isEmpty(){ //Really, not needed, but just in case
		return (username == null || username.isEmpty())||(account == null || account.isEmpty());
	}
}

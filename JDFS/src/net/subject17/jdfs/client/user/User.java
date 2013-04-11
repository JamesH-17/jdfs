package net.subject17.jdfs.client.user;

import java.util.UUID;

import net.subject17.jdfs.client.settings.reader.SettingsReader;

import org.w3c.dom.Element;
public class User {
	public static class UserException extends Exception {
		private static final long serialVersionUID = 1L;
		UserException(String msg){super(msg);}
		UserException(String msg, Throwable thrw){super(msg,thrw);}
	}
	private String username;
	private String account;
	private UUID GUID;
	//private static final char seperatorChar = '\n';
	
	public User(String name, String email, UUID guid) throws UserException {
		if (UserUtil.isValidEmail(email) && UserUtil.isValidUsername(email)) {
			username = name;
			account = email;
			GUID = guid;
		} else {
			if (email == null) email = new String("null");
			if (name == null) name = new String("null");
			throw new UserException("Invalid data for user -- provided email:["+email+"], name: ["+name+"]");
		}
	}
	public User(Element node) throws UserException {
		if (node == null || !node.getTagName().equals("user"))
			throw new UserException("Invalid data for element " + node == null ? "[null]" : node.toString());
		username = SettingsReader.GetFirstNodeValue(node, "userName");
		account = SettingsReader.GetFirstNodeValue(node, "email");
		GUID = UUID.fromString(SettingsReader.GetFirstNodeValue(node, "GUID"));
		
		if (!UserUtil.isValidEmail(account) || !UserUtil.isValidUsername(username)) {
			throw new UserException("Invalid data for user -- provided email:["+account+"], name: ["+username+"], GUID:["+GUID.toString()+"]");
		}
	}
	
	@Override
	public boolean equals(Object cmp) {
		return cmp != null 
				&& cmp instanceof User 
				&& this.username.equals(((User)cmp).username)
				&& this.account.equals(((User)cmp).account)
				&& this.GUID.equals(((User)cmp).GUID);
	}
	
	@Override
	public int hashCode() {
		//return (username+seperatorChar+account).hashCode();
		return GUID.hashCode();
	} 
	
	public String getUserName() {return username;}
	public String getAccountEmail() {return account;}
	public UUID getGUID() {return GUID;}
	public boolean isEmpty(){ //Really, not needed, but just in case
		return (username == null || username.isEmpty())||(account == null || account.isEmpty());
	}
}

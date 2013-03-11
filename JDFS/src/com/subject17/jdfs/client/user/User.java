package com.subject17.jdfs.client.user;

import org.w3c.dom.Element;

public class User {
	private String username;
	private String account;
	
	public User(String name, String email) {
		username = name;
		account = email;
	}
	public User(Element node) throws Exception {
		if (!node.getTagName().equals("user"))
			throw new Exception("Invalid data");
		username = node.getElementsByTagName("userName").item(0).getNodeValue();
		account = node.getElementsByTagName("email").item(0).getNodeValue();
	}
	
	@Override
	public boolean equals(Object cmp) {
		return cmp != null 
				&& cmp instanceof User 
				&& this.username.equals(((User)cmp).username)
				&& this.account.equals(((User)cmp).account);
	}
}

package com.subject17.jdfs.client.user;

import org.w3c.dom.Element;

public class User {
	public String username;
	public String account;
	
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
}

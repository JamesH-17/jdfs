package com.subject17.jdfs.client.account;

import org.w3c.dom.Element;

public class UserUtil {
	
	public static boolean isValidUsername(String usrName) {
		return  !(usrName == null || usrName.isEmpty());
	}
	
	

}

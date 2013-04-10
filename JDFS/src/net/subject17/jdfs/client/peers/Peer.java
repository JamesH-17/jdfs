package net.subject17.jdfs.client.peers;

import java.util.ArrayList;

import net.subject17.jdfs.client.io.Printer;
import net.subject17.jdfs.client.settings.reader.SettingsReader;
import net.subject17.jdfs.client.user.UserUtil;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


public class Peer {
	private String accountEmail;
	private String userName;
	private ArrayList<String> ip4s;
	private ArrayList<String> ip6s;
	
	//Getters
	public ArrayList<String> getIp4s() {return ip4s;}
	public ArrayList<String> getIp6s() {return ip6s;}
	public String getEmail() { return accountEmail; }
	public String getUsername() { return userName; }
	public void addIp4(String ip4) {ip4s.add(ip4);}
	public void addIp6(String ip6) {ip6s.add(ip6);}
	
	private static final char seperatorChar = '\n';
	
	//Constructors
	public Peer(Element peerTag) {
		ip4s = new ArrayList<String>();
		ip6s = new ArrayList<String>();
		
		NodeList ip4Tags = peerTag.getElementsByTagName("ip4");
		NodeList ip6Tags = peerTag.getElementsByTagName("ip6");
		setAccountEmail(SettingsReader.GetFirstNodeValue(peerTag, "accountEmail"));
		setUsername(SettingsReader.GetFirstNodeValue(peerTag, "userName"));
		
		for (int i = 0; i<ip4Tags.getLength(); ++i) {
			String ip4 = ip4Tags.item(0).getTextContent();
			if (!ip4.isEmpty())
				ip4s.add(ip4);
		}
		for (int i = 0; i < ip6Tags.getLength(); ++i){
			String ip6 = ip6Tags.item(0).getTextContent();
			if (!ip6.isEmpty())
				ip6s.add(ip6);
		}
	}

	public boolean isBlankPeer() {
		return (ip4s == null || ip4s.isEmpty())
				&& (ip6s == null || ip6s.isEmpty())
				&& accountEmail.isEmpty()
				&& userName.isEmpty();
		
	}
	
	public boolean setUsername(String usrname) {
		if (UserUtil.isValidUsername(usrname)) {
			userName = usrname;
			return true;
		}
		else return false; 
	}
	
	public boolean setAccountEmail(String email) {
		Printer.log("Validating email: "+email);
		if (UserUtil.isValidEmail(email)) {
			accountEmail = email;
			return true;
		}
		else return false; 
	}
	
	@Override
	public boolean equals(Object cmp) {
		return cmp != null 
				&& cmp instanceof Peer 
				&& this.userName.equals(((Peer)cmp).userName)
				&& this.accountEmail.equals(((Peer)cmp).accountEmail);
	}
	
	@Override
	public int hashCode() {
		return (""+(userName+seperatorChar+accountEmail).hashCode()+seperatorChar+ip4s.hashCode()+seperatorChar+ip6s.hashCode()).hashCode();
	} 
}

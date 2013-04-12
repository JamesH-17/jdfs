package net.subject17.jdfs.client.peers;

import java.util.HashSet;
import java.util.UUID;

import net.subject17.jdfs.client.io.Printer;
import net.subject17.jdfs.client.settings.reader.SettingsReader;
import net.subject17.jdfs.client.user.UserUtil;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


public class Peer {
	private String accountEmail;
	private String userName;
	private UUID GUID;
	private HashSet<UUID> machineGUIDs;
	private HashSet<String> ip4s;
	private HashSet<String> ip6s;
	
	//Getters
	public HashSet<String> getIp4s() {return ip4s;}
	public HashSet<String> getIp6s() {return ip6s;}
	public HashSet<UUID> getMachineGUIDs() {return machineGUIDs;}
	public UUID getGUID(){return GUID;}
	public String getEmail() { return accountEmail; }
	public String getUsername() { return userName; }
	public void addIp4(String ip4) {ip4s.add(ip4);}
	public void addIp6(String ip6) {ip6s.add(ip6);}
	
	private static final char seperatorChar = '\n';
	
	//Constructors
	public Peer(Element peerTag) {
		ip4s = new HashSet<String>();
		ip6s = new HashSet<String>();
		machineGUIDs = new HashSet<UUID>();
		
		NodeList ip4Tags = peerTag.getElementsByTagName("ip4");
		NodeList ip6Tags = peerTag.getElementsByTagName("ip6");
		NodeList machineTags = peerTag.getElementsByTagName("machine");
		
		
		String guid = SettingsReader.GetFirstNodeValue(peerTag, "peerGUID");
		try {
			setGUID(guid);
		} catch(IllegalArgumentException e) {
			setGUID(UUID.randomUUID());
		}
		
		setAccountEmail(SettingsReader.GetFirstNodeValue(peerTag, "accountEmail"));
		setUsername(SettingsReader.GetFirstNodeValue(peerTag, "userName"));
		
		for (int i = 0; i < machineTags.getLength(); ++i) {
			String machineID = machineTags.item(i).getTextContent();
			if (!machineID.isEmpty())
				machineGUIDs.add(UUID.fromString(machineID));
		}
		for (int i = 0; i < ip4Tags.getLength(); ++i) {
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
	
	public void setGUID(String guid) {
			setGUID(UUID.fromString(guid));
	}
	public void setGUID(UUID guid){
		this.GUID = guid;
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
		if (isBlankPeer())
			return 0;
		else {
			return (""+accountEmail+seperatorChar+userName+GUID.hashCode()+seperatorChar+machineGUIDs.hashCode()+seperatorChar+ip4s.hashCode()+seperatorChar+ip6s.hashCode()).hashCode();
		}
	} 
}

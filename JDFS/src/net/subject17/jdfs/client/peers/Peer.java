package net.subject17.jdfs.client.peers;

import java.util.HashSet;
import java.util.UUID;

import net.subject17.jdfs.client.io.Printer;
import net.subject17.jdfs.client.net.IPUtil;
import net.subject17.jdfs.client.settings.reader.SettingsReader;
import net.subject17.jdfs.client.user.UserUtil;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.map.ObjectMapper;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


public class Peer {
	
	//Model properties
	private String accountEmail;
	private String userName;
	private UUID GUID;
	private HashSet<UUID> machineGUIDs;
	private HashSet<String> ip4s;
	private HashSet<String> ip6s;
	
	//Model helpers
	@Deprecated
	@JsonIgnore
	private static final char seperatorChar = '\n';
	@JsonIgnore
	public static final ObjectMapper mapper = new ObjectMapper();
	
	
	////////////////
	//Constructors//
	////////////////
	
	@JsonIgnore
	public Peer (String accountEmail, String userName, UUID GUID) {
		resetMachineAndIpInfo();
		
		this.accountEmail = accountEmail;
		this.userName = userName;
		this.GUID = GUID;
	}
	@JsonIgnore
	public Peer (String accountEmail, String userName, UUID peerGUID, HashSet<UUID> machineGUIDs, HashSet<String> ip4s, HashSet<String> ip6s) {
		this(accountEmail, userName, peerGUID);
		this.machineGUIDs.addAll(machineGUIDs);
		this.ip4s.addAll(ip4s);
		this.ip6s.addAll(ip6s);
	}	
	
	@JsonIgnore
	public Peer(Element peerTag) {
		resetMachineAndIpInfo();
		
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
	
	
	///////////
	//Getters//
	///////////
	
	@JsonIgnore
	public HashSet<String> getIp4s() {return ip4s;}
	@JsonIgnore
	public HashSet<String> getIp6s() {return ip6s;}
	@JsonIgnore
	public HashSet<UUID> getMachineGUIDs() {return machineGUIDs;}
	@JsonIgnore
	public UUID getGUID(){return GUID;}
	@JsonIgnore
	public String getEmail() { return accountEmail; }
	@JsonIgnore
	public String getUsername() { return userName; }
	@JsonIgnore
	public void addIp4(String ip4) {ip4s.add(ip4);}
	@JsonIgnore
	public void addIp6(String ip6) {ip6s.add(ip6);}
	
	
	
	
	
	@JsonIgnore
	public void resetMachineAndIpInfo() {
		machineGUIDs = new HashSet<UUID>();
		ip4s = new HashSet<String>();
		ip6s = new HashSet<String>();
	}

	@JsonIgnore
	public boolean isBlankPeer() {
		return (ip4s == null || ip4s.isEmpty())
				&& (ip6s == null || ip6s.isEmpty())
				&& accountEmail.isEmpty()
				&& userName.isEmpty();
		
	}
	
	///////////
	//Setters//
	///////////
	
	@JsonIgnore
	public boolean setUsername(String usrname) {
		if (UserUtil.isValidUsername(usrname)) {
			userName = usrname;
			return true;
		}
		else return false; 
	}
	
	@JsonIgnore
	public boolean setAccountEmail(String email) {
		Printer.log("Validating email: "+email);
		if (UserUtil.isValidEmail(email)) {
			accountEmail = email;
			return true;
		}
		else return false; 
	}
	
	@JsonIgnore
	public void setGUID(String guid) {
			setGUID(UUID.fromString(guid));
	}
	@JsonIgnore
	public void setGUID(UUID guid){
		this.GUID = guid;
	}
	
	@JsonIgnore
	public void addMachine(UUID machineGUID) {
		machineGUIDs.add(machineGUID);
	}
	@JsonIgnore
	public void addMachine(HashSet<UUID> machineGUIDs) {
		this.machineGUIDs.addAll(machineGUIDs);
	}
	
	
	@JsonIgnore
	public boolean addIP4(String ip4) {
		return IPUtil.isValidIP4Address(ip4) && this.ip6s.add(ip4);
	}
	@JsonIgnore
	public boolean addIP4(HashSet<String> ip4s) {
		boolean allValid = true;
		
		for (String ip4 : ip4s) {
			if (IPUtil.isValidIP4Address(ip4))
				this.ip4s.add(ip4);
			else allValid = false;
		}
		
		return allValid;
	}
	
	
	@JsonIgnore
	public boolean addIP6(String ip6) {
		return IPUtil.isValidIP6Address(ip6) && this.ip6s.add(ip6);
	}
	@JsonIgnore
	public boolean addIP6(HashSet<String> ip6s) {
		boolean allValid = true;
		
		for (String ip6 : ip6s) {
			if (IPUtil.isValidIP6Address(ip6))
				this.ip6s.add(ip6);
			else allValid = false;
		}
		
		return allValid;
	}
	
	
	
	
	//Object overrides
	
	@JsonIgnore
	@Override
	public boolean equals(Object cmp) {
		return cmp != null 
				&& cmp instanceof Peer
				&& this.GUID.equals(((Peer)cmp).GUID)
				&& this.userName.equals(((Peer)cmp).userName)
				&& this.accountEmail.equals(((Peer)cmp).accountEmail);
	}
	
	@JsonIgnore
	@Override
	public int hashCode() {
		if (isBlankPeer())
			return 0;
		else {
			return (""+accountEmail+seperatorChar+userName+GUID.hashCode()+seperatorChar+machineGUIDs.hashCode()+seperatorChar+ip4s.hashCode()+seperatorChar+ip6s.hashCode()).hashCode();
		}
	} 
}

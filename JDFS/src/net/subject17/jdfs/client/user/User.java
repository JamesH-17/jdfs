package net.subject17.jdfs.client.user;

import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;

import net.subject17.jdfs.client.settings.Settings;
import net.subject17.jdfs.client.settings.reader.SettingsReader;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public final class User {
	public static class UserException extends Exception {
		private static final long serialVersionUID = 1L;
		UserException(String msg){super(msg);}
		UserException(String msg, Throwable thrw){super(msg,thrw);}
	}
	private String username;
	private String account;
	private UUID GUID;
	private HashSet<UUID> MachineGUIDs;
	//private static final char seperatorChar = '\n';
	public User(String name, String email) throws UserException {
		this(name, email, UUID.randomUUID());
	}
	public User(String name, String email, UUID guid) throws UserException {
		if (UserUtil.isValidEmail(email) && UserUtil.isValidUsername(email)) {
			username = name;
			account = email;
			GUID = guid;
			
			MachineGUIDs = new HashSet<UUID>();
			MachineGUIDs.add(Settings.getMachineGUIDSafe());
			
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
		String guid = SettingsReader.GetFirstNodeValue(node, "GUID");
		
		GUID = guid.equals("") ? UUID.randomUUID() : UUID.fromString(guid);
		
		//Validate that this is an actual user before continuing 
		if (!UserUtil.isValidEmail(account) || !UserUtil.isValidUsername(username)) {
			this.account = null;
			this.username = null;
			this.GUID = null;
			throw new UserException("Invalid data for user -- provided email:["+account+"], name: ["+username+"], GUID:["+GUID.toString()+"]");
			
		} else { //Now, add on the GUIDs.  This is done here instead of above since this program does things like I do:
				 //As lazy as possible.
			MachineGUIDs = new HashSet<UUID>();
			Element machinesTag = SettingsReader.GetFirstNode(node, "linkedMachines");
			if (machinesTag != null) {
				NodeList machines = machinesTag.getElementsByTagName("machine");
				
				for (int i = 0; i < machines.getLength(); ++i) {
					if (!(null == machines.item(i) || null == machines.item(i).getTextContent())) {
						//Convert Value to uuid and get
						MachineGUIDs.add(UUID.fromString(machines.item(i).getTextContent()));
					}
				}
			}
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
	public int hashCode() { //Note how our equals method is more restrictive than our hashcode method
		//return (username+seperatorChar+account).hashCode();
		return GUID.hashCode();
	} 
	
	public String getUserName() {return username;}
	public String getAccountEmail() {return account;}
	public UUID getGUID() {return GUID;}
	public boolean isEmpty(){ //Really, not needed, but just in case
		return (username == null || username.isEmpty())||(account == null || account.isEmpty());
	}
	
	public final void registerUserToMachine(UUID newMachineGUID){
		MachineGUIDs.add(newMachineGUID);
	}
	public void registerUserToMachine(Collection<UUID> newMachineGUIDs){
		MachineGUIDs.addAll(newMachineGUIDs);
	}
	public boolean delistUserFromMachine(UUID machineGUIDToRemove){
		return MachineGUIDs.remove(machineGUIDToRemove);
	}
	
	public HashSet<UUID> getRegisteredMachines(){return MachineGUIDs;}
}

package net.subject17.jdfs.client.user;

import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;

import net.subject17.jdfs.client.settings.Settings;
import net.subject17.jdfs.client.settings.reader.SettingsReader;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public final class User {
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class UserException extends Exception {
		private static final long serialVersionUID = 1L;
		UserException(String msg){super(msg);}
		UserException(String msg, Throwable thrw){super(msg,thrw);}
	}
	
	private String userName;

	private String accountEmail;
	private UUID guid;
	private HashSet<UUID> MachineGUIDs;
	//private static final char seperatorChar = '\n';
	public User(){MachineGUIDs=new HashSet<UUID>();}
	public User(String name, String email) throws UserException {
		this(name, email, UUID.randomUUID());
	}
	public User(String name, String email, UUID GUID) throws UserException {
		if (UserUtil.isValidEmail(email) && UserUtil.isValidUsername(email)) {
			this.userName = name;

			this.accountEmail = email;
			this.guid = GUID;
			
			MachineGUIDs = new HashSet<UUID>();
			MachineGUIDs.add(Settings.getMachineGUIDSafe());
			
		} else {
			if (email == null) email = new String("null");
			if (name == null) name = new String("null");
			throw new UserException("Invalid data for user -- provided email:["+email+"], name: ["+name+"]");
		}
	}
	@JsonIgnore
	public User(Element node) throws UserException {
		if (node == null || !node.getTagName().equals("user"))
			throw new UserException("Invalid data for element " + node == null ? "[null]" : node.toString());

		userName = SettingsReader.GetFirstNodeValue(node, "userName");

		accountEmail = SettingsReader.GetFirstNodeValue(node, "email");
		String guidString = SettingsReader.GetFirstNodeValue(node, "GUID");
		
		this.guid = guidString.equals("") ? UUID.randomUUID() : UUID.fromString(guidString);
		
		//Validate that this is an actual user before continuing 
		if (!UserUtil.isValidEmail(accountEmail) || !UserUtil.isValidUsername(userName)) {
			this.accountEmail = null;
			this.userName = null;
			this.guid = null;

			throw new UserException("Invalid data for user -- provided email:["+accountEmail+"], name: ["+userName+"], GUID:["+guid.toString()+"]");
			
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
				&& this.userName.equals(((User)cmp).userName)
				&& this.accountEmail.equals(((User)cmp).accountEmail)
				&& this.guid.equals(((User)cmp).guid);
	}
	
	@Override
	public int hashCode() { //Note how our equals method is more restrictive than our hashcode method
		//return (username+seperatorChar+account).hashCode();
		return guid.hashCode();
	} 
	@Override
	public String toString(){
		return "{\n\tGUID: "+this.guid+",\n\tUserName: "+this.userName+",\n\tAccountEmail: "+this.accountEmail+"\n}";
	}
	
	public String getUserName() {return userName;}
	public String getAccountEmail() {return accountEmail;}

	public UUID getGUID() {return guid;}
	@JsonIgnore
	public HashSet<UUID> getRegisteredMachines() {
		if (null == MachineGUIDs)
			MachineGUIDs = new HashSet<UUID>();
		return MachineGUIDs;
	}
	
	@JsonIgnore
	public final boolean isEmpty(){ //Really, not needed, but just in case
		return (userName == null || userName.isEmpty())||(accountEmail == null || accountEmail.isEmpty());
	}
	@JsonIgnore
	public final void registerUserToMachine(UUID newMachineGUID){
		MachineGUIDs.add(newMachineGUID);
	}
	@JsonIgnore
	public final void registerUserToMachine(Collection<UUID> newMachineGUIDs){
		MachineGUIDs.addAll(newMachineGUIDs);
	}
	@JsonIgnore
	public final boolean delistUserFromMachine(UUID machineGUIDToRemove){
		return MachineGUIDs.remove(machineGUIDToRemove);
	}
	
}

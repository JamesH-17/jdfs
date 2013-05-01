package net.subject17.jdfs.client.peers;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.UUID;

import net.subject17.jdfs.JDFSUtil;
import net.subject17.jdfs.client.file.db.DBManager;
import net.subject17.jdfs.client.file.db.DBManager.DBManagerFatalException;
import net.subject17.jdfs.client.file.model.FileRetrieverInfo;
import net.subject17.jdfs.client.file.model.FileRetrieverRequest;
import net.subject17.jdfs.client.io.Printer;
import net.subject17.jdfs.client.net.IPUtil;
import net.subject17.jdfs.client.net.model.MachineInfo;
import net.subject17.jdfs.client.settings.Settings;
import net.subject17.jdfs.client.settings.reader.PeerSettingsReader;
import net.subject17.jdfs.client.settings.reader.SettingsReader;
import net.subject17.jdfs.client.settings.reader.SettingsReader.SettingsReaderException;
import net.subject17.jdfs.client.settings.writer.PeerSettingsWriter;
import net.subject17.jdfs.client.user.User;
import org.codehaus.jackson.map.ObjectMapper;


public class PeersHandler {
	
	private static Path peersFile;
	private static PeerSettingsReader peersReader;
	
	@Deprecated
	private static HashSet<Peer> peers = new HashSet<Peer>();

	public static Path getPeersFile() { return peersFile; }
	
	
	
	//////////////////////////////////////////////////////////////////////////////////////
	//								Handle Incoming Peer								//
	//////////////////////////////////////////////////////////////////////////////////////
	
	
	public static void addIncomingMachine(InetAddress ip, String machineInfoJson) throws DBManagerFatalException, SQLException {
		ObjectMapper mapper = new ObjectMapper();
		MachineInfo info = mapper.convertValue(machineInfoJson, MachineInfo.class);
		addIncomingMachine(ip, info);
	}
	public static void addIncomingMachine(InetAddress ip, MachineInfo info) throws DBManagerFatalException, SQLException {
		UUID machineGuid = info.MachineGUID;
		
		addMachine(machineGuid);
		addIpToMachine(machineGuid, ip.getHostAddress());
		addPeersToMachine(info.users, machineGuid);
	}
	
	//returns true if the machine was successfully added to the db, false if it already exists or the operation failed
	private static boolean addMachine(UUID machineGuid) throws DBManagerFatalException {
		try (ResultSet machinesExist = DBManager.getInstance().select("SELECT MachineGUID FROM Machines WHERE Machines.MachineGUID LIKE '"+"'")
		) {
			if (!machinesExist.next()) { //If no next, result set was empty => MachineGUID not in our DB.  So, add it
				DBManager.getInstance().upsert("INSERT INTO Machines(MachineGUID) VALUES ('"+machineGuid.toString()+"')");
				return true;
			}
			
		} catch (SQLException e) {
			Printer.logErr("Failed to add machine "+machineGuid.toString()+" to database.");
			Printer.logErr(e);
		}
		return false;
	}

	private static void addPeersToMachine(ArrayList<User> usersToAdd, UUID machineGuid) throws DBManagerFatalException, SQLException {
		
		try (ResultSet peersRegisteredToMachine = DBManager.getInstance().select(
				"SELECT PeerPK, PeerGUID, UserName, AccountEmail FROM Peers WHERE Peers.MachineGUID = '"+machineGuid+"'"
		)) {
			
			while (peersRegisteredToMachine.next()) {
				
				//Don't need to check these for null due to DB structure
				String peerGUID = peersRegisteredToMachine.getString("PeerGUID");
				String userName = peersRegisteredToMachine.getString("UserName");
				String email = peersRegisteredToMachine.getString("AccountEmail");
				
				for (User user : usersToAdd) {
					if (peerGUID.equals(user.getGUID().toString()) &&
						email.equals(user.getAccountEmail()) &&
						userName.equals(user.getUserName())
					) {
						usersToAdd.remove(user);
					}
				}
			} 
			
		}
		
		
		//Step 2: Assign any remaining peers to this machine
		for (User peer : usersToAdd) {
			try {
				DBManager.getInstance().upsert("INSERT INTO Peers (PeerGUID, UserName, AccountEmail, MachineGUID) "+
						"VALUES ('"+
							peer.getGUID()+"','"+
							peer.getUserName()+"','"+
							peer.getAccountEmail()+"','"+
							machineGuid+
						"')"	
				);
				
			} catch (SQLException e) {
				Printer.logErr("Error assigning peer "+peer+" to machine "+machineGuid);
				throw e;
			}
		}
	}

	private static void addIpToMachine(UUID machineGuid, String ip) throws DBManagerFatalException, SQLException {
		if (IPUtil.isValidIP6Address(ip)) {
			addIP4ToMachine(machineGuid, ip);
		}
		else if (IPUtil.isValidIP4Address(ip)) {
			addIP6ToMachine(machineGuid, ip);
		}
	}

	private static void addIP6ToMachine(UUID machineGuid, String validIP6String) throws DBManagerFatalException, SQLException {
		try (ResultSet ip6s = DBManager.getInstance().select("SELECT IP6 FROM MachineIP6Links WHERE MachineGUID LIKE '"+machineGuid+"'")) {
			
			if (!ip6s.next()) {
				DBManager.getInstance().upsert("INSERT INTO MachineIP6Links(MachineGUID, IP6) "+
					"VALUES ('"+machineGuid+"','"+validIP6String+"')"
				);
			}
			
		}
	}

	private static void addIP4ToMachine(UUID machineGuid, String validIP4String) throws DBManagerFatalException, SQLException {
		try (ResultSet ip4s = DBManager.getInstance().select("SELECT IP4 FROM MachineIP4Links WHERE MachineGUID LIKE '"+machineGuid+"'")) {
			
			if (!ip4s.next()) {
				DBManager.getInstance().upsert("INSERT INTO MachineIP4Links(MachineGUID, IP4) "+
					"VALUES ('"+machineGuid+"','"+validIP4String+"')"
				);
			}
			
		}
	}
	
	////////////////////////////////////////////////////////////////////////////
	
	
	public Path getStorageLocation(UUID peerGUID, UUID fileGUID) throws SettingsReaderException, IOException {
		Path storageDirectory = SettingsReader.getInstance().getStorageDirectory();
		
		//TODO minor: see if we can combine statements.  
		if (!Files.exists(storageDirectory))
			Files.createDirectory(storageDirectory);
		
		storageDirectory = storageDirectory.resolve(peerGUID.toString());
		
		if (!Files.exists(storageDirectory))
			Files.createDirectory(storageDirectory);
		
		return storageDirectory.resolve(fileGUID.toString()+".xz.enc");
	}
	
	private ResultSet getFiles(UUID peerGuid, UUID fileGuid) throws SQLException, DBManagerFatalException {
		return DBManager.getInstance().select("SELECT PeerFiles.LocalFilePath AS FilePath FROM Peers INNER JOIN PeerFileLinks ON Peers.PeerPK = PeerFileLinks.PeerPK INNER JOIN PeerFiles ON PeerFileLinks.PeerFilePK = PeerFiles.FilePK WHERE PeerFiles.FileGUID LIKE '"+fileGuid+"' AND Peers.PeerGUID LIKE '"+peerGuid+"'");
	}
	
	public ResultSet getFiles(FileRetrieverRequest criteria, String limit, String order) throws SQLException, DBManagerFatalException {
		if (null == limit) limit = "";
		if (null == order) order = "";
		
		if (!( null == criteria.fileGuid || null == criteria.userGuid)) {
			
			StringBuilder restrictions = new StringBuilder();

			
			if (null != criteria.lastUpdatedDate) {
				restrictions.append(" AND '"+criteria.lastUpdatedDate+"' "+criteria.comparison+" PeerFiles.LastUpdatedDate ");
			}
			
			if (null != criteria.parentGUID) {
				restrictions.append(" AND PeerFiles.ParentGUID LIKE '"+criteria.parentGUID+"'");
				restrictions.append(" AND PeerFiles.ParentLocation LIKE '"+criteria.relativeParentLoc+"'");
			}
			
			if (null != criteria.sendingMachineGuid) {
				restrictions.append(" AND Machines.MachineGUID LIKE '"+criteria.sendingMachineGuid+"'");
			}
			
			
			return DBManager.getInstance().select("SELECT "+limit+" PeerFiles.LocalFilePath AS FilePath, PeerFiles.IV AS IV, PeerFiles.UpdatedDate AS UpdatedDate, PeerFiles.FileGUID AS FileGUID, PeerFiles.CheckSum AS CheckSum "+
					"FROM Peers "+
					"INNER JOIN PeerFileLinks ON Peers.PeerPK = PeerFileLinks.PeerPK "+
					"INNER JOIN PeerFiles ON PeerFileLinks.PeerFilePK = PeerFiles.FilePK "+
					"LEFT JOIN Machines ON PeerFileLinks.MachinePK = Machines.MachinePK "+
					"WHERE PeerFiles.FileGUID LIKE '"+criteria.fileGuid+"' AND Peers.PeerGUID LIKE '"+criteria.userGuid+"'"+
					restrictions
			);
		}
		return null;
	}
	
	public FileRetrieverInfo getFile(FileRetrieverRequest criteria) throws DBManagerFatalException {
		try (ResultSet filesFound = getFiles(criteria, "TOP 1", "ORDER BY UpdatedDate DESC")) {
			if (filesFound != null && filesFound.next()) {
				return new FileRetrieverInfo(filesFound);
			}
		} catch (SQLException | IOException e) {
			Printer.logErr("Error retrieving file specified by criteria");
			Printer.logErr(e);
		}
		return null;
	}
	

	
	//////////////////////////////////////////////////////////////////////
	//									XML								//
	//////////////////////////////////////////////////////////////////////
	
	public static void writePeersToFile() {
		writePeersToFile(peersFile);
	}
	
	public static void writePeersToFile(Path file) {
		PeerSettingsWriter writer = new PeerSettingsWriter();
		try {
			writer.writePeerSettings(file,getAllPeersInDB());
		}
		catch (DBManagerFatalException e) {
			Printer.logErr("Error encountered grabbing peers in db, writing what is in local var to xml");
			Printer.logErr(e);
			
			//TODO mayhaps don't write anything since it overwrites existing file?
			writer.writePeerSettings(file,peers);
		}
	}

	public static void setPeersSettingsFile(Path peerSettingsFile) throws Exception {
		peersReader = new PeerSettingsReader(peerSettingsFile);
		peersFile = peerSettingsFile;
		HashSet<Peer> importedPeers = peersReader.getPeers();
		
		addPeersToDB(importedPeers);
		
		peers.addAll(importedPeers);
	}
	
	//XML helpers
	private static void addPeersToDB(HashSet<Peer> importedPeers) throws DBManagerFatalException {
		for(Peer peer : importedPeers) {
			
			//////////////////////////////////////
			
			//Add to peers and machines table first
			try (ResultSet resSet = DBManager.getInstance().select("SELECT DISTINCT MachineGUID FROM Peers WHERE "+
					"Peers.PeerGUID LIKE '"+peer.getGUID().toString()+"' AND "+
					"Peers.UserName LIKE '"+peer.getUsername()+"' AND "+
					"Peers.AccountEmail LIKE '"+peer.getEmail()+"'"
			)){
				
				//Grab all machine ids this peer has, and remove any that already exist in our db
				HashSet<UUID> machineIds = peer.getMachineGUIDs();
				while (resSet.next()) {
					machineIds.remove(UUID.fromString(resSet.getObject("MachineGUID").toString()));
				}
				
				//Now, add any new ids
				for (UUID machineId : machineIds) {
					DBManager.getInstance().upsert("INSERT INTO Peers (PeerGUID, UserName, AccountEmail, MachineGUID) VALUES ("+
							"'"+peer.getGUID().toString()+"',"+
							"'"+peer.getUsername()+"',"+
							"'"+peer.getEmail()+"',"+
							"'"+machineId.toString()+"'"+
					")");
					
					DBManager.getInstance().upsert("INSERT INTO Machines (MachineGUID) VALUES ("+
							"'"+machineId.toString()+"'"+
					")");
					
				}
				
			} catch (SQLException e) {
				Printer.logErr(e);
			}
			
			///////////////////////////////////////
			
			//Next up, link the ips if they aren't already
			try (ResultSet MachinePKset = DBManager.getInstance().select("SELECT MachinePK FROM Machines WHERE "+
					"MachineGUID IN ("+JDFSUtil.stringJoin(peer.getMachineGUIDs(),",")+")"
			)){
				
				while (MachinePKset.next()) {
					
					try (ResultSet ip4sToRemove = DBManager.getInstance().select("SELECT DISTINCT IP4 FROM MachineIP4Links WHERE "+
							" MachinePK = "+MachinePKset.getInt("MachinePK")
					);
					ResultSet ip6sToRemove = DBManager.getInstance().select("SELECT DISTINCT IP6 FROM MachineIP6Links WHERE "+
							" MachinePK = "+MachinePKset.getInt("MachinePK")
					)) {
						
						
						//Grab every ip4 and ip6 this guy has, then remove 
						HashSet<String> ip4s = peer.getIp4s();
						HashSet<String> ip6s = peer.getIp6s();
						
						while (ip4sToRemove.next())
							ip4s.remove(ip4sToRemove.getObject("IP4"));
						
						while (ip6sToRemove.next())
							ip6s.remove(ip6sToRemove.getObject("IP6"));
						
						for(String ip4 : ip4s) {
							DBManager.getInstance().upsert(
									"INSERT INTO MachineIP4Links(MachinePK, IP4) "+
									"VALUES ("+MachinePKset.getInt("MachinePK")+","+ip4+")"
							);
						}
						
						for(String ip6 : ip6s) {
							DBManager.getInstance().upsert(
									"INSERT INTO MachineIP6Links(MachinePK, IP6) "+
									"VALUES ("+MachinePKset.getInt("MachinePK")+","+ip6+")"
							);
						}
						
					} catch (SQLException e) {

						Printer.logErr(e);
					}
				}
				
			} catch (SQLException e) {
				Printer.logErr(e);
			}
			
		}
	}
	
	private static HashSet<Peer> getAllPeersInDB() throws DBManagerFatalException {
		HashSet<Peer> peersFound = new HashSet<Peer>();
		
		try (ResultSet peerUsers = DBManager.getInstance().select("SELECT DISTINCT Peers.* FROM Peers")){
			
			while (peerUsers.next()) {
				HashSet<UUID> machineGUIDs = getMachinesForPeer(peerUsers.getInt("PeerPK"));
				HashSet<String> ip4s = new HashSet<String>();
				HashSet<String> ip6s = new HashSet<String>();
				
				for (UUID machineGUID : machineGUIDs) {
					ip4s.addAll(getIP4sForMachine(machineGUID));
					ip6s.addAll(getIP6sForMachine(machineGUID));
				}
				
				Peer temp = new Peer(
						peerUsers.getString("UserName"),
						peerUsers.getString("AccountEmail"),
						UUID.fromString(peerUsers.getString("PeerGUID")), //TODO catch this?
						machineGUIDs,
						ip4s,
						ip6s
				);
				
				peersFound.add(temp);
			}
		}
		catch (SQLException e) {
			Printer.logErr("Error grabbing peers from db to write");
			Printer.logErr(e);
		}
		return peersFound;
	}


	/////////////////////////
	//To grab machines with//
	/////////////////////////
	private static HashSet<UUID> getMachinesForPeer(int peerPK) throws DBManagerFatalException {
		return getMachinesForPeer("WHERE Peers.PeerPK = "+peerPK);
	}
	public static HashSet<UUID> getMachinesForPeer(UUID peerGUID) throws DBManagerFatalException {
		return getMachinesForPeer("WHERE Peers.PeerGUID LIKE '"+peerGUID+"'");
	}
	private static HashSet<UUID> getMachinesForPeer(String restriction) throws DBManagerFatalException {
		HashSet<UUID> machineGUIDs = new HashSet<UUID>();
		
		try (ResultSet machinesFound = DBManager.getInstance().select(
				"SELECT DISTINCT Machines.* FROM Peers "+
				"INNER JOIN MachinePeerLinks ON Peers.PeerPK = MachinePeerLinks.PeerPK "+
				"INNER JOIN Machines ON MachinePeerLinks.MachinePK = Machines.MachinePK "+
				restriction
			)
		){
			while (machinesFound.next()){
				try {
					machineGUIDs.add(UUID.fromString(machinesFound.getString("MachineGUID")));
				}
				catch (IllegalArgumentException e) {
					Printer.logErr("Error converting machineGUID to UUID.  Skipping. (Value was "+machinesFound.getString("MachineGUID")+")");
				}
			}
		} 
		catch (SQLException e) {
			Printer.logErr("Error grabbing peer machines");
			Printer.logErr(e);
		}
		return machineGUIDs;
	}
	
	/////////////////////////
	//To grab ip4/ip6s with//
	/////////////////////////
	
	private static HashSet<String> getIP6sForMachine(UUID machineGUID) throws DBManagerFatalException {
		HashSet<String> ip6s = new HashSet<String>();
		try (ResultSet ip6sFound = DBManager.getInstance().select(
				"SELECT DISTINCT MachineIP6Links.IP6 FROM MachineIP6Links "+
				"INNER JOIN Machines ON MachineIP6Links.MachinePK = Machines.MachinePK "+
				"WHERE Machines.MachineGUID LIKE '"+machineGUID+"'"
			)
		) {
			while (ip6sFound.next()) {
				ip6s.add(ip6sFound.getString("IP6"));
			}
		} catch (SQLException e) {
			Printer.logErr("Error grabbing ips for machine");
			Printer.logErr(e);
		}
		return ip6s;
	}

	private static HashSet<String> getIP4sForMachine(UUID machineGUID) throws DBManagerFatalException {
		HashSet<String> ip4s = new HashSet<String>();
		try (ResultSet ip4sFound = DBManager.getInstance().select(
				"SELECT DISTINCT MachineIP4Links.IP4 FROM MachineIP4Links "+
				"INNER JOIN Machines ON MachineIP4Links.MachinePK = Machines.MachinePK "+
				"WHERE Machines.MachineGUID LIKE '"+machineGUID+"'"
			)
		) {
			while (ip4sFound.next()) {
				ip4s.add(ip4sFound.getString("IP4"));
			}
		} catch (SQLException e) {
			Printer.logErr("Error grabbing ips for machine");
			Printer.logErr(e);
		}
		return ip4s;
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////
	//						Peer Discovery (currently not implemented)						//
	//////////////////////////////////////////////////////////////////////////////////////////
	
	public static HashSet<Peer> startPeerSearchService() {
		HashSet<Peer> peersFound = new HashSet<Peer>();
		
		//scan a bunch of random ips and ports, attempting to find new jdfs users
		
		
		return peersFound;
	}
	public static HashSet<String> searchForIPs() {
		HashSet<String> peersIpsFound = new HashSet<String>();
		
		// similar to startPeerSearchService, but also simply tracks ips
		
		return peersIpsFound;
	}
	
	public final static HashSet<String> getLinkedMachineIPs() {
		HashSet<String> peerIPs = new HashSet<String>();
		
		try (ResultSet linkedMachinesIp4 = DBManager.getInstance().select(
				"SELECT TOP "+DBManager.maxRecordsToGrab+" "+
				"MachineIP4Links.IP4 AS IP4 "+
				"FROM Users "+
				"INNER JOIN Machines ON Users.MachinePK = Machines.MachinePK "+
				"INNER JOIN MachineIP4Links ON Machines.MachinePK = MachineIP4Links.MachinePK "+
				"WHERE Machines.MachineGUID NOT LIKE '"+Settings.getMachineGUIDSafe()+"'"
			);
			ResultSet linkedMachinesIp6 = DBManager.getInstance().select(
				"SELECT TOP "+DBManager.maxRecordsToGrab+" "+
				"MachineIP6Links.IP6 AS IP6 "+
				"FROM Users "+
				"INNER JOIN Machines ON Users.MachinePK = Machines.MachinePK "+
				"INNER JOIN MachineIP6Links ON Machines.MachinePK = MachineIP6Links.MachinePK "+
				"WHERE Machines.MachineGUID NOT LIKE '"+Settings.getMachineGUIDSafe()+"'"
			)
		) {
			while (linkedMachinesIp4.next()) {
				peerIPs.add(linkedMachinesIp4.getString("IP4"));
			}
			while (linkedMachinesIp6.next()) {
				peerIPs.add(linkedMachinesIp6.getString("IP6"));
			}
			
		} catch (SQLException | DBManagerFatalException e) {
			Printer.logErr("[PeersHandler getLinkedMachineIPs]: Error encountered grabbing linked machines.  Probably returning empty/reduced set");
			Printer.logErr(e);
		}
		return peerIPs;
	}
	
	public static HashSet<String> getSomeStoredIps(int maxToGrab) {
		HashSet<String> peerIPs = new HashSet<String>();
		try (	ResultSet peersIP6 = DBManager.getInstance().select(
				"SELECT TOP "+maxToGrab+" "+
				"MachineIP6Links.IP6 AS IP6 "+
				"FROM MachineIP6Links "
			);
			ResultSet peersIP4 = DBManager.getInstance().select(
				"SELECT TOP "+maxToGrab+" "+
				"MachineIP4Links.IP4 AS IP4 "+
				"FROM MachineIP4Links "
			)
		) {
			while (peersIP4.next() && peerIPs.size() < maxToGrab) {
				peerIPs.add(peersIP4.getString("IP4"));
			}
			while (peersIP6.next() && peerIPs.size() < maxToGrab) {
				peerIPs.add(peersIP6.getString("IP6"));
			}
			
		} catch (SQLException | DBManagerFatalException e) {
			Printer.logErr("Error grabbing random peers in [PeersHandler getSomeStoredIps]");
			Printer.logErr(e);
		}
		return peerIPs;
	}
	
	
	//////////////////////////////////////////////////////////////////////////////////////////
	//										OLD CODE										//
	//////////////////////////////////////////////////////////////////////////////////////////
	
	@Deprecated
	public static Peer getPeerByAccount(String account) {
		for (Peer peer : peers){
			if (peer.getEmail().equals(account))
				return peer;
		}
		return null;
	}
	@Deprecated
	public static Peer getPeerByUsername(String username) {
		for (Peer peer : peers){
			if (peer.getUsername().equals(username))
				return peer;
		}
		return null;
	}
	@Deprecated
	public static HashSet<Peer> getPeersByIp4(String ip4) {
		HashSet<Peer> peersMatchingIp4 = new HashSet<Peer>();
		for (Peer peer : peers){
			if (peer.getIp4s().contains(ip4))
				peersMatchingIp4.add(peer);
		}
		return peersMatchingIp4;
	}
	@Deprecated
	public static HashSet<Peer> getPeersByIp6(String ip6) {
		HashSet<Peer> peersMatchingIp6 = new HashSet<Peer>();
		for (Peer peer : peers) {
			if (peer.getIp6s().contains(ip6))
				peersMatchingIp6.add(peer);
		}
		return peersMatchingIp6;
	}
}

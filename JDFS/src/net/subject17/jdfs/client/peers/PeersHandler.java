package net.subject17.jdfs.client.peers;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.UUID;

import org.hsqldb.result.Result;

import net.subject17.jdfs.JDFSUtil;
import net.subject17.jdfs.client.file.db.DBManager;
import net.subject17.jdfs.client.file.db.DBManager.DBManagerFatalException;
import net.subject17.jdfs.client.settings.reader.PeerSettingsReader;
import net.subject17.jdfs.client.settings.writer.PeerSettingsWriter;


public class PeersHandler {
	
	private static Path peersFile;
	private static PeerSettingsReader peersReader;
	private static HashSet<Peer> peers = new HashSet<Peer>();

	public static Path getPeersFile() { return peersFile; }
	public static void addIncomingPeer(InetAddress ip, int port) {
		//TODO add peer to peersfileList
	}
	
	public static Peer getPeerByAccount(String account) {
		for (Peer peer : peers){
			if (peer.getEmail().equals(account))
				return peer;
		}
		return null;
	}
	
	public static Peer getPeerByUsername(String username) {
		for (Peer peer : peers){
			if (peer.getUsername().equals(username))
				return peer;
		}
		return null;
	}
	
	public static HashSet<Peer> getPeersByIp4(String ip4) {
		HashSet<Peer> peersMatchingIp4 = new HashSet<Peer>();
		for (Peer peer : peers){
			if (peer.getIp4s().contains(ip4))
				peersMatchingIp4.add(peer);
		}
		return peersMatchingIp4;
	}
	
	public static HashSet<Peer> getPeersByIp6(String ip6) {
		HashSet<Peer> peersMatchingIp6 = new HashSet<Peer>();
		for (Peer peer : peers) {
			if (peer.getIp6s().contains(ip6))
				peersMatchingIp6.add(peer);
		}
		return peersMatchingIp6;
	}
	
	public void writePeersToFile() {
		writePeersToFile(peersFile);
	}
	
	public void writePeersToFile(Path file) {
		PeerSettingsWriter writer = new PeerSettingsWriter();
		writer.writePeerSettings(file,peers);
	}

	public static void setPeersSettingsFile(Path peerSettingsFile) throws Exception {
		peersReader = new PeerSettingsReader(peerSettingsFile);
		peersFile = peerSettingsFile;
		HashSet<Peer> importedPeers = new HashSet<Peer>();
		
		addPeersToDB(importedPeers);
		
		peers.addAll(importedPeers);
	}
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
					machineIds.remove(UUID.fromString(resSet.getObject("").toString()));
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
				
			} catch (SQLException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
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
						
					} catch (SQLException | IOException e) {
						
					}
				}
				
			} catch (SQLException | IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			
		}
	}
}

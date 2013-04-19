package net.subject17.jdfs.client.file.handler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashSet;
import java.util.UUID;

import javax.crypto.NoSuchPaddingException;

import org.bouncycastle.pqc.math.linearalgebra.ByteUtils;
import org.bouncycastle.util.Arrays;

import net.subject17.jdfs.JDFSUtil;
import net.subject17.jdfs.client.account.AccountManager;
import net.subject17.jdfs.client.file.FileUtil;
import net.subject17.jdfs.client.file.db.DBManager;
import net.subject17.jdfs.client.file.db.DBManager.DBManagerFatalException;
import net.subject17.jdfs.client.file.model.EncryptedFileInfoStruct;
import net.subject17.jdfs.client.file.model.FileSenderInfo;
import net.subject17.jdfs.client.io.Printer;
import net.subject17.jdfs.client.settings.Settings;
import net.subject17.jdfs.client.settings.reader.SettingsReader;
import net.subject17.jdfs.client.settings.reader.SettingsReader.SettingsReaderException;
import net.subject17.jdfs.client.user.User.UserException;
import net.subject17.jdfs.security.JDFSSecurity;

public final class FileHandler {
	public final static class FileHandlerException extends Exception {
		private static final long serialVersionUID = -8693105516511333054L;
		public FileHandlerException()								{super();}
		public FileHandlerException(String message)					{super(message);}
		public FileHandlerException(Exception e)					{super(e);}
		public FileHandlerException(String message, Throwable thrw)	{super(message, thrw);}
	}
	
	private static FileHandler _instance;
	
	public final static int minNumPeersToGrab = 100;
	
	
	private FileHandler(){
	}
	
	public static FileHandler getInstance(){
		if (null == _instance) {
			synchronized(FileHandler.class) {
				if (null == _instance) {
					_instance = new FileHandler();
				}
			}
		}
		return _instance;
	}
	
	public boolean canStoreFile(FileSenderInfo info){
		return true; //TODO for future
	}
	
	public FileSenderInfo prepareToSendFile(Path context) throws FileHandlerException {
		try (ResultSet filesToSend = DBManager.getInstance().select(
				"SELECT TOP 1 "+
				"UserFiles.FileGUID AS FileGUID, UserFiles.UpdatedDate AS UpdatedDate, UserFiles.Priority AS Priority"+
				"Users.UserGUID AS UserGUID, Users.UserName AS UserName, Users.AccountEmail AS AccountEmail"+
				"FROM UserFiles INNER JOIN UserFileLinks ON UserFiles.UserFilePK = UserFileLinks.UserFilePK "+
				"INNER JOIN Users ON Users.UserPK = UserFileLinks.UserPK "+
				"WHERE UserFiles.LocalFilePath = '"+context.toString()+"' "+
				"AND COALESCE(UserFiles.IV,'') LIKE ''" //TODO exception case here.  We've received a file, but haven't decoded it, and old one is marked for sending
		)){
			
			EncryptedFileInfoStruct fileData = FileUtil.getInstance().compressAndEncryptFile(
					context,
					AccountManager.getInstance().getPasswordDigest()
			);
			UUID MachineGUID = Settings.getMachineGUIDSafe();
			byte[] CheckSum = FileUtil.getInstance().getMD5Checksum(context); //Note that the
			
			if (filesToSend.next()) {
				UUID fileGUID = UUID.fromString(filesToSend.getString("FileGUID"));
				UUID userGUID = UUID.fromString(filesToSend.getString("UserGUID"));
				
				String email = filesToSend.getString("AccountEmail");
				String userName = filesToSend.getString("UserName");
				
				Date UpdatedDate = new Date(Files.getLastModifiedTime(context).toMillis());
				
				int priority = filesToSend.getInt("Priority");
				return new FileSenderInfo(fileData,context,fileGUID,userGUID,MachineGUID,UpdatedDate,priority,CheckSum);
			}
			else {
				throw new FileHandlerException("Unable to send file");
			}
		} catch (SQLException | InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | IOException | UserException | DBManagerFatalException e){
			throw new FileHandlerException("Unable to send file",e);
		}
	}   
	
	public HashSet<String> getPeersToSendFileTo(UUID fileGuid) throws FileHandlerException {
		
		//We're not making a distinction between ip4 and ip6 addresses here.
		//File sender will figure that out
		HashSet<String> peerIPs = new HashSet<String>();
		
		//////////////////////////////////////////////////
		//			First, grab linked machines			//
		//////////////////////////////////////////////////
		try (	ResultSet linkedMachinesIp4 = DBManager.getInstance().select(
					"SELECT TOP "+minNumPeersToGrab+" "+
					"MachineIP4Links.IP4 AS IP4 "+
					"FROM Users "+
					"INNER JOIN Machines ON Users.MachinePK = Machines.MachinePK "+
					"INNER JOIN MachineIP4Links ON Machines.MachinePK = MachineIP4Links.MachinePK "+
					"WHERE Machines.MachineGUID NOT LIKE '"+Settings.getMachineGUIDSafe()+"'"
				);
				ResultSet linkedMachinesIp6 = DBManager.getInstance().select(
					"SELECT TOP "+minNumPeersToGrab+" "+
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
			
		} catch (SQLException | DBManagerFatalException e){
			throw new FileHandlerException("Unable to send file",e);
		}
		
		
		//////////////////////////////////////////////////
		//  Second, grab machines with this file on it	//
		//////////////////////////////////////////////////
		try (	ResultSet peersWithThisFileIP4 = DBManager.getInstance().select(
					"SELECT TOP "+minNumPeersToGrab+" "+
					"MachineIP4Links.IP4 AS IP4 "+
					"FROM UserFiles INNER JOIN UserFileLinks ON UserFiles.UserFilePK = UserFileLinks.UserFilePK "+
					"INNER JOIN MachineIP4Links ON UserFileLinks.MachinePK = MachineIP4Links.MachinePK "+
					"WHERE UserFiles.FileGUID = '"+fileGuid.toString()+"' "
					//May wish to add restriction for only this user
				);
				ResultSet peersWithThisFileIP6 = DBManager.getInstance().select(
					"SELECT TOP "+minNumPeersToGrab+" "+
					"MachineIP6Links.IP6 AS IP6 "+
					"FROM UserFiles INNER JOIN UserFileLinks ON UserFiles.UserFilePK = UserFileLinks.UserFilePK "+
					"INNER JOIN Users ON Users.UserPK = UserFileLinks.UserPK "+
					"INNER JOIN MachineIP6Links ON UserFileLinks.MachinePK = MachineIP6Links.MachinePK "+
					"WHERE UserFiles.FileGUID = '"+fileGuid.toString()+"' "
					//May wish to add restriction for only this user
				)
		) {
			while (peersWithThisFileIP6.next()) {
				peerIPs.add(peersWithThisFileIP6.getString("IP6"));
			}
			while (peersWithThisFileIP4.next()) {
				peerIPs.add(peersWithThisFileIP6.getString("IP4"));
			}
			
		} catch (SQLException | DBManagerFatalException e){
			throw new FileHandlerException("Unable to send file",e);
		}
		//////////////////////////////////////////////////
		//  Third, fill out with more peers if needed	//
		//////////////////////////////////////////////////
		if (minNumPeersToGrab < peerIPs.size()) {
			int numOfEachToGrab = peerIPs.size() - minNumPeersToGrab; //Guaranteed to be > 0
			
			try (	ResultSet peersIP6 = DBManager.getInstance().select(
					"SELECT TOP "+numOfEachToGrab+" "+
					"MachineIP6Links.IP6 AS IP6 "+
					"FROM MachineIP6Links "+
					((peerIPs.size() > 0) ? " WHERE IP6 NOT IN ("+JDFSUtil.stringJoin(peerIPs)+")" : "")
					//May wish to add restriction for only this user
				);
				ResultSet peersIP4 = DBManager.getInstance().select(
					"SELECT TOP "+numOfEachToGrab+" "+
					"MachineIP4Links.IP4 AS IP4 "+
					"FROM MachineIP4Links "+
					((peerIPs.size() > 0) ? " WHERE IP4 NOT IN ("+JDFSUtil.stringJoin(peerIPs)+")" : "")
				)
			) {
				while (peersIP4.next() && peerIPs.size() < minNumPeersToGrab) {
					peerIPs.add(peersIP4.getString("IP4"));
				}
				while (peersIP6.next() && peerIPs.size() < minNumPeersToGrab) {
					peerIPs.add(peersIP6.getString("IP6"));
				}
				
			} catch (SQLException | DBManagerFatalException e){
				throw new FileHandlerException("Unable to send file",e);
			}
			
		}
		
		return peerIPs;
	}
	
	public boolean organizeFile(Path tempFileLocation, FileSenderInfo info) throws DBManagerFatalException {
		
		
		DBManager dbm = DBManager.getInstance();
		
		try (ResultSet MachinePKs = dbm.select("SELECT DISTINCT MachinePK FROM Machines "+
				"WHERE Machines.MachineGUID LIKE '"+info.sendingMachineGuid+"'"
			);
			ResultSet matches = dbm.select(
				"SELECT DISTINCT Peers.PeerPK AS PeerPK, PeerFiles.FilePK AS FilePK, "+
						" PeerFiles.LocalFileName AS FileName, PeerFiles.LocalFilePath AS FilePath"+
						" PeerFiles.UpdatedDate AS UpdatedDate, PeerFiles.IV AS IV, PeerFiles.ParentGUID AS ParentGUID, "+
						" PeerFiles.ParentPath AS ParentPath, PeerFiles.Priority AS Priority, PeerFiles.CheckSum AS CheckSum " +
				"FROM Peers "+
				"INNER JOIN PeerFileLinks ON Peers.PeerPK = PeerFileLinks.PeerPK "+
				"INNER JOIN PeerFiles ON PeerFileLinks.PeerFilePK = PeerFiles.FilePK "+
				"WHERE PeerFiles.FileGUID LIKE '"+info.fileGuid+"' AND Peers.PeerGUID LIKE '"+info.userGuid+"'"
			);){
			
			boolean fileExistsInDB = false; 
			
			int MachinePK;
			
			if (MachinePKs.next()) {
				MachinePK = MachinePKs.getInt("MachinePK");
			} else { //should not be able to get here
				synchronized(DBManager.class) {
					dbm.upsert("INSERT INTO Machines(MachineGUID) VALUES ('"+info.sendingMachineGuid+"')");
					try (ResultSet priKey = dbm.select("SELECT TOP 1 MachinePK FROM Machines ORDER BY MachinePK DESC")
					) {
						priKey.next();  //Hard fail if this doesn't work
						MachinePK = priKey.getInt("MachinePK");
					}
				}
			}
			
			while (matches.next()) {
				
				//Note how HSQLDB starts at position 1 instead of zero, along with forcing specification of byte array len
				
				if (Arrays.areEqual(matches.getBlob("CheckSum").getBytes(1, FileUtil.NUM_CHECKSUM_BYTES), info.Checksum) &&
					Arrays.areEqual(matches.getBlob("ParentGUID").getBytes(1, JDFSSecurity.NUM_IV_BYTES), 
							info.AESInitializationVector //Note that we doubly-store it if it has a different IV
					)
				) {
					fileExistsInDB = true;
					
					if (matches.getString("ParentPath").equals( info.parentLocation.toString() ) && 
						matches.getString("ParentGUID").equals( info.parentGUID.toString() )
					) {
						String sqlUpdate = "";
						
						if (matches.getInt("Priority") != info.priority) { //update priority
							sqlUpdate += "Priority = "+info.priority;
						}
						
						if (!matches.getDate("UpdatedDate").equals(info.lastUpdatedDate)) {
							if (sqlUpdate.length() > 0)
								sqlUpdate +=", ";
							sqlUpdate += "UpdatedDate = '"+info.lastUpdatedDate+"'";
						}
						
						if (sqlUpdate.length() > 0) {
							dbm.upsert("UPDATE PeerFiles SET "+sqlUpdate+" WHERE "+
								"PeerFiles.FilePK = "+matches.getString("FilePK")
							);
						}
						
					} else { //store once, link twice
						synchronized(DBManager.class) {
							dbm.upsert("INSERT INTO PeerFiles (FileGUID, LocalFileName, LocalFilePath, UpdatedDate, IV, ParentGUID, ParentPath, Priority, CheckSum) "+
									"VALUES ('"+
										info.fileGuid+"','"+
										matches.getString("FileName")+"','"+
										matches.getString("FilePath")+"','"+
										info.lastUpdatedDate+"','"+
										ByteUtils.toHexString(info.AESInitializationVector)+"','"+
										info.parentGUID+"','"+
										info.parentLocation+"',"+
										info.priority+",'"+
										ByteUtils.toHexString(info.Checksum)+"'"+
									")"
							);
							
							
							try (ResultSet priKey = dbm.select("SELECT TOP 1 PeerFiles.FilePK AS FilePK FROM PeerFiles ORDER BY PeerFiles.FilePK DESC")
							){
								priKey.next(); //Hard fail if this doesn't work
								int PeerFilePK = priKey.getInt("FilePK");
								Printer.log("PeerFilePK:"+PeerFilePK);
								dbm.upsert("INSERT INTO PeerFileLinks(PeerFilePK, PeerPK, MachinePK) VALUES ("+PeerFilePK+","+matches.getInt("PeerPK")+","+MachinePK+")");
							}
						}
					}
				}				
			}
			
			if (!fileExistsInDB) { //Insert it if it isn't found in our db
				Path fileLoc = moveFileToCorrectPlace(tempFileLocation, info);
				
				
				int PeerPK;
				try (ResultSet peerPKs = dbm.select("SELECT TOP 1 PeerPK FROM Peers WHERE Peers.PeerGUID LIKE '"+info.userGuid+"'")){
					peerPKs.next();
					PeerPK = peerPKs.getInt("PeerPK");
				}
				
				
				synchronized(DBManager.class) {
					dbm.upsert("INSERT INTO PeerFiles (FileGUID, LocalFileName, LocalFilePath, UpdatedDate, IV, ParentGUID, ParentPath, Priority, CheckSum) "+
							"VALUES ('"+
								info.fileGuid+"','"+
								fileLoc.getFileName()+"','"+
								fileLoc+"','"+
								info.lastUpdatedDate+"','"+
								ByteUtils.toHexString(info.AESInitializationVector)+"','"+
								info.parentGUID+"','"+
								info.parentLocation+"',"+
								info.priority+",'"+
								ByteUtils.toHexString(info.Checksum)+"'"+
							")"
					);
					
					try (ResultSet priKey = dbm.select("SELECT TOP 1 PeerFiles.FilePK AS FilePK FROM PeerFiles ORDER BY PeerFiles.FilePK DESC")
					) {
						priKey.next(); //Hard fail if this doesn't work
						int PeerFilePK = priKey.getInt("FilePK");
						Printer.log("PeerFilePK:"+PeerFilePK);
						dbm.upsert("INSERT INTO PeerFileLinks(PeerFilePK, PeerPK, MachinePK) VALUES ("+PeerFilePK+","+PeerPK+","+MachinePK+")");
					}
				}
				
			} else {
				Files.delete(tempFileLocation);
			}
			
			return true;
		} catch(SQLException | IOException | SettingsReaderException e) {
			Printer.logErr(e);
			return false;
		}
	}
	
	private Path moveFileToCorrectPlace(Path tempFileLocation,
			FileSenderInfo info) throws SettingsReaderException, IOException {

		//LOOKUP PATH:
		//StorageDirectory -> User_GUID ->( <Parent_GUID> -> <Resolved_Against_parent>)->File_GUID->random_name.xz.enc
		
		Path location = SettingsReader.getInstance().getStorageDirectory();
		
		//TODO minor: see if we can combine statements.  
		if (!Files.exists(location))
			Files.createDirectory(location);
		
		location = location.resolve(info.userGuid.toString());
		
		if (!Files.exists(location))
			Files.createDirectory(location);
		
		if (null != info.parentGUID) {
			location = location.resolve(info.parentGUID.toString());
			if (!Files.exists(location))
				Files.createDirectory(location);
			
			if (null != info.parentLocation) {
				location = location.resolve(info.parentLocation);
				if (!Files.exists(location))
					Files.createDirectory(location);
			}
		}
		
		Path tempLoc = location.resolve(info.fileGuid+".xz.enc");

		for (int i = 0; Files.exists(tempLoc) && i >= 0; ++i) { //Stupid way to prevent infinite loop.  We've got some big problems if that ever occurs
			tempLoc = location.resolve(info.fileGuid+"."+i+".xz.enc");
		}
		
		Files.move(tempFileLocation, tempLoc);
		
		return tempLoc;
	}
}

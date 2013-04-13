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

import net.subject17.jdfs.JDFSUtil;
import net.subject17.jdfs.client.account.AccountManager;
import net.subject17.jdfs.client.file.FileUtil;
import net.subject17.jdfs.client.file.db.DBManager;
import net.subject17.jdfs.client.file.db.DBManager.DBManagerFatalException;
import net.subject17.jdfs.client.file.model.EncryptedFileInfoStruct;
import net.subject17.jdfs.client.file.model.FileSenderInfo;
import net.subject17.jdfs.client.settings.Settings;
import net.subject17.jdfs.client.settings.reader.SettingsReader;
import net.subject17.jdfs.client.settings.reader.SettingsReader.SettingsReaderException;
import net.subject17.jdfs.client.user.User.UserException;

public final class FileHandler {
	
	public final static int minNumPeersToGrab = 100;
	
	public final static class FileHandlerException extends Exception {
		private static final long serialVersionUID = -8693105516511333054L;
		public FileHandlerException(){super();}
		public FileHandlerException(String message){super(message);}
		public FileHandlerException(Exception e){super(e);}
		public FileHandlerException(String message, Throwable thrw){super(message, thrw);}
	}
	
	private static FileHandler _instance;
	
	private FileHandler(){}
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
	
	public boolean canStoreFile(Path p){
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
			
		} catch (SQLException | IOException | DBManagerFatalException e){
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
			
		} catch (SQLException | IOException | DBManagerFatalException e){
			throw new FileHandlerException("Unable to send file",e);
		}
		//////////////////////////////////////////////////
		//  Third, fill out with more peers if needed	//
		//////////////////////////////////////////////////
		if (minNumPeersToGrab < peerIPs.size()) {
			
			try (	ResultSet peersIP6 = DBManager.getInstance().select(
					"SELECT TOP "+minNumPeersToGrab+" "+
					"MachineIP6Links.IP6 AS IP6 "+
					"FROM MachineIP6Links "+
					((peerIPs.size() > 0) ? " WHERE IP6 NOT IN ("+JDFSUtil.stringJoin(peerIPs)+")" : "")
					//May wish to add restriction for only this user
				);
				ResultSet peersIP4 = DBManager.getInstance().select(
					"SELECT TOP "+minNumPeersToGrab+" "+
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
				
			} catch (SQLException | IOException | DBManagerFatalException e){
				throw new FileHandlerException("Unable to send file",e);
			}
			
		}
		
		return peerIPs;
	}
	
	public Path getStorageLocation(UUID userGUID, UUID fileGUID) throws SettingsReaderException {
		return SettingsReader.getInstance().getStorageDirectory();
	}
}

package net.subject17.jdfs.client.file.handler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashSet;
import java.util.UUID;

import javax.crypto.NoSuchPaddingException;

import org.bouncycastle.pqc.math.linearalgebra.ByteUtils;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;

import net.subject17.jdfs.JDFSUtil;
import net.subject17.jdfs.client.account.AccountManager;
import net.subject17.jdfs.client.file.FileUtil;
import net.subject17.jdfs.client.file.db.DBManager;
import net.subject17.jdfs.client.file.db.DBManager.DBManagerFatalException;
import net.subject17.jdfs.client.file.model.EncryptedFileInfoStruct;
import net.subject17.jdfs.client.file.model.FileRetrieverInfo;
import net.subject17.jdfs.client.file.model.FileRetrieverRequest;
import net.subject17.jdfs.client.file.model.FileSenderInfo;
import net.subject17.jdfs.client.io.Printer;
import net.subject17.jdfs.client.peers.PeersHandler;
import net.subject17.jdfs.client.settings.Settings;
import net.subject17.jdfs.client.settings.reader.SettingsReader;
import net.subject17.jdfs.client.settings.reader.SettingsReader.SettingsReaderException;
import net.subject17.jdfs.client.user.User.UserException;

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
				//"UserFiles.FileGUID AS FileGUID, UserFiles.ParentGUID AS ParentGUID, UserFiles.RelativeParentPath AS RelativeParentPath, UserFiles.LastUpdatedLocal AS UpdatedDate, UserFiles.Priority AS Priority, "+
				"UserFiles.*, "+
				"Users.UserGUID AS UserGUID, Users.UserName AS UserName, Users.AccountEmail AS AccountEmail "+
				"FROM UserFiles INNER JOIN UserFileLinks ON UserFiles.UserFilePK = UserFileLinks.UserFilePK "+
				"INNER JOIN Users ON Users.UserPK = UserFileLinks.UserPK "+
				"WHERE UserFiles.LocalFilePath LIKE '"+context.toString()+"' "+
				"AND COALESCE(UserFiles.IV,'') LIKE ''" //TODO exception case here.  We've received a file, but haven't decoded it, and old one is marked for sending
		)){
			
			EncryptedFileInfoStruct fileData = FileUtil.getInstance().compressAndEncryptFile(
					context,
					AccountManager.getInstance().getPasswordDigest()
			);
			UUID MachineGUID = Settings.getMachineGUIDSafe();
			byte[] CheckSum = FileUtil.getInstance().getMD5Checksum(context); //Note that the
			
			if (filesToSend.next()) {
				UUID userGUID = UUID.fromString(filesToSend.getString("UserGUID"));
				
				//String email = filesToSend.getString("AccountEmail");
				//String userName = filesToSend.getString("UserName");
				
				Date UpdatedDate = new Date(Files.getLastModifiedTime(context).toMillis());
				
				int priority = filesToSend.getInt("Priority");
				
				if (null == filesToSend.getString("FileGUID") || filesToSend.getString("FileGUID").equals("")) {
					UUID parentGUID = UUID.fromString(filesToSend.getString("ParentGUID"));
					Path relParentPath = Paths.get(filesToSend.getString("RelativeParentPath"));

					return new FileSenderInfo(fileData,context,userGUID,MachineGUID,UpdatedDate,priority,CheckSum,parentGUID,relParentPath);
				}
				else {
					UUID fileGUID = UUID.fromString(filesToSend.getString("FileGUID"));
					
					return new FileSenderInfo(fileData,context,fileGUID,userGUID,MachineGUID,UpdatedDate,priority,CheckSum);
				}
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
		peerIPs.addAll(PeersHandler.getLinkedMachineIPs());
		
		
		//////////////////////////////////////////////////
		//  Second, grab machines with this file on it	//
		//////////////////////////////////////////////////
		try (	ResultSet peersWithThisFileIP4 = DBManager.getInstance().select(
					"SELECT TOP "+minNumPeersToGrab+" "+
					"MachineIP4Links.IP4 AS IP4 "+
					"FROM UserFiles INNER JOIN UserFileLinks ON UserFiles.UserFilePK = UserFileLinks.UserFilePK "+
					"INNER JOIN MachineIP4Links ON UserFileLinks.MachinePK = MachineIP4Links.MachinePK "+
					"WHERE UserFiles.FileGUID LIKE '"+fileGuid.toString()+"' "
					//May wish to add restriction for only this user
				);
				ResultSet peersWithThisFileIP6 = DBManager.getInstance().select(
					"SELECT TOP "+minNumPeersToGrab+" "+
					"MachineIP6Links.IP6 AS IP6 "+
					"FROM UserFiles INNER JOIN UserFileLinks ON UserFiles.UserFilePK = UserFileLinks.UserFilePK "+
					"INNER JOIN Users ON Users.UserPK = UserFileLinks.UserPK "+
					"INNER JOIN MachineIP6Links ON UserFileLinks.MachinePK = MachineIP6Links.MachinePK "+
					"WHERE UserFiles.FileGUID LIKE '"+fileGuid.toString()+"' "
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
		Printer.log("-------------------------------------------Filling out peers");
		fillOutIPs(peerIPs);
		
		return peerIPs;
	}
	
	public HashSet<String> getPeersToSendFileTo(UUID parentGUID, Path relativePath) throws FileHandlerException {
		
		//We're not making a distinction between ip4 and ip6 addresses here.
		//File sender will figure that out
		HashSet<String> peerIPs = new HashSet<String>();
		
		//////////////////////////////////////////////////
		//			First, grab linked machines			//
		//////////////////////////////////////////////////
		peerIPs.addAll(PeersHandler.getLinkedMachineIPs());
		
		
		//////////////////////////////////////////////////
		//  Second, grab machines with this file on it	//
		//////////////////////////////////////////////////
		try (	ResultSet peersWithThisFileIP4 = DBManager.getInstance().select(
					"SELECT TOP "+minNumPeersToGrab+" "+
					"MachineIP4Links.IP4 AS IP4 "+
					"FROM UserFiles INNER JOIN UserFileLinks ON UserFiles.UserFilePK = UserFileLinks.UserFilePK "+
					"INNER JOIN MachineIP4Links ON UserFileLinks.MachinePK = MachineIP4Links.MachinePK "+
					"WHERE UserFiles.FileGUID LIKE '' "+
					"AND UserFiles.ParentGUID LIKE '"+parentGUID+"' "+
					"AND UserFiles.RelativeParentPath LIKE '"+relativePath+"' "
					//May wish to add restriction for only this user
				);
				ResultSet peersWithThisFileIP6 = DBManager.getInstance().select(
					"SELECT TOP "+minNumPeersToGrab+" "+
					"MachineIP6Links.IP6 AS IP6 "+
					"FROM UserFiles INNER JOIN UserFileLinks ON UserFiles.UserFilePK = UserFileLinks.UserFilePK "+
					"INNER JOIN Users ON Users.UserPK = UserFileLinks.UserPK "+
					"INNER JOIN MachineIP6Links ON UserFileLinks.MachinePK = MachineIP6Links.MachinePK "+
					"WHERE UserFiles.FileGUID LIKE '' "+
					"AND UserFiles.ParentGUID LIKE '"+parentGUID+"' "+
					"AND UserFiles.RelativeParentPath LIKE '"+relativePath+"' "
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
		Printer.log("-------------------------------------------Filling out peers");
		fillOutIPs(peerIPs);
		
		return peerIPs;
	}
	
	private static HashSet<String> fillOutIPs(HashSet<String> peerIPs) throws FileHandlerException {
		if (minNumPeersToGrab > peerIPs.size()) {
			int numOfEachToGrab = minNumPeersToGrab - peerIPs.size(); //Guaranteed to be > 0
			
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
		
		String whereClause = (null == info.fileGuid) ? 
				"PeerFiles.ParentGUID LIKE '"+info.parentGUID+ "'"+" AND PeerFiles.RelativeParentPath LIKE '"+info.locationRelativeToParent+"' "
				:"PeerFiles.FileGUID LIKE '"+info.fileGuid+"'";
		
		try (ResultSet MachinePKs = dbm.select("SELECT TOP 1 DISTINCT MachinePK FROM Machines "+
				"WHERE Machines.MachineGUID LIKE '"+info.sendingMachineGuid+"'"
			);
			ResultSet matches = dbm.select(
				"SELECT DISTINCT Peers.PeerPK AS PeerPK, PeerFiles.FilePK AS FilePK, "+
						" PeerFiles.LocalFileName AS FileName, PeerFiles.LocalFilePath AS FilePath"+
						" PeerFiles.UpdatedDate AS UpdatedDate, PeerFiles.IV AS IV, PeerFiles.ParentGUID AS ParentGUID, "+
						" PeerFiles.ParentPath AS ParentPath, PeerFiles.Priority AS Priority, PeerFiles.CheckSum AS CheckSum " +
				"FROM Peers "+
				"INNER JOIN PeerFileLinks ON Peers.PeerPK = PeerFileLinks.PeerPK "+
				"INNER JOIN PeerFiles ON PeerFileLinks.FilePK = PeerFiles.FilePK "+
				"WHERE Peers.PeerGUID LIKE '"+info.userGuid+"' AND "+whereClause
			)){
			
			boolean fileExistsInDB = false; 
			
			
			//////////////////////////////
			//		Get Machine PK		//
			//////////////////////////////
			int MachinePK;
			
			if (MachinePKs.next()) {
				MachinePK = MachinePKs.getInt("MachinePK");
			} else { //should not be able to get here, since it should be inserted upon initial client connection
				Printer.logErr("For some reason, machinePK for client wasn't already in db", Printer.Level.Low);
				
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
				
				if (matches.getString("CheckSum").equals(info.Checksum) &&
					matches.getString("IV").equals(
							info.AESInitializationVector //Note that we doubly-store it if it has a different IV
					)
				) {
					fileExistsInDB = true; //Really this is the only needed part
					
					Printer.log("File recieved already found in database, not stored");
					
					//Here from when storing directories was different than that of files
					/*
					if (matches.getString("RelativeParentPath").equals( info.locationRelativeToParent.toString() ) && 
						matches.getString("ParentGUID").equals( info.parentGUID.toString() )
					) {
						String sqlUpdate = "";
						
						if (matches.getInt("Priority") != info.priority) { //update priority
							sqlUpdate += "Priority = "+info.priority;
						}
						
						if (!matches.getDate("UpdatedDate").equals(info.Date)) {
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
					*/
					/*
						synchronized(DBManager.class) {
							try (ResultSet priKey = dbm.upsert("INSERT INTO PeerFiles (FileGUID, LocalFileName, LocalFilePath, UpdatedDate, IV, ParentGUID, ParentPath, Priority, CheckSum) "+
									"VALUES ('"+
										(null == info.fileGuid ? "" :info.fileGuid)+"','"+
										matches.getString("FileName")+"','"+
										matches.getString("FilePath")+"','"+
										info.lastUpdatedDate+"','"+
										info.AESInitializationVector+"','"+
										(null == info.parentGUID ? "" :info.parentGUID)+"','"+
										(null == info.locationRelativeToParent ? "" :info.locationRelativeToParent)+"',"+
										info.priority+",'"+
										info.Checksum+"'"+
									")"
								)
							) {
								priKey.next(); //Hard fail if this doesn't work
								int PeerFilePK = priKey.getInt("FilePK");
								Printer.log("PeerFilePK:"+PeerFilePK);
								dbm.upsert("INSERT INTO PeerFileLinks(PeerFilePK, PeerPK, MachinePK) VALUES ("+PeerFilePK+","+matches.getInt("PeerPK")+","+MachinePK+")");
							}
						}
						*/ //No clue why I was doubly inserting it there.
							//Only LastUpdatedDate and priority could be different provided DB integrity is sane,
							//And we wouldn't want to change the LastUpdatedDate anyway (or, at most, take the min of the two)
					//}
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
					dbm.upsert("INSERT INTO PeerFiles (FileGUID, LocalFileName, LocalFilePath, PathOnClient, UpdatedDate, IV, ParentGUID, ParentPath, Priority, CheckSum) "+
							"VALUES ('"+
								(null == info.fileGuid ? "" :info.fileGuid)+"','"+
								fileLoc.getFileName()+"','"+
								fileLoc+"','"+
								info.fileLocation+"','"+
								info.lastUpdatedDate+"','"+
								info.AESInitializationVector+"','"+
								(null == info.parentGUID ? "" :info.parentGUID)+"','"+
								(null == info.locationRelativeToParent ? "" :info.locationRelativeToParent)+"',"+
								info.priority+",'"+
								info.Checksum+"'"+
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
		
		Path tempLoc, location = SettingsReader.getInstance().getStorageDirectory();
		
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
			
			//Assuming locationRelativeToParent can't be null since it shouldn't be

			tempLoc = location.resolve(info.locationRelativeToParent+".xz.enc");
			for (int i = 0; Files.exists(tempLoc) && i >= 0; ++i) { //Stupid way to prevent infinite loop.  We've got some big problems if that ever occurs
				tempLoc = location.resolve(info.locationRelativeToParent+"."+i+".xz.enc");
			}
		}
		else {
			tempLoc = location.resolve(info.fileGuid+".xz.enc");

			for (int i = 0; Files.exists(tempLoc) && i >= 0; ++i) { //Stupid way to prevent infinite loop.  We've got some big problems if that ever occurs
				tempLoc = location.resolve(info.fileGuid+"."+i+".xz.enc");
			}
		}
		
		Files.move(tempFileLocation, tempLoc);
		
		return tempLoc;
	}

	public FileRetrieverInfo getFileStoredOnMachine(FileRetrieverRequest criteria) throws DBManagerFatalException {
		try {
			Printer.log("Criteria :"+JDFSUtil.toJSON(criteria));
		} catch (IOException e) {
			Printer.logErr(e);
		}
		String criteriaRequestString = "SELECT DISTINCT PeerFiles.* FROM PeerFiles "+
				"INNER JOIN PeerFileLinks ON PeerFiles.FilePK = PeerFileLinks.PeerFilePK "+
				"INNER JOIN Peers ON Peers.PeerPK = PeerFileLinks.PeerPK "+
				"LEFT JOIN Machines ON Machines.MachinePK = PeerFileLinks.MachinePK "+
				"WHERE Peers.PeerGUID LIKE '"+criteria.userGuid+"'";
		
		//File vs directory restrictions
		if (null != criteria.parentGUID) {
			criteriaRequestString += " AND PeerFiles.ParentGUID LIKE '"+criteria.parentGUID+"'";
			//This should not be null in the current iteration of the program.  Once we can get entire directories, that will change
			if (null != criteria.relativeParentLoc)
				criteriaRequestString += " AND PeerFiles.RelativeParentPath LIKE '"+criteria.relativeParentLoc+"'";
			else
				Printer.logErr("Functionality required for future:  Retrieve entire directory", Printer.Level.High);
		}
		else {
			criteriaRequestString += " AND PeerFiles.FileGUID LIKE '"+criteria.fileGuid+"'";
		}
		
		//Machine restriction if present
		if (null != criteria.sendingMachineGuid) {
			criteriaRequestString += " AND Machines.MachineGUID LIKE '"+criteria.sendingMachineGuid+"'";
		}
		
		//Updated date restriction if present
		if (null != criteria.lastUpdatedDate) {
			criteriaRequestString += " AND PeerFiles.UpdatedDate "+criteria.comparison+" '"+criteria.lastUpdatedDate+"'";
		}
		
		criteriaRequestString += " ORDER BY PeerFiles.UpdatedDate DESC";
		
		try (ResultSet filesFound = DBManager.getInstance().select(criteriaRequestString)){
			if (filesFound.next()) {
				return new FileRetrieverInfo(filesFound);
			}
			else {
				Printer.log("none found");
			}
		}
		catch (SQLException e) {
			Printer.logErr("SQLException encountered in FileHandler [getFileStoredOnMachine], refusing to send");
			Printer.logErr(e);
		}
		catch (IOException e) {
			Printer.logErr("IOException encountered in FileHandler [getFileStoredOnMachine], refusing to send");
			Printer.logErr(e);
		}
		Printer.log("Not on machine");
		return null;
	}

	public void manageRecievedFile(Path tempStoreLocation, FileRetrieverInfo incomingInfo) {
		if (null == incomingInfo) {
			Printer.logErr("For some reason, passed incoming info was null", Printer.Level.High);
		}
		else {
			
			//Init values to default
			Path targetPath = incomingInfo.defaultLocation;
			String sqlRestriction = "", sqlFurtherRestriction = "";
			
			try {
				//TODO assertEquals here
				Printer.log("Calculated Checksum: "+ByteUtils.toHexString(FileUtil.getInstance().getMD5Checksum(tempStoreLocation)));
				Printer.log("Provided checksum: "+incomingInfo.Checksum);

				
				if (null != incomingInfo.fileGuid) {
					sqlRestriction = "UserFiles.FileGUID LIKE '"+incomingInfo.fileGuid+"'";
				}
				else if (null != incomingInfo.parentGUID) {
					sqlRestriction = "UserFiles.ParentGUID LIKE '"+incomingInfo.fileGuid+"'";
					
					if (null != incomingInfo.parentLocation) {
						sqlFurtherRestriction = " AND UserFiles.RelativeParentPath LIKE '"+incomingInfo.parentLocation+"'";
					}
				}
				
				if (!sqlRestriction.equals("")) {
					try (ResultSet fileLocationResults = DBManager.getInstance().select(
							"SELECT TOP 1 UserFiles.* "+
							"FROM UserFiles " +
							"WHERE "+sqlRestriction+sqlFurtherRestriction
						)
					) {
						if (fileLocationResults.next()) {
							targetPath = Paths.get(fileLocationResults.getString("LocalFilePath"));
						}
						else {
							try (ResultSet lessRestrictResults = DBManager.getInstance().select(
									"SELECT TOP 1 UserFiles.* "+
									"FROM UserFiles " +
									"WHERE "+sqlRestriction
								)
							) {
								if (lessRestrictResults.next()) {
									targetPath = Paths.get(fileLocationResults.getString("LocalFilePath"));
								}
							}
						}
					} catch (SQLException | DBManagerFatalException e) {
						Printer.logErr("SQL/DB Exception encountered in FileHandler whilst storing recieved file.");
						Printer.logErr("Proceeding with default value of ["+incomingInfo.defaultLocation+"]");
						Printer.logErr(e);
					}
				}
				
				FileUtil.getInstance().decryptAndExtractFile(
							tempStoreLocation,
							targetPath,
							AccountManager.getInstance().getPasswordDigest(),
							ByteUtils.fromHexString(incomingInfo.AESInitializationVector)
				);
				
				Printer.log("Success retrieving file!");
			}
			catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException e) {
				Printer.logErr("An error occured during the decryption of the recovered file.");
				Printer.logErr("The encrypted, compressed version is stored at "+tempStoreLocation+", and the HEX encoded IV is "+incomingInfo.AESInitializationVector);
				Printer.logErr(e);
			}
			catch (IOException e) {
				Printer.logErr("An IOException occured during the decryption of the recovered file.");
				Printer.logErr("Attempted to decrypt & extract file to the location ["+targetPath+"]");
				Printer.logErr("The encrypted, compressed version is stored at "+tempStoreLocation+", and the HEX encoded IV is "+incomingInfo.AESInitializationVector);
				Printer.logErr(e);
			}
			catch (UserException e) {
				Printer.logErr("An error occured getting the active user password during the decryption of the recovered file.");
				Printer.logErr("The encrypted, compressed version of tis stored at "+tempStoreLocation+", and the HEX encoded IV is "+incomingInfo.AESInitializationVector);
				Printer.logErr(e);
			}
		}
		Printer.log("Done with file retrieval");
	}
}

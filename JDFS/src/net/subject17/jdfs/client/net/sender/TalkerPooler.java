package net.subject17.jdfs.client.net.sender;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.UUID;

import javax.crypto.NoSuchPaddingException;

import net.subject17.jdfs.client.account.AccountManager;
import net.subject17.jdfs.client.file.FileUtil;
import net.subject17.jdfs.client.file.db.DBManager;
import net.subject17.jdfs.client.file.db.DBManager.DBManagerFatalException;
import net.subject17.jdfs.client.file.handler.FileHandler;
import net.subject17.jdfs.client.file.handler.FileHandler.FileHandlerException;
import net.subject17.jdfs.client.file.model.FileRetrieverRequest;
import net.subject17.jdfs.client.file.model.FileSenderInfo;
import net.subject17.jdfs.client.io.Printer;
import net.subject17.jdfs.client.peers.PeersHandler;
import net.subject17.jdfs.client.user.User;
import net.subject17.jdfs.client.user.User.UserException;

public final class TalkerPooler {
	
	private static TalkerPooler _instance = null;
	
	private TalkerPooler(){}
	
	public static TalkerPooler getInstance() {
		if (null == _instance){
			synchronized(TalkerPooler.class){
				if (null == _instance){
					_instance = new TalkerPooler(); //Can't do easy instantiation since I wanna be able to throw that exception
				}
			}
		}
		return _instance;
	}
	
	public final void UpdatePath(Path context, User user) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IOException, UserException {
		//TODO Design choice:  Only update this user, or all users that modify that file?  
		//TODO implement the directory handling as well
		Printer.log("TALKER POOLER Sending file "+context);
		try {
			Thread[] talkers;
			
			FileSenderInfo info = FileHandler.getInstance().prepareToSendFile(context);
			
			HashSet<String> peerIPs = (null == info.fileGuid) ? 
					FileHandler.getInstance().getPeersToSendFileTo(info.parentGUID, info.locationRelativeToParent)
					: FileHandler.getInstance().getPeersToSendFileTo(info.fileGuid);
			
			if (peerIPs.size() < 1) 
				peerIPs.addAll(PeersHandler.searchForIPs());
			
			if (peerIPs.size() > 0) {
				talkers = new Thread[peerIPs.size()];
				int i = 0;
				for (String ip : peerIPs) {
					FileSender temp = new FileSender(info.encryptedFileLocation, info.getAsJSON());
					talkers[i] = new Thread(new Talker(ip, temp));
					talkers[i].run();
				}
				
			} else {
				Printer.log("File not sent, no peers found");
			}
			
		} catch (FileHandlerException e) {
			Printer.logErr("Could not send file ["+context+"] !");
			Printer.logErr(e);
		}
	}

	public final static void checkForUpdates() throws DBManagerFatalException {
		
		//First, handle files that exist on our system linked to a user
		try (ResultSet filesToCheck = DBManager.getInstance().select("SELECT DISTINCT UserFiles.* FROM UserFiles INNER JOIN UserFileLinks ON UserFiles.UserFilePK = UserFileLinks.UserFilePK")
		) {
			HashSet<String> peerIPs = null;
			Path fileToSend = null;
			
			while (filesToCheck.next()) {
				
				Printer.log(filesToCheck);
				
				UUID fileGUID = null;
				
				//First, grab IPS and figure out if we're dealing with a regular file
				//or one in a watched directory
				if (!filesToCheck.getString("FileGUID").equals("")) { //Normal file
					
					fileGUID = UUID.fromString(filesToCheck.getString("FileGUID"));
					
					peerIPs = FileHandler.getInstance().getPeersToSendFileTo(
							UUID.fromString( filesToCheck.getString("FileGUID") )
					);
				}
				else if (!filesToCheck.getString("ParentGUID").equals("")) { //File in directory				
					peerIPs = FileHandler.getInstance().getPeersToSendFileTo(
							UUID.fromString( filesToCheck.getString("ParentGUID") ),
							Paths.get( filesToCheck.getString("RelativeParentPath") )
					);
				}
				else {
					fileGUID = addGuidToFile(filesToCheck);
				}
				
				//////////
				//Step 2: Set up the grab criteria if this is a file,
				//////////
				fileToSend = Paths.get( filesToCheck.getString("LocalFilePath") );
				
				
				if (!Files.exists(fileToSend) ) {
					Printer.log("File ["+fileToSend+"] does not exist -- skipped");
				}
				else if ( Files.isDirectory(fileToSend)) {
					Printer.log("Found directory in updates -- skipped");		
				}
				else {
					Printer.log("Seeing if updates exist for file "+fileToSend);
					
					Timestamp lastModified = new Timestamp(Files.getLastModifiedTime(fileToSend).toMillis());
					FileRetrieverRequest request;
					
					if (null != fileGUID) {
						request = new FileRetrieverRequest(
								fileGUID,
								AccountManager.getInstance().getActiveUser().getGUID(),
								lastModified,
								">"
						);
					}
					else {
						request = new FileRetrieverRequest(
								AccountManager.getInstance().getActiveUser().getGUID(),
								lastModified, ">",
								UUID.fromString(filesToCheck.getString("ParentGUID")),
								Paths.get( filesToCheck.getString("RelativeParentPath") )
						);
					}
					Printer.log("------------------------------"+request.toJSON());
					
					//Finally, request the file from the peers we found
					//We're just grabbing everything now, which is horribly inefficient.
					//In the future, getting the newest file would probably be right
					if (peerIPs.size() > 0) {

						Thread[] talkers = new Thread[peerIPs.size()];
						int i = 0;
						for (String ip : peerIPs) {
							FileRetriever temp = new FileRetriever(fileToSend, request);
							talkers[i] = new Thread(new Talker(ip, temp));
							talkers[i].run();
						}
						Printer.log("File sent!");
					}
					else {
						Printer.log("File not sent, no peers found");
					}
					
				}//End if (Files.isDirectory(fileToSend))
			} //End while (each file)
			
			
			
			
			//Now, just see if anything else out there exists
			//Next, anything for this user
			
			FileRetrieverRequest request = new FileRetrieverRequest(AccountManager.getInstance().getActiveUser().getGUID());
			peerIPs = PeersHandler.getSomeStoredIps(100);
			
			if (peerIPs.size() > 0) {

				Thread[] talkers = new Thread[peerIPs.size()];
				
				int i = 0;

				Path tempDir = FileUtil.tempDir;
				
				for (String ip : peerIPs) {
					
					Path tempTempTempTemptempDirTempFileTempDirStoreHeretempTempTempestTemp = tempDir.resolve(request.userGuid+".enc.xz");
					for (int j = 0; Files.exists(tempTempTempTemptempDirTempFileTempDirStoreHeretempTempTempestTemp) && j >=0; ++j) {
						tempTempTempTemptempDirTempFileTempDirStoreHeretempTempTempestTemp =
							tempDir.resolve(request.userGuid+"."+j+".enc.xz");
					}
					
					FileRetriever temp = new FileRetriever(
							tempTempTempTemptempDirTempFileTempDirStoreHeretempTempTempestTemp,
							request
					);
					talkers[i] = new Thread(new Talker(ip, temp));
					talkers[i].run();
					
					++i;
				}
				
			}
			else {
				Printer.log("No peers found, no user updates preformed");
			}
			
		} //End try
		catch (IOException | FileHandlerException | SQLException e) {
			Printer.logErr(e);
		}
		
	}
	
	private static UUID addGuidToFile(ResultSet filesToCheck) throws SQLException, DBManagerFatalException {
		UUID retVal = UUID.randomUUID();
		try (ResultSet update = DBManager.getInstance().upsert(
				"UPDATE UserFiles SET FileGUID = '"+retVal+
				"' WHERE UserFiles.UserFilePK = "+filesToCheck.getString("UserFilePK"))
		) {
			return retVal;
		}
	}
}

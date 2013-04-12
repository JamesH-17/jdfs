package net.subject17.jdfs.client.net.sender;

import java.io.IOException;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.UUID;

import javax.crypto.NoSuchPaddingException;

import net.subject17.jdfs.client.account.AccountManager;
import net.subject17.jdfs.client.file.FileUtil;
import net.subject17.jdfs.client.file.db.DBManager;
import net.subject17.jdfs.client.file.db.DBManager.DBManagerFatalException;
import net.subject17.jdfs.client.file.model.EncryptedFileInfoStruct;
import net.subject17.jdfs.client.file.model.FileSenderInfo;
import net.subject17.jdfs.client.io.Printer;
import net.subject17.jdfs.client.settings.Settings;
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
	
	
	//
	public String[] grabIP4s(){
		return new String[]{""};
	}
	
	
	
	private String jsonifyFileData(String pathGUID){
		//Connect to db, 
		return null;
	}



	public final void UpdatePath(Path context) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IOException, UserException {
		//TODO Design choice:  Only update this user, or all users that modify that file?  
		
		
		//TODO Use DB to find every peer that has this file
		//Then use file sender to send it to them
		
		EncryptedFileInfoStruct fileData = FileUtil.getInstance().compressAndEncryptFile(context, AccountManager.getInstance().getPasswordDigest());
		
		
		try (ResultSet filesToSend = DBManager.getInstance().select(
				"SELECT TOP 1 "+
				"UserFiles.FileGUID AS FileGUID, UserFiles.UpdatedDate AS UpdatedDate, UserFiles.Priority AS Priority"+
				"Users.UserGUID AS UserGUID, Users.UserName AS UserName, Users.AccountEmail AS AccountEmail"+
				"FROM UserFiles INNER JOIN UserFileLinks ON UserFiles.UserFilePK = UserFileLinks.UserFilePK "+
				"INNER JOIN Users ON Users.UserPK = UserFileLinks.UserPK "+
				"WHERE UserFiles.LocalFilePath = '"+context.toString()+"' "+
				"AND COALESCE(UserFiles.IV,'') LIKE ''" //TODO exception case here.  We've received a file, but haven't decoded it, and old one is marked for sending
		)){
			
			UUID MachineGUID = Settings.getMachineGUIDSafe();
			byte[] CheckSum = FileUtil.getInstance().getMD5Checksum(context);
			
			while(filesToSend.next()) {
				UUID fileGUID = UUID.fromString(filesToSend.getString("FileGUID"));
				UUID userGUID = UUID.fromString(filesToSend.getString("UserGUID"));
				
				String email = filesToSend.getString("AccountEmail");
				String userName = filesToSend.getString("UserName");
				
				Date UpdatedDate = filesToSend.getDate("UpdatedDate");
				//byte[] IV = filesToSend.getBlob("IV").getBytes(1, (int) filesToSend.getBlob("IV").length());
				int priority = filesToSend.getInt("Priority");
				FileSenderInfo info = new FileSenderInfo(fileData,context,fileGUID,userGUID,);
			}
			
			
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			Printer.logErr(e);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			Printer.logErr(e);
		} catch (DBManagerFatalException e) {
			// TODO Auto-generated catch block
			Printer.logErr(e);
		}
		
		
		Printer.log("Path:");
		Printer.log("Got here");
	}
}

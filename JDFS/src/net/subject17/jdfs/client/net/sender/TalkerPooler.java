package net.subject17.jdfs.client.net.sender;

import java.io.IOException;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;

import javax.crypto.NoSuchPaddingException;

import net.subject17.jdfs.client.file.handler.FileHandler;
import net.subject17.jdfs.client.file.handler.FileHandler.FileHandlerException;
import net.subject17.jdfs.client.file.model.FileSenderInfo;
import net.subject17.jdfs.client.io.Printer;
import net.subject17.jdfs.client.net.PortMgr;
import net.subject17.jdfs.client.peers.PeersHandler;
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
	
	public final void UpdatePath(Path context) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IOException, UserException {
		//TODO Design choice:  Only update this user, or all users that modify that file?  
		//TODO implement the directory handling as well
		try {
			Thread[] talkers;
			
			FileSenderInfo info = FileHandler.getInstance().prepareToSendFile(context);
			
			HashSet<String> peerIPs = FileHandler.getInstance().getPeersToSendFileTo(info.fileGuid);
			
			if (peerIPs.size() < 1) 
				peerIPs.addAll(PeersHandler.searchForIPs());
			
			if (peerIPs.size() > 0) {
				talkers = new Thread[peerIPs.size()];
				int i = 0;
				for (String ip : peerIPs) {
					FileSender temp = new FileSender(info.encryptedFileLocation, info.getAsJSON());
					talkers[i] = new Thread(new Talker(ip, PortMgr.getServerPort(), temp));
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
}

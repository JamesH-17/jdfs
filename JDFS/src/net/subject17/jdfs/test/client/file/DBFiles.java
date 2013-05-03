package net.subject17.jdfs.test.client.file;

import static org.junit.Assert.*;

import net.subject17.jdfs.client.file.db.DBManager;
import net.subject17.jdfs.client.io.Printer;

import org.junit.Test;

public class DBFiles {

	@Test
	public void showUserFiles() {
		try {
			DBManager.getInstance().writeTableToLog("UserFiles");
		}
		catch (Exception e){
			Printer.logErr(e);
			fail("Exception encountered");
		}
	}

	@Test
	public void showUserFileLinks() {
		try {
			DBManager.getInstance().writeTableToLog("UserFileLinks");
		}
		catch (Exception e){
			Printer.logErr(e);
			fail("Exception encountered");
		}
	}

	@Test
	public void showPeerFiles() {
		try {
			DBManager.getInstance().writeTableToLog("PeerFiles");
		}
		catch (Exception e){
			Printer.logErr(e);
			fail("Exception encountered");
		}
	}
	
	@Test
	public void showPeerFilesLinks() {
		try {
			DBManager.getInstance().writeTableToLog("PeerFileLinks");
		}
		catch (Exception e){
			Printer.logErr(e);
			fail("Exception encountered");
		}
	}
	
	
	//TODO move following to another test class
	@Test
	public void showUsers() {
		try {
			DBManager.getInstance().writeTableToLog("Users");
		}
		catch (Exception e){
			Printer.logErr(e);
			fail("Exception encountered");
		}
	}
	
	@Test
	public void showPeers() {
		try {
			DBManager.getInstance().writeTableToLog("Peers");
		}
		catch (Exception e){
			Printer.logErr(e);
			fail("Exception encountered");
		}
	}
	
	@Test
	public void showMachines() {
		try {
			DBManager.getInstance().writeTableToLog("Machines");
		}
		catch (Exception e){
			Printer.logErr(e);
			fail("Exception encountered");
		}
	}
	
	@Test
	public void showUserMachines() {
		try {
			DBManager.getInstance().writeTableToLog("MachineUserLinks");
		}
		catch (Exception e){
			Printer.logErr(e);
			fail("Exception encountered");
		}
	}
	
	@Test
	public void showPeerMachines() {
		try {
			DBManager.getInstance().writeTableToLog("MachinePeerLinks");
		}
		catch (Exception e){
			Printer.logErr(e);
			fail("Exception encountered");
		}
	}
	
	@Test
	public void showIP4s() {
		try {
			DBManager.getInstance().writeTableToLog("MachineIP4Links");
		}
		catch (Exception e){
			Printer.logErr(e);
			fail("Exception encountered");
		}
	}
	
	@Test
	public void showIP6s() {
		try {
			DBManager.getInstance().writeTableToLog("MachineIP6Links");
		}
		catch (Exception e){
			Printer.logErr(e);
			fail("Exception encountered");
		}
	}
}

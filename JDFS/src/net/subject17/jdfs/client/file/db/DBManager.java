package net.subject17.jdfs.client.file.db;

import java.io.IOException;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import net.subject17.jdfs.client.io.Printer;

public class DBManager {
	public final class DBManagerFatalException extends Exception {
		private static final long serialVersionUID = -749681840594206045L;
		public DBManagerFatalException()							{super();}
		public DBManagerFatalException(String msg)					{super(msg);}
		public DBManagerFatalException(String msg, Throwable thrw)	{super(msg,thrw);}
		public DBManagerFatalException(Exception e)	{super(e);}
	}
	
	
	private final static String dbUser = "SA";
	private final static String dbPassword = "";
	
	private static DBManager _instance = null;
	
	protected DBManager() throws DBManagerFatalException {
		try {
			initializeDatabase();
			
		} catch(SQLException e){
			e.printStackTrace();
			Printer.log("An error occured initializing the database");
			Printer.log(e);
			throw new DBManagerFatalException(e);
		}
	}
	
	public static DBManager getInstance() throws IOException, DBManagerFatalException {
		if (null == _instance){
			synchronized(DBManager.class){
				if (null == _instance){
					_instance = new DBManager(); //Initialization can throw exception, so can't do normal singleton instantiation method
				}
			}
		}
		return _instance;
	};
	
	private String getDBLocation(){
		return Paths.get(System.getProperty("user.dir"),"DB","jdfs.hsqldb").toString();
	}
	
	private Connection getConnection() throws SQLException {
		/* Apparently this is unneeded for Java 7
		  try {
		      Class.forName("org.hsqldb.jdbc.JDBCDriver" );
		  } catch (Exception e) {
		      System.err.println("ERROR: failed to load HSQLDB JDBC driver.");
		      e.printStackTrace();
		  }
		  */
		  return DriverManager.getConnection("jdbc:hsqldb:file:"+getDBLocation(), dbUser, dbPassword);
	}
	
	private void initializeDatabase() throws SQLException {
		/*				Design notes:
		 * We could load in these statements from a file, but considering their short length, I feel that's more
		 * effort than it would be worth.  Also, I don't want to risk that file being deleted.
		 * 
		 * Also, in a future release, these could be combined into a single execution, but I find seperating them
		 * is easier to debug.
		 * 
		 * Users Table:  UserID is not the PK since it's randomly generated for each user.
		 * 	While I'm hoping account emails will be naturally unique, I feel it's possible
		 * 	to find users with the same username+account combo in the wild
		 * 	Remember, people are lazy, and there could be a lot of {UserName:'john doe', UserEmail:'fake@mailinator.net'}
		 * 	out there
		 * 
		 * Peers Table:  Note the duplication of data from users table.  Really, the only difference between a "peer"
		 * 	and a "user" in the context of this program is whether or not that object has an account on the machine
		 * 	running this program.  If so, then they're a user.  If not, they're a peer.  Otherwise, they have the same
		 * 	data
		 * 	
		 * Files Tables: We always receive a file if it doesn't have the same checksum as the one currently stored.
		 * 	However, for 
		 * 
		 * Ip6's and Ip4's
		 */
		
		
		try(Connection conn = getConnection();
			Statement statement = conn.createStatement()
		){
			
			//Create users table if it doesn't exist
			statement.executeQuery("CREATE TABLE IF NOT EXISTS Users ("+
							"UserPK INTEGER Generated By DEFAULT As IDENTITY PRIMARY KEY, "+
							"UserGUID VARCHAR(36) NOT NULL UNIQUE, "+
							"UserName VARCHAR(36) NOT NULL, "+
							"AccountEmail VARCHAR(36) NOT NULL"+
			")");
			
			//Create peers table if it doesn't exist
			statement.executeQuery("CREATE TABLE IF NOT EXISTS Peers ("+
							"PeerPeerPK INTEGER Generated By DEFAULT As IDENTITY PRIMARY KEY, "+
							"UserGUID VARCHAR(36) NOT NULL UNIQUE,"+
							"UserName VARCHAR(36) NOT NULL UNIQUE,"+
							"AccountEmail VARCHAR(36) NOT NULL UNIQUE,"+
							"MachineGUID VARCHAR(36) NOT NULL UNIQUE"+
			")");
			
			//Create user files table if it doesn't exist
			statement.executeQuery("CREATE TABLE IF NOT EXISTS UserFiles ("+
							"UserFilePK INTEGER Generated By DEFAULT As IDENTITY PRIMARY KEY, "+
							"FileGUID VARCHAR(36) NOT NULL, " +
							"LocalFileName text NOT NULL, " + //NOTE THE DATATYPE!  MAJOR PREFORMANCE ISSUE POSSIBLE!
							"LocalFilePath text NOT NULL, " + //including name
							"LastUpdatedLocal DATETIME, " +
							"CheckSum VARCHAR(36), " +
							"IV BLOB, " +
							"Priority INTEGER DEFAULT 0 NOT NULL, " + //Default priority = 0, lower priorities < 0 and higher priorities >0
							"WatchedDirectoryPK Boolean DEFAULT FALSE NOT NULL"+
			")");
			
			//Create peer files table if it doesn't exist
			statement.executeQuery("CREATE TABLE IF NOT EXISTS PeerFiles ("+
							"FilePK INTEGER Generated By DEFAULT As IDENTITY PRIMARY KEY, "+
							"FileGUID VARCHAR(36) NOT NULL, " +
							"LocalFileName text NOT NULL, " + //NOTE THE DATATYPE!  MAJOR PREFORMANCE ISSUE POSSIBLE!
							"LocalFilePath text NOT NULL, " + //including name
							"UpdatedDate DATETIME, " +
							"IV BLOB, " +
							"Priority INTEGER DEFAULT 0 NOT NULL, " + //Default priority = 0, lower priorities < 0 and higher priorities >0
							"CheckSum VARCHAR(36) " +
			")");
			
			//Create machines table if it doesn't exist
			statement.executeQuery("CREATE TABLE IF NOT EXISTS Machines ("+
							"MachinePK INTEGER Generated By DEFAULT As IDENTITY PRIMARY KEY, "+
							"MachineGUID VARCHAR(36) NOT NULL UNIQUE"+
			")");
			
			///////////////////////////
			// Create Linking Tables //
			///////////////////////////
			
			//Create User Files Link table if it doesn't exist
			statement.executeQuery("CREATE TABLE IF NOT EXISTS UserFileLinks ("+
							"LinkID INTEGER Generated By DEFAULT As IDENTITY PRIMARY KEY, "+
							"UserFilePK INTEGER NOT NULL, "+
							"UserPK INTEGER NOT NULL, "+
							"MachinePK INTEGER NOT NULL"+
			")");
			
			//Create Peer Files Link table if it doesn't exist
			statement.executeQuery("CREATE TABLE IF NOT EXISTS PeerFileLinks ("+
							"LinkID INTEGER Generated By DEFAULT As IDENTITY PRIMARY KEY, "+
							"PeerFilePK INTEGER NOT NULL,"+
							"PeerPK INTEGER NOT NULL,"+
							"MachinePK INTEGER NOT NULL"+
			")");
			
			//Create Machine Users Link table if it doesn't exist
			statement.executeQuery("CREATE TABLE IF NOT EXISTS MachineUserLinks ("+
							"LinkID INTEGER Generated By DEFAULT As IDENTITY PRIMARY KEY, "+
							"MachinePK INTEGER NOT NULL, "+
							"UserPK INTEGER NOT NULL"+
			")");
			
			//Create Machine Peers Link table if it doesn't exist
			statement.executeQuery("CREATE TABLE IF NOT EXISTS MachinePeerLinks ("+
							"LinkID INTEGER Generated By DEFAULT As IDENTITY PRIMARY KEY, "+
							"MachinePK INTEGER NOT NULL, "+
							"PeerPK INTEGER NOT NULL"+
			")");
			
			//Create Machine IP4 Link table if it doesn't exist
			statement.executeQuery("CREATE TABLE IF NOT EXISTS MachineIP4Links ("+
							"LinkID INTEGER Generated By DEFAULT As IDENTITY PRIMARY KEY, "+
							"MachinePK INTEGER NOT NULL, "+
							"IP4 VARCHAR(16) NOT NULL"+
			")");
			
			//Create Machine IP6 Link table if it doesn't exist
			statement.executeQuery("CREATE TABLE IF NOT EXISTS MachineIP6Links ("+
							"LinkID INTEGER Generated By DEFAULT As IDENTITY PRIMARY KEY, "+
							"MachinePK INTEGER NOT NULL, "+
							"IP6 VARCHAR(46) NOT NULL"+
			")");
			
			//auto closes statement and connection
		}
	}
	
	//For testing
	private boolean dropEverything() throws SQLException {
		try(Connection conn = getConnection();
			Statement statement = conn.createStatement()
		){
			statement.execute("TRUNCATE TABLE Users AND COMMIT NO CHECK");
			statement.execute("TRUNCATE TABLE Peers AND COMMIT NO CHECK");
			statement.execute("TRUNCATE TABLE UserFiles AND COMMIT NO CHECK");
			statement.execute("TRUNCATE TABLE PeerFiles AND COMMIT NO CHECK");
			statement.execute("TRUNCATE TABLE Machines AND COMMIT NO CHECK");
			statement.execute("TRUNCATE TABLE UserFileLinks AND COMMIT NO CHECK");
			statement.execute("TRUNCATE TABLE PeerFileLinks AND COMMIT NO CHECK");
			statement.execute("TRUNCATE TABLE MachineUserLinks AND COMMIT NO CHECK");
			statement.execute("TRUNCATE TABLE MachinePeerLinks AND COMMIT NO CHECK");
			statement.execute("TRUNCATE TABLE MachineIP4Links AND COMMIT NO CHECK");
			statement.execute("TRUNCATE TABLE MachineIP6Links AND COMMIT NO CHECK");
		}
		return true;
	}
	
	public void finalizeSesssion() throws DBManagerFatalException { //Possibly add flag, or make connection class property?
		try(Connection conn = getConnection();
			Statement statement = conn.createStatement()
		){
			statement.execute("SHUTDOWN");
			statement.close();
			conn.close();
		}
		catch (SQLException e) {
			Printer.logErr("An error occured while finalizing the database");
			Printer.logErr(e);
			throw new DBManagerFatalException(e);
		}
	}
	
	public int upsert(String sql) throws SQLException {
		try( Connection conn = getConnection();
			 Statement statement = conn.createStatement()
		){
			return statement.executeUpdate(sql);
		}
	}
	
	public ResultSet select(String sql) throws SQLException {
		try( Connection conn = getConnection();
			 Statement statement = conn.createStatement()
		){
			return statement.executeQuery(sql);
		}
	}
}

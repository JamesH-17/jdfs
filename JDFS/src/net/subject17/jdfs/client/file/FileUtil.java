package net.subject17.jdfs.client.file;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import net.subject17.jdfs.client.io.Printer;
import net.subject17.jdfs.client.file.model.EncryptedFileInfo;

import org.bouncycastle.crypto.digests.SHA3Digest;
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.XZInputStream;
import org.tukaani.xz.XZOutputStream;


public final class FileUtil {
	private final static String tempCompressDirectory = "temp/compress";
	public final static Path compressDirectory = Paths.get(System.getProperty("user.dir"),tempCompressDirectory);

	private final static int maxBufferShift = 26;
	
	private final static int numRoundsToHash = 10000;
	private final static String saltsies = "JDFS-AprilLover~Java*Distributed_File.System^";
	
	
	private static FileUtil _instance = null;
	
	protected FileUtil() throws IOException {
		Printer.log("Created File Util!");
		
		if (!Files.exists(compressDirectory.getParent()))
			Files.createDirectory(compressDirectory.getParent());
		if (!Files.exists(compressDirectory))
			Files.createDirectory(compressDirectory);
	}
	
	public static FileUtil getInstance() throws IOException {
		if (null == _instance){
			synchronized(FileUtil.class){
				if (null == _instance){
					_instance = new FileUtil(); //Can't do easy instantiation since I wanna be able to throw that exception
				}
			}
		}
		return _instance;
	};
	
	public Path compressFile(Path inPath) throws IOException {
		
		Path outPath = compressDirectory.resolve(inPath.getFileName()+".xz");
		Files.deleteIfExists(outPath); //Talk about dangerous.  Hey!!! LOOK OUT FOR THREADING!!!
		
		//Set up options
        LZMA2Options options;
        
		try {
			final int blockSize = 1 << Math.min(maxBufferShift,29); //1<<29 bytes == 512 MiB
			
			options = new LZMA2Options(
					blockSize,					//How big of a dictionary you wish to use
					LZMA2Options.LC_DEFAULT,	//No clue
					LZMA2Options.LP_DEFAULT,	//No Clue
					4,							//Word Boundary
					LZMA2Options.MODE_NORMAL,	//Normal/fast/uncompressed mode, we want normal
					LZMA2Options.NICE_LEN_MAX,	//Max length of a run (Wiki LZMA if you don't know what a run is)
					LZMA2Options.MF_BT4,		//Basically, tree or hashcode.  The description in his source is conflicting, but I believe the tree gives better compression
					700							//Max depth of the tree
				);
			//throw new Exception ("Manually throwing error since we seem to get a heap exception on custom");
		} catch (Exception e) {
			Printer.logErr("Error in setting our custom options for LZMA compression, using default");
			Printer.logErr(e);
			options = new LZMA2Options();
		}
		
		
		//Compress and write file
        Printer.log("Writing file of size "+Files.size(inPath));
		
		try (
	        BufferedOutputStream outFStream = new BufferedOutputStream(new FileOutputStream(outPath.toFile()));
	        XZOutputStream compressedOut = new XZOutputStream(outFStream, options)
        ){    
	        readFileToStream(inPath, compressedOut);
	        compressedOut.finish();
	        
		} //Streams auto closed
		return outPath;
	}
	
	public Path decompressFile(Path inPath, Path targetPath) throws IOException {
		Files.deleteIfExists(targetPath);
		
		try (
	        InputStream fInStream = new FileInputStream(inPath.toFile());
			XZInputStream decompStream = new XZInputStream(fInStream);
	        BufferedOutputStream fOutStream = new BufferedOutputStream(new FileOutputStream(targetPath.toFile())) )
	    {
		
			int size;
			byte[] bytesToWrite = new byte[getBuffSize(inPath)];
			
			while ((size = decompStream.read(bytesToWrite)) != -1) {
				fOutStream.write(bytesToWrite, 0, size);
			}
			
			fOutStream.flush();
			
		} //Streams are auto closed
        
		return targetPath;
	}
	
	public EncryptedFileInfo compressAndEncryptFile(Path pathToRead, String plaintextPassword) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IOException {
		
		//Set output directory
		Path outputLoc = compressDirectory.resolve(pathToRead.getFileName().toString()+".xz.enc");
		Files.deleteIfExists(outputLoc); //Talk about dangerous.  Hey!!! LOOK OUT FOR THREADING!!!
		
		//Get cipher
		Cipher ciph = getEncryptCipher(plaintextPassword);
		
		//Set up options
        LZMA2Options options;
        
		try {
			final int blockSize = 1 << Math.min(maxBufferShift,29); //1<<29 bytes == 512 MiB
			
			options = new LZMA2Options(
					blockSize,					//How big of a dictionary you wish to use
					LZMA2Options.LC_DEFAULT,	//No clue
					LZMA2Options.LP_DEFAULT,	//No Clue
					4,							//Word Boundary
					LZMA2Options.MODE_NORMAL,	//Normal/fast/uncompressed mode, we want normal
					LZMA2Options.NICE_LEN_MAX,	//Max length of a run (Wiki LZMA if you don't know what a run is)
					LZMA2Options.MF_BT4,		//Basically, tree or hashcode.  The description in his source is conflicting, but I believe the tree gives better compression
					700							//Max depth of the tree
				);
			//throw new Exception ("Manually throwing error since we seem to get a heap exception on custom");
		} catch (Exception e) {
			Printer.logErr("Error in setting our custom options for LZMA compression, using default");
			Printer.logErr(e);
			options = new LZMA2Options();
		}
		
		
		//Compress and write file
        Printer.log("Writing file of size "+Files.size(pathToRead));
		
		try (
	        BufferedOutputStream outFStream = new BufferedOutputStream(new FileOutputStream(outputLoc.toFile()));
			CipherOutputStream encryptOut = new CipherOutputStream(outFStream,ciph);
	        XZOutputStream compressedOut = new XZOutputStream(encryptOut, options);
        ){    
	        readFileToStream(pathToRead, compressedOut);
	        
	        //Flush streams
	        compressedOut.finish();
	        encryptOut.flush();
	        outFStream.flush();
	        
		} //Streams auto closed
		
		return new EncryptedFileInfo(outputLoc, ciph.getIV());
	}
	
	public Path decryptAndExtractFile(EncryptedFileInfo efi, Path targetPath, String plaintextPassword) throws InvalidKeyException, FileNotFoundException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, IOException{
		return decryptAndExtractFile(efi.fileLocation, targetPath, plaintextPassword, new IvParameterSpec(efi.IV));
	}
	
	public Path decryptAndExtractFile(Path pathToRead, Path targetPath, String plaintextPassword, IvParameterSpec aesIV) throws FileNotFoundException, IOException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException {
		Files.deleteIfExists(targetPath);
		
		//Get cipher
		Cipher ciph = getDecryptCipher(plaintextPassword, aesIV);		
		
		try (
	        InputStream fInStream = new FileInputStream(pathToRead.toFile());
			CipherInputStream ciphInStream = new CipherInputStream(fInStream,ciph);
			XZInputStream decompStream = new XZInputStream(ciphInStream);
	        BufferedOutputStream fOutStream = new BufferedOutputStream(new FileOutputStream(targetPath.toFile())) )
	    {
		
			readStreamToStream(decompStream, fOutStream);
			
			//Flush streams
			fOutStream.flush();
			
		} //Streams are auto closed
        
		return targetPath;
	}
	
	
	/////////////////  You shouldn't actually use these two in practice, but they're here if needed.  Also, good for testing.
	
	public Path encryptFile(Path path, String plaintextPassword) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IOException {
		
		Cipher ciph = getEncryptCipher(plaintextPassword);
		Path outPath = compressDirectory.resolve(path.getFileName().toString()+".enc");
		
		CipherOutputStream ciphOut = new CipherOutputStream(new FileOutputStream(outPath.toFile()),ciph);
		readFileToStream(path,ciphOut);
		ciphOut.flush();
		
		return outPath;
	}
	
	public Path decryptFile(Path path, String plaintextPassword) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, FileNotFoundException, IOException, InvalidAlgorithmParameterException {
		
		Path outF = path.resolve(path.getParent()).resolve(path.getFileName().toString()+".dec.txt");
		Files.deleteIfExists(outF);
		
		Cipher ciph = getDecryptCipher(plaintextPassword, getLaughablyUnsecureDefaultIV());
		
		Printer.log("CIPH:"+ciph.toString());
		
		FileOutputStream fOut = new FileOutputStream(outF.toFile());
		
		CipherInputStream ciphIn = new CipherInputStream(new FileInputStream(path.toFile()),ciph);
		
		readStreamToStream(ciphIn,fOut);
		
		fOut.write("TEST OVER".getBytes());
		fOut.flush();
		return outF;
	}
	
	/**
	 * Reads data from 1 stream to the other, 1MiB at a time
	 * @param inStream stream to read data from
	 * @param outStream stream to write data to
	 * @throws IOException Thrown if there is an issue (reading from)/(writing to) the streams
	 */
	public void readStreamToStream(InputStream inStream, OutputStream outStream) throws IOException {
		
		//Set up the buffer that we'll read into
		byte[] fileBytes = new byte[1<<20];
		
		//Now, read the file into said buffer
		int bytesRead;
		while ((bytesRead = inStream.read(fileBytes)) != -1) {
			outStream.write(fileBytes,0,bytesRead);
			Printer.log("Writing");
		}
		Printer.log("Written");
	}
	
	/**
	 * Note that this function does not close the 
	 * @param path Reads in the data from this location
	 * @param outStream incrementally puts the data onto this stream
	 * @throws IOException Thrown if an issue occurs accessing input file or in writing to outStream
	 */
	public void readFileToStream(Path path, OutputStream outStream) throws IOException {
		
		//Set up the buffer that we'll read into
		byte[] fileBytes = new byte[getBuffSize(path)];
		ByteBuffer fileBytesBuff = ByteBuffer.wrap(fileBytes);
		
		//Now, read the file into said buffer
		int bytesRead;
		FileChannel file = FileChannel.open(path);
		while ((bytesRead = file.read(fileBytesBuff)) != -1) {
			outStream.write(fileBytesBuff.array(),0,bytesRead);
			fileBytesBuff.clear();
		}
	}
	
	/**
	 * Reads file located at <b>path</b> into a ByteBuffer, then returns said buffer
	 * 
	 * @param path Path to read file from
	 * @return ByteBuffer of size <i>(int)Files.size(path)</i> containing the raw bytes of the file
	 * @throws IOException Throws IOException if file cannot be read, if file size is greater than INT_MAX
	 */
	public ByteBuffer readInFile(Path path) throws IOException{
		
		if (Integer.MAX_VALUE <= Files.size(path))
			throw new IOException("Error:  File too large");
		
		//Set up the buffer that we'll read into
		int fileSize = (int)  Files.size(path);
		byte[] fileBytes = new byte[fileSize];
		ByteBuffer fileBytesBuff = ByteBuffer.wrap(fileBytes);
		
		//Now, read the file into said buffer
		FileChannel.open(path).read(fileBytesBuff);
		
		return fileBytesBuff;
	}
	
	/**
	 * @param path the path to be evaluated
	 * @return returns true if the string represents a valid directory
	 * Returns true if the string represents a valid directory path on windows, unix, and mac
	 */
	public boolean isValidDirectory(String path){
		boolean ret = true;
		try {
			path = new File(path).getCanonicalPath().toLowerCase();
			String[] paths = path.split(java.io.File.separator);
			
			for(String pth : paths){
				ret &= isValidXplatformName(pth);
			}
			
		} catch(IOException e){
			ret =  false;
		}
		return ret;
	}
	
	private boolean isValidXplatformName(String pth) {
		return !(pth.contains("<") || pth.contains(">") || pth.contains("|") || pth.contains(":") || pth.contains("*") || pth.contains("?"));
		//return pth.matches("/[]");
		///return pth.matches("[^<>\\*:\\|\\?]");
		//return !(pth.matches("<*>*:*\"[*]*|*"));
	}
	
	private boolean isValidRootWindows(String prefix) {
		return prefix.matches("[a-z]+:\\");
	}
	
	private int getBuffSize(Path path) throws IOException{
		return 1 << Math.min( Math.max(Long.SIZE - Long.numberOfLeadingZeros(Files.size(path)),13), 26);
	}
	
	
	
	
	//////////////////////////////////////////////
	//			Encryption Utilities			//
	//////////////////////////////////////////////
	
	//////  Ciphers //////
	
	private Cipher getEncryptCipher(String plaintextPassword) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException {
		//Generate key given password
		byte[] key = Arrays.copyOf(getSecureDigest(plaintextPassword),16); //TODO use bouncy castle eventually
		SecretKeySpec oKey = new SecretKeySpec(key,"AES");
		
		//instantiate cipher
		Cipher ciph = Cipher.getInstance("AES/CBC/PKCS5Padding");
		ciph.init(Cipher.ENCRYPT_MODE,oKey);
		
		return ciph;
	}
	
	private Cipher getDecryptCipher(String plaintextPassword, IvParameterSpec iv) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
		//Generate key given password
		byte[] key = Arrays.copyOf(getSecureDigest(plaintextPassword),16); //TODO use bouncy castle eventually
		SecretKeySpec oKey = new SecretKeySpec(key,"AES");
		
		//instantiate cipher
		Cipher ciph = Cipher.getInstance("AES/CBC/PKCS5Padding");
		ciph.init(Cipher.DECRYPT_MODE, oKey, iv);
		ciph.getIV();
		
		return ciph;
	}
	
	////// Cryptographic Hashes/Digests //////
	
	private byte[] getSaltedSha256Digest(byte toHash[]) throws NoSuchAlgorithmException{
		//Once Java gets SHA-3 as a standard, I'd like to use it just for the hell of it.  Alternatively, make it a user choice
		MessageDigest digest = MessageDigest.getInstance("SHA-256"); //SHA2, using keysize of 256 bits
		digest.update(saltsies.getBytes());
		digest.update(toHash);
		
		return digest.digest();
	}

	private byte[] getSaltedSha3Digest(byte[] toHash) {
		SHA3Digest sha3 = new SHA3Digest();
		
		sha3.update(saltsies.getBytes(), 0, saltsies.getBytes().length);
		sha3.update(toHash, 0, toHash.length);
		
		byte[] sha3Digest = new byte[sha3.getDigestSize()];
		sha3.doFinal(sha3Digest, 0);

		sha3.reset();
		
		return sha3Digest;
	}
	
	private byte[] getSecureDigest(String plaintextPassword) throws NoSuchAlgorithmException {
		//We use both Sha3 and Sha2
		byte[] digest = plaintextPassword.getBytes();
		
		for (int i = 0; i < numRoundsToHash; ++i) {
			digest = 0 == (i & 1) ? getSaltedSha3Digest(digest) : getSaltedSha256Digest(digest);
		}
		digest = getSaltedSha256Digest(digest);
		return digest;
	}
	
	/**
	 * This function returns a unique IV for security.  The program is not fit for production while
	 * this function remains used for encryption, but it's needed for the program to work.
	 * 
	 * Will finish implementation after other, more essential features are implemented
	 * 
	 * @return returns a constant IV used for testing purposes.
	 */
	private final IvParameterSpec getLaughablyUnsecureDefaultIV() {
		byte[] iv = "klj1234@#J42#:$J@#4,.jk23l;'4jk".getBytes();
		for (int i = 0; i < 10000; ++i)
			iv = getSaltedSha3Digest(iv);
		return new IvParameterSpec(iv,0,16);
	}
}

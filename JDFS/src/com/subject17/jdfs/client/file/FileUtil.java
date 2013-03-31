package com.subject17.jdfs.client.file;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.XZInputStream;
import org.tukaani.xz.XZOutputStream;

import com.subject17.jdfs.JDFSUtil;
import com.subject17.jdfs.client.io.Printer;

public class FileUtil {
	private final static String tempCompressDirectory = "temp/compress";
	public final static Path compressDirectory = Paths.get(System.getProperty("user.dir"),tempCompressDirectory);

	private final static int maxBufferShift = 26;
	
	private static FileUtil _instance = null;
	
	protected FileUtil() throws IOException{
		Printer.log("Created File Util!");
		
		if (!Files.exists(compressDirectory.getParent()))
			Files.createDirectory(compressDirectory.getParent());
		if (!Files.exists(compressDirectory))
			Files.createDirectory(compressDirectory);
	}
	
	public static FileUtil getInstance() throws IOException {
		synchronized(FileUtil.class){
			if (_instance == null){
				synchronized(FileUtil.class){
					_instance = new FileUtil();
				}
			}
		}
		return _instance;
	};
	/*
	public static void checkIfFileReadable(File toCheck) throws FileNotFoundException, IOException {
		if (!toCheck.isFile() || !toCheck.exists() || toCheck.isDirectory())
			throw new FileNotFoundException("Provided parameter is not a valid file");
		if (!toCheck.canRead())
			throw new IOException("Cannot read from file "+toCheck.getAbsolutePath()+" for some reason");
	}
	
	public static boolean isValidDirectory(File loc){
		return (loc.isDirectory());
	}
	
	public static boolean isValidDirectory(String loc){
		return isValidDirectory(new File(loc));
	}*/
	
	
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
		
			//I feel like doing a bit of math for once in my code.
			//This is unneeded, you could easily just hardcode the buff size
			//-----
			 
			//-----
			
			//<pun>Back to your regularly scheduled programming </pun>
			int size;
			byte[] bytesToWrite = new byte[getBuffSize(inPath)];
			
			while ((size = decompStream.read(bytesToWrite)) != -1) {
				fOutStream.write(bytesToWrite, 0, size);
			}
			
			fOutStream.flush();
			
		} //Streams are auto closed
        
		return targetPath;
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
		}
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
}

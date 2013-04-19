package net.subject17.jdfs.security;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import net.subject17.jdfs.client.io.Printer;

import org.bouncycastle.crypto.digests.SHA3Digest;

public final class JDFSSecurity {
	private final static int numRoundsToHash = 10_000;
	private final static String saltsies = "JDFS-AprilLover~Java*Distributed_File.System^";
	public final static int NUM_IV_BYTES = numBytesInIV();
	//////////////////////////////////////////////
	//			Encryption Utilities			//
	//////////////////////////////////////////////
	
	//////  Ciphers //////
	public static Cipher getEncryptCipher(String plaintextPassword) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException {
		return getEncryptCipher(getSecureDigest(plaintextPassword));
	}
	public static Cipher getEncryptCipher(byte[] secureDigest) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException {
		//Generate key given password
		byte[] key = Arrays.copyOf(secureDigest,16); //TODO use bouncy castle eventually
		SecretKeySpec oKey = new SecretKeySpec(key,"AES");
		
		//instantiate cipher
		Cipher ciph = Cipher.getInstance("AES/CBC/PKCS5Padding");
		ciph.init(Cipher.ENCRYPT_MODE,oKey);
		
		return ciph;
	}
	public static Cipher getDecryptCipher(String plaintextPassword, IvParameterSpec iv) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
		return getDecryptCipher(getSecureDigest(plaintextPassword),iv);
	}
	public static Cipher getDecryptCipher(byte[] secureDigest, IvParameterSpec iv) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
		//Generate key given password
		byte[] key = Arrays.copyOf(secureDigest, 16); //TODO use bouncy castle eventually
		SecretKeySpec oKey = new SecretKeySpec(key,"AES");
		
		//instantiate cipher
		Cipher ciph = Cipher.getInstance("AES/CBC/PKCS5Padding");
		ciph.init(Cipher.DECRYPT_MODE, oKey, iv);
		ciph.getIV();
		
		return ciph;
	}
	
	////// Cryptographic Hashes/Digests //////
	
	public static byte[] getSaltedSha256Digest(byte toHash[]) throws NoSuchAlgorithmException{
		//Once Java gets SHA-3 as a standard, I'd like to use it just for the hell of it.  Alternatively, make it a user choice
		MessageDigest digest = MessageDigest.getInstance("SHA-256"); //SHA2, using keysize of 256 bits
		digest.update(saltsies.getBytes());
		digest.update(toHash);
		
		return digest.digest();
	}
	
	public static byte[] getSaltedSha3Digest(byte[] toHash) {
		SHA3Digest sha3 = new SHA3Digest();
		
		sha3.update(saltsies.getBytes(), 0, saltsies.getBytes().length);
		sha3.update(toHash, 0, toHash.length);
		
		byte[] sha3Digest = new byte[sha3.getDigestSize()];
		sha3.doFinal(sha3Digest, 0);
		
		sha3.reset();
		
		return sha3Digest;
	}
	
	public static byte[] getSecureDigest(String plaintextPassword) throws NoSuchAlgorithmException {
		//We use both Sha3 and Sha2
		byte[] digest = plaintextPassword.getBytes();
	
		for (int i = 0; i < numRoundsToHash; ++i) {
			digest = 0 == (i & 1) ? getSaltedSha3Digest(digest) : getSaltedSha256Digest(digest);
		}
		digest = getSaltedSha256Digest(digest);
		return digest;
	}
	
	///////////////////////////////////////
	//				Misc
	
	private final static int numBytesInIV() {
		try {
			return Cipher.getInstance("AES/CBC/PKCS5Padding").getIV().length;
		} catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
			Printer.logErr("Major error here! Error thrown when initializing IV length. Using default of 128", Printer.Level.High);
			Printer.logErr(e);
			return 128;
		}
	}
}

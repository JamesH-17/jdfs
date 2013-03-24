package com.subject17.jdfs.client.net;

import java.nio.ByteBuffer;

public final class NetworkUtil {
	public static final int numBytesInInt = 4;
	
	public static int convertBytesToInt(byte[] bytes) {
		return ByteBuffer.wrap(bytes).getInt();
	} 
}

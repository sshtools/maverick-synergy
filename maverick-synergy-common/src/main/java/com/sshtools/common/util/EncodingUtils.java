package com.sshtools.common.util;

import java.io.UnsupportedEncodingException;

public class EncodingUtils {

	public static String getUTF8String(byte[] str) {
		try {
			return new String(str, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException("System does not appear to support UTF-8 encoding!");
		}
	}
	
	public static byte[] getUTF8Bytes(String str) {
		try {
			return str.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException("System does not appear to support UTF-8 encoding!");
		}
	}
}

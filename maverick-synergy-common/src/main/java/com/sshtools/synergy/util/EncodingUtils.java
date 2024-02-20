package com.sshtools.synergy.util;

import java.io.UnsupportedEncodingException;
import java.util.Objects;

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
			if(Objects.isNull(str)) {
				return null;
			}
			return str.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException("System does not appear to support UTF-8 encoding!");
		}
	}
}

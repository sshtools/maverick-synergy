package com.sshtools.common.util;

public class FileSystemUtils {

	public static String addTrailingSlash(String str) {
		if(!str.endsWith("/")) {
			return str + "/";
		} else {
			return str;
		}	
	}
	
	public static String removeTrailingSlash(String str) {
		if(str.endsWith("/")) {
			return str.substring(0, str.length()-1);
		} else {
			return str;
		}	
	}

	public static String removeStartingSlash(String str) {
		if(str.startsWith("/")) {
			return str.substring(1);
		}
		return str;
	}
	
	public static String addStartingSlash(String str) {
		if(str.startsWith("/")) {
			return str;
		}
		return "/" + str;
	}

	public static String getFilename(String path) {
		int idx = path.indexOf("/");
		if(idx > -1) {
			return path.substring(idx+1);
		} else {
			return path;
		}
	}
}

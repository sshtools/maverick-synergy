package com.sshtools.vsession.commands.ssh;

public class CommandUtil {

	public static boolean isNotEmpty(String str) {
		return str != null && str.trim().length() > 0;
	}
	
	public static boolean isEmpty(String str) {
		return !isNotEmpty(str);
	}

}

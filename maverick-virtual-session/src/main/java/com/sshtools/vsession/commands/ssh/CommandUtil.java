package com.sshtools.vsession.commands.ssh;

import java.util.List;

public class CommandUtil {
	
	public static boolean isNotEmpty(String str) {
		return str != null && str.trim().length() > 0;
	}
	
	public static boolean isEmpty(String str) {
		return !isNotEmpty(str);
	}
	
	public static boolean isNotEmpty(String[] array) {
		return array != null && array.length > 0;
	}
	
	public static boolean isEmpty(String[] array) {
		return !isNotEmpty(array);
	}

	public static String csv(List<String> strings) {
		return String.join(",", strings.toArray(new String[0]));
	}
}

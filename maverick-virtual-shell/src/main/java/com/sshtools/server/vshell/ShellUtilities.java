package com.sshtools.server.vshell;


public class ShellUtilities {

	public static String padString(String s, int pad) {
		String ret = s;

		if (ret.length() < pad) {
			for (int i = 0; i < (pad - s.length()); i++) {
				ret += " ";
			}
		}

		return ret.substring(0, pad);
	}

	public static String repeat(String s, int len) {
		String ret = "";

		for (int i = 0; i < len; i += s.length()) {
			ret += s;
		}

		return ret;
	}
}

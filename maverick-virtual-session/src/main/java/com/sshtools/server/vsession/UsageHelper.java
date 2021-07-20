
package com.sshtools.server.vsession;

public class UsageHelper {

	public static String build(String... lines) {
		StringBuffer buf = new StringBuffer();
		for(String line : lines) {
			if(buf.length() > 0) {
				buf.append("\r\n");
			}
			buf.append(line);
		}
		return buf.toString();
	}

}

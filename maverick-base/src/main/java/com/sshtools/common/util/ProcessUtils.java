package com.sshtools.common.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;


public class ProcessUtils {

	public static String executeCommand(String... args) throws IOException {
		
		
		 Process process = new ProcessBuilder(Arrays.asList(args)).start();
		 
		 ByteArrayOutputStream out = new ByteArrayOutputStream();
		 InputStream in = process.getInputStream();
		 
		 try {
			 IOUtil.copy(in, out);
		 } finally {
			 in.close();
		 }
		 return new String(out.toByteArray(), "UTF-8");
	}
}

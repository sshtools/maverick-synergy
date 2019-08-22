package com.sshtools.common.tests;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;

public class AbstractFileNIOProviderTests {

	
	
	
	@Test
	public void testFilesystemCreation() throws IOException {
		
		Map<String,Object> env = new HashMap<>();
		env.put("connection", new MockConnection("lee", 
				UUID.randomUUID().toString(),
				new InetSocketAddress(InetAddress.getLocalHost(), 22),
				new InetSocketAddress(InetAddress.getLocalHost(), 22)));
		
		FileSystem fs = FileSystems.newFileSystem(URI.create("abfs://"), env);
		Path path = fs.getPath("");
		
	}
}

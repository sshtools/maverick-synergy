package com.sshtools.synergy.niofs;

import static org.junit.Assert.assertEquals;

import java.util.Optional;

import org.junit.Test;

public class SftpPathBuilderTest extends AbstractNioFsTest {
	@Test
	public void testDefaultOnly() {
		var path = SftpPathBuilder.create().build();
		assertEquals("sftp://guest@localhost/", path.toString());
	}

	@Test
	public void testHostnameOnly() {
		var path = SftpPathBuilder.create().
				withHost("somehost").build();
		assertEquals("sftp://guest@somehost/", path.toString());
	}
	
	@Test
	public void testHostnameAndPort() {
		var path = SftpPathBuilder.create().
				withPort(12345).
				withHost("somehost").build();
		assertEquals("sftp://guest@somehost:12345/", path.toString());
	}
	
	@Test
	public void testHostnamePortAndPath() {
		var path = SftpPathBuilder.create().
				withPort(12345).
				withPath("/home/xxxxx").
				withHost("somehost").build();
		assertEquals("sftp://guest@somehost:12345//home/xxxxx", path.toString());
	}
	
	@Test
	public void testHostnameUsernamePortAndPath() {
		var path = SftpPathBuilder.create().
				withPort(12345).
				withUsername("joeb").
				withPath("/home/xxxxx").
				withHost("somehost").build();
		assertEquals("sftp://joeb@somehost:12345//home/xxxxx", path.toString());
	}
	
	@Test
	public void testHostnameUsernamePasswordPortAndPath() {
		var path = SftpPathBuilder.create().
				withPort(12345).
				withPassword("QWErty123!\":@#").
				withUsername("joeb").
				withPath("/home/xxxxx").
				withHost("somehost").build();
		assertEquals("sftp://joeb:QWErty123!%22:%40%23@somehost:12345//home/xxxxx", path.toString());
	}
	
	@Test
	public void testCharArrayPassword() {
		var path = SftpPathBuilder.create().
				withPasswordCharacters("QWErty123!\":@#".toCharArray()).
				build();
		assertEquals("sftp://guest:QWErty123!%22:%40%23@localhost/", path.toString());
	}
	
	@Test
	public void testOptioanlPassword() {
		var path = SftpPathBuilder.create().
				withPassword(Optional.of("Qwerty")).
				build();
		assertEquals("sftp://guest:Qwerty@localhost/", path.toString());
	}
}

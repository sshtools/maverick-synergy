package com.sshtools.synergy.niofs;

/*-
 * #%L
 * Nio Filesystem Tests
 * %%
 * Copyright (C) 2002 - 2026 JADAPTIVE Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.ConnectException;
import java.nio.file.Files;
import java.util.Map;

import org.junit.Test;

import com.sshtools.client.SshClient.SshClientBuilder;
import com.sshtools.client.sftp.SftpClient.SftpClientBuilder;

public class SftpFileSystemsTest extends AbstractNioFsTest {

	@Test
	public void testNewSftpFileSystemFromExistingClient() throws Exception {
		try (var ssh = SshClientBuilder.create().
				withTarget("localhost", port).
				withUsername("test").
				withPassword("test")
				.build()) {
			try (var sftp = SftpClientBuilder.create().withClient(ssh).build()) {
				try(var fs = SftpFileSystems.newFileSystem(sftp)) {
					var path = fs.getPath("testFile");
					System.out.println("path: " + path + " abspath: " + path.toAbsolutePath().toString());
					Files.createFile(path);
				}
			}
		}
		assertTrue("testFile must exist", Files.exists(tmpDir.resolve("testFile")));
	}

	@Test(expected = IOException.class)
	public void testFailNewSftpFileSystemFromExistingClient() throws Exception {
		try (var ssh = SshClientBuilder.create().
				withTarget("localhost", port).
				withUsername("test").
				withPassword("test").
				build()) {
			var sftp = SftpClientBuilder.create().withClient(ssh).build();
			sftp.close();
			try(var fs = SftpFileSystems.newFileSystem(sftp)) {
				Files.createFile(fs.getPath("testFile"));
			}
		}
		assertTrue("testFile must exist", Files.exists(tmpDir.resolve("testFile")));
	}

	@Test(expected = ConnectException.class)
	public void testFailNewSftpFileSystemFromMap() throws Exception {
		try(var fs = SftpFileSystems.newFileSystem(Map.of(
				SftpFileSystemProvider.USERNAME, "test",
			    SftpFileSystemProvider.PASSWORD, "test",
			    SftpFileSystemProvider.HOSTNAME, "localhost",
			    SftpFileSystemProvider.PORT, 3333
				))) {
			Files.createFile(fs.getPath("testFile"));
		}
		assertTrue("testFile must exist", Files.exists(tmpDir.resolve("testFile")));
	}

	@Test(expected = Exception.class)
	public void testFailNewSftpFileSystemFromMapDefaultPort() throws Exception {
		try(var fs = SftpFileSystems.newFileSystem(Map.of(
				SftpFileSystemProvider.USERNAME, "XXXXXXXXXXXXXXXXXXXXXXXXXXX",
			    SftpFileSystemProvider.PASSWORD, "XXXXXXXXXXXXXXXXXXXXXXXXXXX",
			    SftpFileSystemProvider.HOSTNAME, "localhost",
			    SftpFileSystemProvider.PORT, 22
				))) {
			Files.createFile(fs.getPath("testFile"));
		}
		assertTrue("testFile must exist", Files.exists(tmpDir.resolve("testFile")));
	}

	@Test
	public void testNewSftpFileSystemFromMap() throws Exception {
		try(var fs = SftpFileSystems.newFileSystem(Map.of(
				SftpFileSystemProvider.USERNAME, "test",
			    SftpFileSystemProvider.PASSWORD, "test",
			    SftpFileSystemProvider.HOSTNAME, "localhost",
			    SftpFileSystemProvider.PORT, port
				))) {
			Files.createFile(fs.getPath("testFile"));
		}
		assertTrue("testFile must exist", Files.exists(tmpDir.resolve("testFile")));
	}
}

package com.sshtools.synergy.niofs;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.ProcessBuilder.Redirect;
import java.net.URI;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sshtools.client.SshClient.SshClientBuilder;
import com.sshtools.client.sftp.SftpClient;
import com.sshtools.client.sftp.SftpClient.SftpClientBuilder;

public class SftpFileSystemTestNoSandbox extends AbstractNioFsTest {

	@BeforeClass
	public static void setup() throws Exception {
		sandbox = false;
	}

	@AfterClass
	public static void teardown() throws Exception {
		sandbox = true;
	}

	@Test
	public void testResolveRootOnDefaultRoot() throws Exception {
		try (var ssh = SshClientBuilder.create().
				withTarget("localhost", port).
				withUsername("test").
				withPassword("test").
				build()) {
			var sftp = SftpClientBuilder.create().
					withClient(ssh).
					withRemotePath("/").
					build();
			try (var fs = SftpFileSystems.newFileSystem(sftp)) {
				assertTrue(Files.exists(fs.getPath("/")));
			}
		}
	}

	@Test
	public void testResolveEmptyOnDefaultRoot() throws Exception {
		try (var ssh = SshClientBuilder.create().
				withTarget("localhost", port).
				withUsername("test").
				withPassword("test")
				.build()) {
			var sftp = SftpClientBuilder.create().
					withClient(ssh).
					withRemotePath("").
					build();
			try (var fs = SftpFileSystems.newFileSystem(sftp)) {
				assertTrue(Files.exists(fs.getPath("")));
			}
		}
	}

	@Test
	public void testResolveAbsoluteOnDefaultRoot() throws Exception {
		try (var ssh = SshClientBuilder.create().
				withTarget("localhost", port).
				withUsername("test").
				withPassword("test")
				.build()) {
			var sftp = SftpClientBuilder.create().
					withClient(ssh).
					withRemotePath("").
					build();
			try (var fs = SftpFileSystems.newFileSystem(sftp)) {
				assertTrue(Files.exists(fs.getPath("/")));
			}
		}
	}

	@Test (expected = IOException.class)
	public void testBadRoot() throws Exception {
		try (var ssh = SshClientBuilder.create().
				withTarget("localhost", port).
				withUsername("test").
				withPassword("test").
				build()) {
			var sftp = SftpClientBuilder.create().
					withClient(ssh).
					withRemotePath("/home/ZZZZZZ/some/bad/path").build();
			try (var fs = SftpFileSystems.newFileSystem(sftp)) {
			}
		}
	}

	@Test 
	public void testBadRootDefaultPath() throws Exception {
		try (var ssh = SshClientBuilder.create().
				withTarget("localhost", port).
				withUsername("test").
				withPassword("test").
				build()) {
			var sftp = SftpClientBuilder.create().
					withClient(ssh).build();
			try (var fs = SftpFileSystems.newFileSystem(sftp, Paths.get(""))) {
			}
		}
	}

	@Test (expected = IOException.class)
	public void testFailFsWhenClosedDuring() throws Exception {
		try (var ssh = SshClientBuilder.create().
				withTarget("localhost", port).
				withUsername("test").
				withPassword("test").
				build()) {
			var sftp = SftpClientBuilder.create().
					withClient(ssh).
					build();
			ssh.close();
			try (var fs = SftpFileSystems.newFileSystem(sftp)) {
			}
		}
	}

	@Test (expected = IllegalStateException.class)
	public void testFailBuilderWhenClosed() throws Exception {
		try (var ssh = SshClientBuilder.create().
				withTarget("localhost", port).
				withUsername("test").
				withPassword("test").
				build()) {
			ssh.disconnect();
			SftpClientBuilder.create().
					withClient(ssh).
					build();
		}
	}
	
	@Test(expected = UncheckedIOException.class)
	public void testFailFsForDefaultPath() throws Exception {
		try (var ssh = SshClientBuilder.create().
				withTarget("localhost", port).
				withUsername("test").
				withPassword("test").
				build()) {
			SftpClient sftp = SftpClientBuilder.create().
							withClient(ssh).
							build();
			sftp.close();
			try (var fs = SftpFileSystems.newFileSystem(sftp, Paths.get(""))) {
			}
		}
	}
	
	@Test
	public void testOtherFileType() throws Exception {
		var tf = Files.createTempFile("xzzzz", ".test");
		Files.delete(tf);
		assumeTrue(new ProcessBuilder("mkfifo", tf.toString()).redirectError(Redirect.INHERIT).redirectInput(Redirect.INHERIT).redirectOutput(Redirect.INHERIT).start().waitFor() == 0);
		try (var ssh = SshClientBuilder.create().
				withTarget("localhost", port).
				withUsername("test").
				withPassword("test").
				build()) {
			ssh.executeCommand("mkfifo " + tf);
			try (var fs = SftpFileSystems.newFileSystem(SftpClientBuilder.create().
					withClient(ssh).
					withRemotePath("/").
					build())) {
				assertTrue(Files.readAttributes(fs.getPath("/dev/null"), BasicFileAttributes.class).isOther());
				assertTrue(Files.readAttributes(fs.getPath(tf.toString()), BasicFileAttributes.class).isOther());
			}
		}
		finally {
			Files.delete(tf);
		}
	}
	
	@Test(expected = FileSystemNotFoundException.class)
	public void testNoFsWithUri() throws Exception {
		var uri = URI.create("sftp://test:test@localhost:" + port + tmpDir.toString());
		FileSystems.getFileSystem(uri);
	}
	
	@Test
	public void testFromUri() throws Exception {
		var uri = URI.create("sftp://test:test@localhost:" + port + tmpDir.toString());
		var fs = FileSystems.newFileSystem(uri, Collections.emptyMap());
		var file = fs.getPath("testfile");
		Files.createFile(file);
		assertTrue(Files.exists(file));
		assertNotNull(FileSystems.getFileSystem(uri));
	}
	
	@Test(expected = FileSystemAlreadyExistsException.class)
	public void testFromUriTwoSameURI() throws Exception {
		FileSystems.newFileSystem(URI.create("sftp://test:test@localhost:" + port + tmpDir.toString()), Collections.emptyMap());
		FileSystems.newFileSystem(URI.create("sftp://test:test@localhost:" + port + tmpDir.toString()), Collections.emptyMap());
	}
}

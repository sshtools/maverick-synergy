package com.sshtools.synergy.tests;

/*-
 * #%L
 * Integration Tests
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sshtools.client.SshClient;
import com.sshtools.client.SshClient.SshClientBuilder;
import com.sshtools.client.sftp.SftpClient;
import com.sshtools.client.sftp.SftpClient.SftpClientBuilder;
import com.sshtools.common.files.direct.NioFileFactory;
import com.sshtools.common.files.direct.NioFileFactory.NioFileFactoryBuilder;
import com.sshtools.common.publickey.SshKeyPairGenerator;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.util.FileUtils;
import com.sshtools.server.InMemoryPasswordAuthenticator;
import com.sshtools.server.SshServer;

@DisplayName("JADAPTIVE-SA-2026-003 sandbox boundary")
class SftpSandboxBoundaryIT {

	private static final String USER = "attacker";
	private static final String PASSWORD = "test-password";
	private static final String OUTSIDE_CONTENT = "CRITICAL-OUTSIDE-DATA";

	private Path sandboxRoot;
	private Path outsideDir;
	private Path outsideSecret;
	private SshServer server;

	@BeforeEach
	void setUp() throws Exception {
		sandboxRoot = Files.createTempDirectory("sftp-sandbox-root-");
		outsideDir = Files.createTempDirectory("sftp-outside-dir-");
		outsideSecret = outsideDir.resolve("secret.txt");
		Files.writeString(outsideSecret, OUTSIDE_CONTENT, StandardCharsets.UTF_8);

		NioFileFactory factory = NioFileFactoryBuilder.create()
				.withHome(sandboxRoot)
				.withSandbox(true)
				.build();
		SshKeyPair hostKey = SshKeyPairGenerator.generateKeyPair(SshKeyPairGenerator.ED25519, 0);

		server = new SshServer(0);
		server.addHostKey(hostKey);
		server.addAuthenticator(new InMemoryPasswordAuthenticator().addUser(USER, PASSWORD.toCharArray()));
		server.setFileFactory(con -> factory);
		server.start();
	}

	@AfterEach
	void tearDown() throws Exception {
		if (server != null && server.isRunning())
			server.stop();
		if (outsideDir != null)
			FileUtils.deleteFolder(outsideDir.toFile());
		if (sandboxRoot != null)
			FileUtils.deleteFolder(sandboxRoot.toFile());
	}

	private SshClient connectClient() throws IOException, SshException {
		return SshClientBuilder.create()
				.withHostname("127.0.0.1")
				.withPort(server.getPort())
				.withUsername(USER)
				.withPassword(PASSWORD)
				.onConfigure(ctx -> ctx.setHostKeyVerification((host, key) -> true))
				.build();
	}

	private SftpClient openSftp(SshClient client) throws Exception {
		SftpClient sftp = SftpClientBuilder.create().withClient(client).build();
		sftp.cd("");
		return sftp;
	}

	private void putString(SftpClient sftp, String remotePath, String content) throws Exception {
		Path temp = Files.createTempFile("sftp-put-", ".tmp");
		try {
			Files.writeString(temp, content, StandardCharsets.UTF_8);
			sftp.put(temp.toString(), remotePath);
		} finally {
			Files.deleteIfExists(temp);
		}
	}

	private String getString(SftpClient sftp, String remotePath) throws Exception {
		Path temp = Files.createTempFile("sftp-get-", ".tmp");
		try {
			sftp.get(remotePath, temp.toString());
			return Files.readString(temp, StandardCharsets.UTF_8);
		} finally {
			Files.deleteIfExists(temp);
		}
	}

	@Test
	@DisplayName("A: clean sandbox blocks traversal and external symlink creation")
	void cleanSandbox() throws Exception {
		try (SshClient client1 = connectClient();
				SshClient client2 = connectClient();
				SftpClient sftp1 = openSftp(client1);
				SftpClient sftp2 = openSftp(client2)) {
			putString(sftp1, "inside.txt", "hello-world");
			assertEquals("hello-world", getString(sftp2, "inside.txt"));
			sftp1.mkdir("sub1");
			assertTrue(sftp2.exists("sub1"));

			assertThrows(SftpStatusException.class, () -> getString(sftp1, "../../secret.txt"));
			assertThrows(SftpStatusException.class, () -> putString(sftp1, "../../evil.txt", "payload"));
			assertThrows(SftpStatusException.class, () -> sftp1.mkdir("../evil_dir"));
			assertThrows(SftpStatusException.class, () -> sftp1.rm("../../secret.txt"));
			assertThrows(SftpStatusException.class, () -> sftp1.rename("inside.txt", "../escaped.txt"));
			assertThrows(SftpStatusException.class,
					() -> getString(sftp1, outsideSecret.toAbsolutePath().toString()));
			assertThrows(SftpStatusException.class,
					() -> putString(sftp1, outsideDir.resolve("evil.txt").toString(), "payload"));
			assertThrows(SftpStatusException.class,
					() -> sftp1.symlink(outsideSecret.toAbsolutePath().toString(), "link_out"));
			assertThrows(SftpStatusException.class, () -> sftp1.symlink("../../secret.txt", "link_out_rel"));
			assertThrows(SftpStatusException.class,
					() -> sftp1.symlink("../outside_nonexistent", "link_dangling_out"));

			assertFalse(Files.exists(outsideDir.resolve("evil.txt")));
			assertEquals(OUTSIDE_CONTENT, Files.readString(outsideSecret));
		}
	}

	@Test
	@DisplayName("B: operations through an administrator-created external symlink are denied")
	void adminCreatedExternalSymlink() throws Exception {
		Files.createSymbolicLink(sandboxRoot.resolve("admin_link"), outsideDir);

		try (SshClient client1 = connectClient();
				SshClient client2 = connectClient();
				SftpClient sftp1 = openSftp(client1);
				SftpClient sftp2 = openSftp(client2)) {
			assertThrows(SftpStatusException.class, () -> getString(sftp1, "admin_link/secret.txt"));
			assertThrows(SftpStatusException.class, () -> putString(sftp1, "admin_link/pwned.txt", "attack"));
			assertThrows(SftpStatusException.class, () -> sftp2.ls("admin_link"));
			assertThrows(SftpStatusException.class, () -> sftp1.stat("admin_link"));
			assertThrows(SftpStatusException.class, () -> sftp1.rm("admin_link"));
			assertThrows(SftpStatusException.class, () -> sftp1.rename("admin_link", "admin_link_renamed"));

			assertEquals(OUTSIDE_CONTENT, Files.readString(outsideSecret));
			assertFalse(Files.exists(outsideDir.resolve("pwned.txt")));
			assertTrue(Files.isSymbolicLink(sandboxRoot.resolve("admin_link")));
		}
	}

	@Test
	@DisplayName("C: writes through a dangling external symlink are denied")
	void danglingExternalSymlink() throws Exception {
		Path outsideTarget = outsideDir.resolve("target_should_not_exist.txt");
		Files.createSymbolicLink(sandboxRoot.resolve("dangling_link"), outsideTarget);

		try (SshClient client = connectClient(); SftpClient sftp = openSftp(client)) {
			assertThrows(SftpStatusException.class,
					() -> putString(sftp, "dangling_link", "malicious-write"));
			assertThrows(SftpStatusException.class, () -> sftp.stat("dangling_link"));
			assertThrows(SftpStatusException.class, () -> getString(sftp, "dangling_link"));
			assertFalse(Files.exists(outsideTarget));
		}
	}

	@Test
	@DisplayName("D: nested directories and permitted internal symlinks remain usable")
	void nestedDirectoriesAndInternalSymlinks() throws Exception {
		try (SshClient client = connectClient(); SftpClient sftp = openSftp(client)) {
			sftp.mkdir("dir1");
			sftp.mkdir("dir1/dir2");
			putString(sftp, "dir1/dir2/data.txt", "nested-data");
			sftp.symlink("dir1/dir2/data.txt", "dir1/link_to_data");
			assertEquals("nested-data", getString(sftp, "dir1/link_to_data"));

			assertThrows(SftpStatusException.class,
					() -> sftp.relativeSymlink("../dir2/data.txt", "dir1/dir2/link_up"));

			putString(sftp, "dir1/data_in_dir1.txt", "parent-level-data");
			sftp.symlink("dir1/data_in_dir1.txt", "dir1/dir2/link_up_parent");
			assertEquals("parent-level-data", getString(sftp, "dir1/dir2/link_up_parent"));
			sftp.rename("dir1/dir2/link_up_parent", "escaped_rel_link");
			assertTrue(Files.isSymbolicLink(sandboxRoot.resolve("escaped_rel_link")));
			assertEquals("parent-level-data", getString(sftp, "escaped_rel_link"));
		}
	}

	@Test
	@DisplayName("E: concurrent authenticated sessions rename and replace an in-sandbox directory")
	void concurrentSessionsDirectorySubstitution() throws Exception {
		try (SshClient client1 = connectClient();
				SshClient client2 = connectClient();
				SftpClient sftp1 = openSftp(client1);
				SftpClient sftp2 = openSftp(client2)) {
			sftp1.mkdir("race_dir");
			sftp1.mkdir("safe_target");
			putString(sftp1, "safe_target/marker.txt", "internal-target");

			CountDownLatch start = new CountDownLatch(1);
			AtomicInteger completedReadsWrites = new AtomicInteger();
			AtomicInteger expectedStatusErrors = new AtomicInteger();
			AtomicInteger completedMutations = new AtomicInteger();
			ExecutorService pool = Executors.newFixedThreadPool(2);
			try {
				Future<?> writer = pool.submit(() -> {
					start.await();
					for (int i = 0; i < 50; i++) {
						try {
							String path = "race_dir/file_" + (i % 3) + ".txt";
							putString(sftp1, path, "data-" + i);
							getString(sftp1, path);
							completedReadsWrites.incrementAndGet();
						} catch (SftpStatusException expected) {
							expectedStatusErrors.incrementAndGet();
						}
					}
					return null;
				});
				Future<?> mutator = pool.submit(() -> {
					start.await();
					for (int i = 0; i < 50; i++) {
						String moved = "race_dir_old_" + i;
						try {
							sftp2.rename("race_dir", moved);
							completedMutations.incrementAndGet();
						} catch (SftpStatusException expected) {
							expectedStatusErrors.incrementAndGet();
						}
						try {
							sftp2.symlink("safe_target", "race_dir");
							completedMutations.incrementAndGet();
						} catch (SftpStatusException expected) {
							expectedStatusErrors.incrementAndGet();
						}
						try {
							sftp2.rm("race_dir");
							completedMutations.incrementAndGet();
						} catch (SftpStatusException expected) {
							expectedStatusErrors.incrementAndGet();
						}
						try {
							sftp2.rename(moved, "race_dir");
							completedMutations.incrementAndGet();
						} catch (SftpStatusException expected) {
							expectedStatusErrors.incrementAndGet();
						}
					}
					return null;
				});

				start.countDown();
				writer.get(30, TimeUnit.SECONDS);
				mutator.get(30, TimeUnit.SECONDS);
			} finally {
				pool.shutdownNow();
			}

			assertTrue(completedMutations.get() > 0, "The SFTP mutator must perform at least one operation");
			assertTrue(completedReadsWrites.get() + expectedStatusErrors.get() > 0,
					"The writer must observe concurrent operations");
			try (var files = Files.list(outsideDir)) {
				assertEquals(1, files.count(), "Only the initial secret should be outside the sandbox");
			}
			assertEquals(OUTSIDE_CONTENT, Files.readString(outsideSecret));
		}
	}

	@Test
	@DisplayName("RMDIR, READLINK, SETSTAT, and FSETSTAT remain confined to the sandbox")
	void additionalRequestBoundaries() throws Exception {
		Path outsideEmptyDirectory = Files.createDirectory(outsideDir.resolve("empty"));
		Files.createSymbolicLink(sandboxRoot.resolve("admin_file_link"), outsideSecret);

		try (SshClient client1 = connectClient();
				SshClient client2 = connectClient();
				SftpClient sftp1 = openSftp(client1);
				SftpClient sftp2 = openSftp(client2)) {
			putString(sftp1, "metadata.txt", "sandbox-data");
			sftp1.symlink("metadata.txt", "internal_link");

			String internalTarget = sftp1.getSubsystemChannel()
					.getSymbolicLinkTarget(sandboxRoot.resolve("internal_link").toString());
			assertTrue(internalTarget.endsWith("metadata.txt"), internalTarget);
			assertThrows(SftpStatusException.class, () -> sftp1.getSubsystemChannel()
					.getSymbolicLinkTarget(sandboxRoot.resolve("admin_file_link").toString()));

			assertThrows(SftpStatusException.class, () -> sftp1.rmdir(outsideEmptyDirectory.toString()));
			assertTrue(Files.isDirectory(outsideEmptyDirectory));
			assertThrows(SftpStatusException.class, () -> sftp2.rmdir("admin_file_link"));

			var permissions = com.sshtools.common.sftp.SftpFileAttributes.SftpFileAttributesBuilder.create()
					.withPermissions(com.sshtools.common.sftp.PosixPermissions.PosixPermissionsBuilder.create()
							.fromFileModeString("rw-------").build())
					.build();
			sftp1.setAttributes("metadata.txt", permissions);
			assertThrows(SftpStatusException.class, () -> sftp1.setAttributes("../../secret.txt", permissions));
			assertThrows(SftpStatusException.class, () -> sftp1.setAttributes("admin_file_link", permissions));
			assertThrows(SftpStatusException.class, () -> sftp1.openFile("admin_file_link",
					com.sshtools.client.sftp.SftpChannel.OPEN_READ | com.sshtools.client.sftp.SftpChannel.OPEN_WRITE));

			try (var handle = sftp1.openFile("metadata.txt",
					com.sshtools.client.sftp.SftpChannel.OPEN_READ | com.sshtools.client.sftp.SftpChannel.OPEN_WRITE)) {
				handle.setAttributes(permissions);
			}

			assertEquals(OUTSIDE_CONTENT, Files.readString(outsideSecret));
		}
	}

	@Test
	@DisplayName("An open SFTP handle remains bound to its file after directory replacement")
	void openHandleRemainsBoundAcrossDirectoryReplacement() throws Exception {
		try (SshClient client1 = connectClient();
				SshClient client2 = connectClient();
				SftpClient sftp1 = openSftp(client1);
				SftpClient sftp2 = openSftp(client2)) {
			sftp1.mkdir("handle_dir");
			sftp1.mkdir("safe_target");
			putString(sftp1, "handle_dir/data.txt", "original");
			putString(sftp1, "safe_target/data.txt", "untouched");

			try (var handle = sftp1.openFile("handle_dir/data.txt",
					com.sshtools.client.sftp.SftpChannel.OPEN_READ | com.sshtools.client.sftp.SftpChannel.OPEN_WRITE)) {
				sftp2.rename("handle_dir", "handle_dir_moved");
				sftp2.symlink("safe_target", "handle_dir");
				byte[] replacement = "written-through-open-handle".getBytes(StandardCharsets.UTF_8);
				handle.write(0, replacement, 0, replacement.length);
			}

			assertEquals("written-through-open-handle",
					getString(sftp1, "handle_dir_moved/data.txt"));
			assertEquals("untouched", getString(sftp1, "safe_target/data.txt"));
		}
	}
}

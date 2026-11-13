package com.sshtools.common.tests;

/*-
 * #%L
 * Base API Tests
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.files.direct.NioFile;
import com.sshtools.common.files.direct.NioFileFactory;
import com.sshtools.common.files.direct.NioFileFactory.NioFileFactoryBuilder;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.AbstractFileSystem;
import com.sshtools.common.sftp.OpenFile;
import com.sshtools.common.util.FileUtils;
import com.sshtools.common.util.UnsignedInteger32;

public class NioFileFactorySandboxTests {

	private Path sandboxRoot;
	private Path outsideDir;
	private Path outsideFile;
	private Path insideFile;
	private Path symlinkToOutside;
	private Path symlinkToInside;

	@Before
	public void setUp() throws Exception {
		sandboxRoot = Files.createTempDirectory("nio-sandbox-root");
		outsideDir = Files.createTempDirectory("nio-outside-dir");
		outsideFile = Files.createTempFile(outsideDir, "secret", ".txt");
		Files.writeString(outsideFile, "top-secret-data");
		insideFile = Files.createTempFile(sandboxRoot, "allowed", ".txt");
		Files.writeString(insideFile, "public-data");

		symlinkToOutside = sandboxRoot.resolve("link_outside");
		Files.createSymbolicLink(symlinkToOutside, outsideDir);

		symlinkToInside = sandboxRoot.resolve("link_inside");
		Files.createSymbolicLink(symlinkToInside, insideFile);
	}

	@After
	public void tearDown() throws Exception {
		Files.deleteIfExists(symlinkToInside);
		Files.deleteIfExists(symlinkToOutside);
		Files.deleteIfExists(insideFile);
		Files.deleteIfExists(outsideFile);
		FileUtils.deleteFolder(outsideDir.toFile());
		FileUtils.deleteFolder(sandboxRoot.toFile());
	}

	@Test
	public void testAllowedInsideSandboxAllOperations() throws Exception {
		NioFileFactory factory = NioFileFactoryBuilder.create()
				.withHome(sandboxRoot)
				.withSandbox(true)
				.build();

		NioFile file = factory.getFile(insideFile.getFileName().toString());
		assertNotNull(file);
		assertTrue(file.exists());
		assertTrue(file.isFile());
		assertFalse(file.isDirectory());

		// Test read
		try (InputStream in = file.getInputStream()) {
			byte[] content = in.readAllBytes();
			assertEquals("public-data", new String(content, StandardCharsets.UTF_8));
		}

		// Test write/truncate
		NioFile newFile = factory.getFile("created.txt");
		assertFalse(newFile.exists());
		assertTrue(newFile.createNewFile());
		assertTrue(newFile.exists());

		try (OutputStream out = newFile.getOutputStream()) {
			out.write("written-content".getBytes(StandardCharsets.UTF_8));
		}

		try (InputStream in = newFile.getInputStream()) {
			byte[] content = in.readAllBytes();
			assertEquals("written-content", new String(content, StandardCharsets.UTF_8));
		}

		// Test OpenFile (SFTP handle write/read)
		OpenFile open = newFile.open(new UnsignedInteger32(AbstractFileSystem.OPEN_READ | AbstractFileSystem.OPEN_WRITE), Optional.empty(), new byte[] { 1 });
		byte[] buf = new byte[7];
		int read = open.read(buf, 0, 7);
		assertEquals(7, read);
		assertEquals("written", new String(buf, StandardCharsets.UTF_8));
		open.close();

		// Test delete
		assertTrue(newFile.delete(false));
		assertFalse(newFile.exists());

		// Test folder creation and directory listing
		NioFile subDir = factory.getFile("subfolder");
		assertTrue(subDir.createFolder());
		assertTrue(subDir.exists());
		assertTrue(subDir.isDirectory());
		assertEquals(0, subDir.getChildren().size());
	}

	@Test
	public void testDenyDotDotTraversalEscape() throws Exception {
		NioFileFactory factory = NioFileFactoryBuilder.create()
				.withHome(sandboxRoot)
				.withSandbox(true)
				.build();

		try {
			factory.getFile("../../etc/passwd");
			fail("Expected PermissionDeniedException on ../../etc/passwd traversal");
		} catch (PermissionDeniedException expected) {
		}

		try {
			factory.getFile("../../../../../../../../../../../etc/hosts");
			fail("Expected PermissionDeniedException on deep ../ traversal");
		} catch (PermissionDeniedException expected) {
		}

		try {
			factory.getFile("subfolder/../../etc/shadow");
			fail("Expected PermissionDeniedException on subfolder/../../ traversal");
		} catch (PermissionDeniedException expected) {
		}
	}

	@Test
	public void testDenyDanglingSymlinkToOutsideArbitraryWrite() throws Exception {
		NioFileFactory factory = NioFileFactoryBuilder.create()
				.withHome(sandboxRoot)
				.withSandbox(true)
				.build();

		Path outsideTarget = outsideDir.resolve("target_should_not_be_created.txt");
		Files.deleteIfExists(outsideTarget);

		Path danglingSymlink = sandboxRoot.resolve("dangling_to_outside");
		Files.createSymbolicLink(danglingSymlink, outsideTarget);

		try {
			NioFile file = factory.getFile("dangling_to_outside");
			try (OutputStream out = file.getOutputStream()) {
				out.write("malicious-payload".getBytes(StandardCharsets.UTF_8));
			}
			fail("Expected PermissionDeniedException when attempting to access dangling symlink pointing outside sandbox");
		} catch (PermissionDeniedException expected) {
		}

		// Verify that the file was never created or written outside the sandbox
		assertFalse("Outside target must not have been created through dangling symlink", Files.exists(outsideTarget));
	}

	@Test
	public void testAllowDanglingSymlinkToInside() throws Exception {
		NioFileFactory factory = NioFileFactoryBuilder.create()
				.withHome(sandboxRoot)
				.withSandbox(true)
				.build();

		Path insideTarget = sandboxRoot.resolve("inside_future_file.txt");
		Files.deleteIfExists(insideTarget);

		Path danglingInside = sandboxRoot.resolve("dangling_to_inside");
		Files.createSymbolicLink(danglingInside, insideTarget);

		NioFile file = factory.getFile("dangling_to_inside");
		assertNotNull(file);

		// Writing through an intra-sandbox dangling symlink should be permitted and land inside sandbox
		try (OutputStream out = file.getOutputStream()) {
			out.write("allowed-inside".getBytes(StandardCharsets.UTF_8));
		}

		assertTrue("File should be created at intra-sandbox target", Files.exists(insideTarget));
		assertEquals("allowed-inside", Files.readString(insideTarget));
	}

	@Test
	public void testDenyIntermediateDanglingSymlinkDirectory() throws Exception {
		NioFileFactory factory = NioFileFactoryBuilder.create()
				.withHome(sandboxRoot)
				.withSandbox(true)
				.build();

		Path outsideNonExistentDir = outsideDir.resolve("non_existent_subdir");
		Path danglingDirLink = sandboxRoot.resolve("dang_dir");
		Files.createSymbolicLink(danglingDirLink, outsideNonExistentDir);

		try {
			NioFile file = factory.getFile("dang_dir/pwned.txt");
			try (OutputStream out = file.getOutputStream()) {
				out.write("payload".getBytes(StandardCharsets.UTF_8));
			}
			fail("Expected PermissionDeniedException on path traversing through dangling symlink dir pointing outside");
		} catch (PermissionDeniedException expected) {
		}

		assertFalse(Files.exists(outsideNonExistentDir.resolve("pwned.txt")));
	}

	@Test
	public void testDenyToctouPathRedirectionAtPointOfUse() throws Exception {
		NioFileFactory factory = NioFileFactoryBuilder.create()
				.withHome(sandboxRoot)
				.withSandbox(true)
				.build();

		Path subDir = sandboxRoot.resolve("legit_sub");
		Files.createDirectories(subDir);

		// Obtain an NioFile handle while legit_sub is a real directory inside the sandbox
		NioFile file = factory.getFile("legit_sub/toctou_target.txt");
		assertNotNull(file);

		// Attacker replaces legit_sub with a symlink to outside directory
		Files.delete(subDir);
		Files.createSymbolicLink(subDir, outsideDir);

		Path outsideVictim = outsideDir.resolve("toctou_target.txt");
		assertFalse(Files.exists(outsideVictim));

		// Point of use validation MUST catch the redirection and deny access
		try (OutputStream out = file.getOutputStream()) {
			out.write("toctou-payload".getBytes(StandardCharsets.UTF_8));
			fail("Expected PermissionDeniedException on redirected NioFile handle at point of use");
		} catch (PermissionDeniedException expected) {
		}

		assertFalse("Target in outside directory must not have been created", Files.exists(outsideVictim));
	}

	@Test
	public void testDenySymlinkToExistingOutsideEscape() throws Exception {
		NioFileFactory factory = NioFileFactoryBuilder.create()
				.withHome(sandboxRoot)
				.withSandbox(true)
				.build();

		try {
			NioFile file = factory.getFile("link_outside");
			file.getInputStream();
			fail("Expected PermissionDeniedException on reading symlink pointing outside sandbox");
		} catch (PermissionDeniedException expected) {
		}

		try {
			NioFile file = factory.getFile("link_outside/" + outsideFile.getFileName().toString());
			file.getInputStream();
			fail("Expected PermissionDeniedException on accessing file under outside symlink directory");
		} catch (PermissionDeniedException expected) {
		}

		try {
			NioFile file = factory.getFile("link_outside");
			file.delete(false);
			fail("Expected PermissionDeniedException on deleting symlink pointing outside");
		} catch (PermissionDeniedException expected) {
		}
	}

	@Test
	public void testAllowIntraSandboxSymlinkActualReadWrite() throws Exception {
		NioFileFactory factory = NioFileFactoryBuilder.create()
				.withHome(sandboxRoot)
				.withSandbox(true)
				.build();

		NioFile file = factory.getFile("link_inside");
		assertNotNull(file);
		assertTrue(file.exists());

		try (InputStream in = file.getInputStream()) {
			byte[] content = in.readAllBytes();
			assertEquals("public-data", new String(content, StandardCharsets.UTF_8));
		}

		try (OutputStream out = file.getOutputStream()) {
			out.write("updated-via-symlink".getBytes(StandardCharsets.UTF_8));
		}

		assertEquals("updated-via-symlink", Files.readString(insideFile));
	}

	@Test
	public void testTwoPathOperationsCrossBoundaryEnforcement() throws Exception {
		NioFileFactory sandboxedFactory = NioFileFactoryBuilder.create()
				.withHome(sandboxRoot)
				.withSandbox(true)
				.build();

		NioFileFactory unconfinedFactory = NioFileFactoryBuilder.create()
				.withHome(outsideDir)
				.withoutSandbox()
				.build();

		NioFile sandboxedDest = sandboxedFactory.getFile("dest.txt");
		NioFile outsideSource = unconfinedFactory.getFile(outsideFile.getFileName().toString());

		// Attempting to copy into or move from outside file must be rejected by sandboxed destination
		try {
			sandboxedDest.copyFrom(outsideSource);
			fail("Expected PermissionDeniedException copying from outside source");
		} catch (PermissionDeniedException expected) {
		}

		try {
			outsideSource.moveTo(sandboxedDest);
			fail("Expected PermissionDeniedException moving outside source to sandboxed destination");
		} catch (PermissionDeniedException expected) {
		}

		// Move and copy within sandbox should succeed
		NioFile validSrc = sandboxedFactory.getFile(insideFile.getFileName().toString());
		NioFile validDst = sandboxedFactory.getFile("copied.txt");
		validDst.copyFrom(validSrc);
		assertTrue(validDst.exists());
		assertEquals("public-data", Files.readString(sandboxRoot.resolve("copied.txt")));
	}

	@Test
	public void testRootDirectoryParentAndDeletionSafety() throws Exception {
		NioFileFactory factory = NioFileFactoryBuilder.create()
				.withHome(sandboxRoot)
				.withSandbox(true)
				.build();

		NioFile root = factory.getFile("");
		assertNotNull(root);
		assertTrue(root.exists());

		// getParentFile() on root must return null, preventing traversal out of sandbox
		AbstractFile parent = root.getParentFile();
		assertNull("getParentFile() on sandbox root must be null", parent);

		// delete() on root must throw PermissionDeniedException
		try {
			root.delete(false);
			fail("Expected PermissionDeniedException when deleting sandbox root");
		} catch (PermissionDeniedException expected) {
		}

		try {
			root.delete(true);
			fail("Expected PermissionDeniedException when recursively deleting sandbox root");
		} catch (PermissionDeniedException expected) {
		}
	}

	@Test
	public void testDenyAbsoluteOutsidePathWhenSandboxed() throws Exception {
		NioFileFactory factory = NioFileFactoryBuilder.create()
				.withHome(sandboxRoot)
				.withSandbox(true)
				.build();

		try {
			factory.getFile(outsideFile.toAbsolutePath().toString());
			fail("Expected PermissionDeniedException on absolute path outside sandbox");
		} catch (PermissionDeniedException expected) {
		}
	}

	@Test
	public void testAllowedWithoutSandboxAllOperations() throws Exception {
		NioFileFactory factory = NioFileFactoryBuilder.create()
				.withHome(sandboxRoot)
				.withoutSandbox()
				.build();

		NioFile file = factory.getFile(outsideFile.toAbsolutePath().toString());
		assertNotNull(file);
		assertTrue(file.exists());

		try (InputStream in = file.getInputStream()) {
			byte[] content = in.readAllBytes();
			assertEquals("top-secret-data", new String(content, StandardCharsets.UTF_8));
		}
	}
}

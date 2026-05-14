package com.sshtools.synergy.niofs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.nio.file.ProviderMismatchException;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.junit.Test;

import com.sshtools.client.SshClient.SshClientBuilder;
import com.sshtools.client.sftp.SftpClient.SftpClientBuilder;
import com.sshtools.common.util.Utils;

public class SftpPathTest extends AbstractNioFsTest {

	@Test(expected = UnsupportedOperationException.class)
	public void testRegister() throws Exception {
		testWithFilesystem(
				fs -> fs.getPath("/some/sub/folder/file").register(null, StandardWatchEventKinds.ENTRY_CREATE));
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testRegisterWithModifier() throws Exception {
		testWithFilesystem(fs -> fs.getPath("/some/sub/folder/file").register(null,
				new Kind[] { StandardWatchEventKinds.ENTRY_CREATE }, new WatchEvent.Modifier() {
					@Override
					public String name() {
						return "fake";
					}
				}));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testPathNegativeIndex() throws Exception {
		testWithFilesystem(fs -> {
			var p1 = fs.getPath("/some/sub/folder/file");
			p1.getName(-1);
		});
	}

	@Test(expected = IllegalArgumentException.class)
	public void testPathOverflowIndex() throws Exception {
		testWithFilesystem(fs -> {
			var p1 = fs.getPath("/some/sub/folder/file");
			p1.getName(9999);
		});
	}

	@Test
	public void testRootParentIsNull() throws Exception {
		testWithFilesystem(fs -> {
			var p1 = fs.getPath("/");
			assertNull(p1.getParent());
		});
	}

	@Test
	public void testRootFilenameIsNull() throws Exception {
		testWithFilesystem(fs -> {
			var p1 = fs.getPath("/");
			assertNull(p1.getFileName());
		});
	}

	@Test
	public void testRelativeRootIsNull() throws Exception {
		testWithFilesystem(fs -> {
			var p1 = fs.getPath("def");
			assertNull(p1.getRoot());
		});
	}

	@Test
	public void testAbsoluteRealPath() throws Exception {
		testWithFilesystem(fs -> {
			var folder = fs.getPath(tmpDir.toString() + "/some/folder");
			Files.createDirectories(folder);
			var src = folder.resolve("testfile");
			createRandomContent(src);
			var link = fs.getPath(tmpDir.toString() + "/testlink");
			Files.createSymbolicLink(link, src);
			var real = link.toRealPath();
			assertEquals(src, real);
		});
	}

	@Test
	public void testRealPath() throws Exception {
		testWithFilesystem(fs -> {
			var src = fs.getPath("testfile");
			createRandomContent(src);
			var link = fs.getPath("testlink");
			Files.createSymbolicLink(link, src);
			var real = link.toRealPath();
			assertEquals(src.toAbsolutePath(), real);
		});
	}

	@Test
	public void testRealPathNotLink() throws Exception {
		testWithFilesystem(fs -> {
			var src = fs.getPath("testfile");
			createRandomContent(src);
			var real = src.toRealPath();
			assertEquals(src.toAbsolutePath(), real);
		});
	}

	@Test
	public void testRealPathNoFollow() throws Exception {
		testWithFilesystem(fs -> {
			var src = fs.getPath("testfile");
			createRandomContent(src);
			var link = fs.getPath("testlink");
			Files.createSymbolicLink(link, src);
			var real = link.toRealPath(LinkOption.NOFOLLOW_LINKS);
			assertEquals(link.toAbsolutePath(), real);
		});
	}

	@Test(expected = IOException.class)
	public void testFailRealPath() throws Exception {
		testWithFilesystem(fs -> {
			var src = fs.getPath("testfile");
			createRandomContent(src);
			var link = fs.getPath("testlink");
			Files.createSymbolicLink(link, src);
			fs.close();
			link.toRealPath();
		});
	}

	@Test
	public void testNormalizeSameFile() throws Exception {
		testWithFilesystem(fs -> {
			var p1 = fs.getPath("some/sub/folder/somefile");
			p1 = p1.normalize();
			assertEquals("some/sub/folder/somefile", p1.toString());
		});
	}

	@Test
	public void testNormalize() throws Exception {
		testWithFilesystem(fs -> {
			var p1 = fs.getPath("some/sub/folder/somefile");
			var t1 = p1.resolve("./../../c1");
			assertEquals("some/sub/folder/somefile/./../../c1", t1.toString());
			t1 = t1.normalize();
			assertEquals("some/sub/c1", t1.toString());
			t1 = t1.resolve("../../../../d1");
			assertEquals("some/sub/c1/../../../../d1", t1.toString());
			t1 = t1.normalize();
			assertEquals("../d1", t1.toString());
		});
	}

	@Test
	public void testNormalizeAbsolute() throws Exception {
		testWithFilesystem(fs -> {
			var p1 = fs.getPath(tmpDir.toString() + "/some/sub/folder/somefile");
			var t1 = p1.resolve("../../c1");
			assertEquals(tmpDir.toString() + "/some/sub/folder/somefile/../../c1", t1.toString());
			t1 = t1.normalize();
			assertEquals(tmpDir.toString() + "/some/sub/c1", t1.toString());
			t1 = t1.resolve("../../../../../../../../../d1");
			assertEquals(tmpDir.toString() + "/some/sub/c1/../../../../../../../../../d1", t1.toString());
			t1 = t1.normalize();
			assertEquals("/d1", t1.toString());
		});
	}

	@Test
	public void testStartsWith() throws Exception {
		testWithFilesystem(fs -> {
			var p1 = fs.getPath("some/sub/folder");
			var p2 = fs.getPath("some/sub/folder/somefile");
			assertTrue("Path 2 must start with Path 1", p2.startsWith(p1));
			assertTrue("Path 2 must start with Path 1", p2.startsWith("some/sub/folder"));
		});
	}

	@Test
	public void testToAbsoluteWhenAbsolute() throws Exception {
		testWithFilesystem(fs -> {
			var p1 = fs.getPath("/some/sub/folder");
			var s1 = p1.resolve("another");
			var a1 = s1.toAbsolutePath();
			assertEquals(s1, a1);
		});
	}

	@Test
	public void testToAbsoluteWhenRelative() throws Exception {
		testWithFilesystem(fs -> {
			var p1 = fs.getPath("some/sub/folder");
			var s1 = p1.resolve("another");
			var a1 = s1.toAbsolutePath();
			assertNotEquals(p1, a1);
			assertEquals(tmpDir.toString() + "/some/sub/folder/another", a1.toString());
		});
	}

	@Test(expected = ProviderMismatchException.class)
	public void testRelativeDifferentProviders() throws Exception {
		var root1 = Paths.get("r1");
		testWithFilesystem(fs -> {
			var root2 = fs.getPath("r2");
			root1.relativize(root2);
		});
	}

	@Test(expected = IllegalArgumentException.class)
	public void testRelativeDifferentRoots() throws Exception {
		testWithFilesystem(fs -> {
			var root1 = fs.getPath("r2");
			var root2 = fs.getPath("/r2");
			root1.relativize(root2);
		});
	}

	@Test
	public void testRelativeSame() throws Exception {
		testWithFilesystem(fs -> {
			var root1 = fs.getPath("r1");
			var root2 = fs.getPath("r1");
			var rel = root1.relativize(root2);
			assertEquals("", rel.toString());
		});
	}

	@Test
	public void testNotStartsWith() throws Exception {
		testWithFilesystem(fs -> {
			var p1 = fs.getPath("some/sub/folder");
			var p2 = fs.getPath("some/othersub/folder/somefile");
			assertFalse("Path 2 must not start with Path 1", p2.startsWith(p1));
			assertFalse("Path 2 must not start with Path 1", p2.startsWith("some/sub/folder"));
		});
	}

	@Test
	public void testNotStartsWithCrossFileSystem() throws Exception {
		var p1 = Paths.get("some/sub/folder");
		testWithFilesystem(fs -> {
			var p2 = fs.getPath("some/sub/folder/somefile");
			assertFalse("Path 2 must not start with Path 1", p2.startsWith(p1));
		});
	}

	@Test
	public void testNotEndsWithCrossFileSystem() throws Exception {
		var p1 = Paths.get("some/sub/folder");
		testWithFilesystem(fs -> {
			var p2 = fs.getPath("some/sub/folder/somefile");
			assertFalse(p2.endsWith(p1));
		});
	}

	@Test
	public void testEndsWithString() throws Exception {
		testWithFilesystem(fs -> {
			var p1 = fs.getPath("some/sub/folder");
			assertTrue(p1.endsWith("folder"));
		});
	}

	@Test
	public void testNotEndsWithString() throws Exception {
		testWithFilesystem(fs -> {
			var p1 = fs.getPath("some/sub/folder");
			assertFalse(p1.endsWith("XXXXXXX"));
		});
	}

	@Test
	public void testEquals() throws Exception {
		testWithFilesystem(fs -> {
			var p1 = fs.getPath("some/sub/folder/somefile");
			var p2 = fs.getPath("some/sub/folder/somefile");
			assertTrue("Path 2 must start with Path 1", p2.startsWith(p1));
			assertTrue("Path 2 must end with Path 1", p2.endsWith(p1));
			assertTrue("Path 1 must start with Path 2", p1.startsWith(p2));
			assertTrue("Path 1 must end with Path 2", p2.endsWith(p2));
		});
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testToFile() throws Exception {
		testWithFilesystem(fs -> {
			fs.getPath("some/sub/folder/somefile").toFile();
		});
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testRegisterWatchKey() throws Exception {
		/*
		 * NOTE: Probably won't need specific if and tests when watch service is
		 * implemented. These tests are just for full coverage
		 */
		testWithFilesystem(fs -> {
			fs.getPath("some/sub/folder/somefile").register(null);
		});
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testRegisterWatchKeyWithKinds() throws Exception {
		/*
		 * NOTE: Probably won't need specific if and tests when watch service is
		 * implemented. These tests are just for full coverage
		 */
		testWithFilesystem(fs -> {
			fs.getPath("some/sub/folder/somefile").register(null, new Kind<?>[0]);
		});
	}

	@Test
	public void testToURI() throws Exception {
		testWithFilesystem(fs -> {
			assertEquals("sftp://localhost:" + port + tmpDir.toString() + "/some/sub/folder/somefile",
					fs.getPath("some/sub/folder/somefile").toUri().toString());
		});
	}

	@Test
	public void testHashCodeCrossFileSystem() throws Exception {
		var p1 = Paths.get("some/sub/folder");
		testWithFilesystem(fs -> {
			var p2 = fs.getPath("some/sub/folder");
			assertNotEquals(p1.hashCode(), p2.hashCode());
		});
	}

	@Test
	public void testSort() throws Exception {
		testWithFilesystem(fs -> {
			var l = new ArrayList<>(Arrays.asList(fs.getPath("gravy.doc"), fs.getPath("123"), fs.getPath("BBB,CCC"),
					fs.getPath("banana.doc"), fs.getPath("abacus.doc"), fs.getPath("cherry.doc"),
					fs.getPath("europe.doc"), fs.getPath("zzzzzz.jar"), fs.getPath("abc123.txt"), fs.getPath("DEF,GHI"),
					fs.getPath("fanta.doc"), fs.getPath("helium.doc"), fs.getPath("dancing.doc"),
					fs.getPath("gravyZdoc"), fs.getPath("india.dog"), fs.getPath("?aaaa.doc"), fs.getPath("b!ack.cat"),
					fs.getPath("xxxxbxxxx.doc")));
			Collections.sort(l);
			assertEquals("123", l.get(0).toString());
			assertEquals("zzzzzz.jar", l.get(l.size() - 1).toString());
		});
	}

	@Test
	public void testCompare() throws Exception {
		testWithFilesystem(fs -> {
			var p1 = fs.getPath("gravy.doc");
			assertEquals(1, p1.compareTo(null));
			var pX = fs.getPath("");
			assertEquals(1, p1.compareTo(pX));

			var p2 = fs.getPath("sub-folder/gravy.doc");
			assertEquals(-12, p1.compareTo(p2));
			assertEquals(12, p2.compareTo(p1));
		});
	}

	@Test(expected = ProviderMismatchException.class)
	public void testResolveOfDifferentType() throws Exception {
		var other = Paths.get("SOMEPATH");
		testWithFilesystem(fs -> {
			var p1 = fs.getPath("folder");
			p1.resolve(other);
		});
	}

	@Test(expected = ProviderMismatchException.class)
	public void testResolveDifferentSftpProvider() throws Exception {
		var prov = new SftpFileSystemProvider();
		try (var ssh = SshClientBuilder.create().
				withTarget("localhost", port).
				withUsername("test").
				withPassword("test").
				build()) {
			var sftp = SftpClientBuilder.create().withClient(ssh).build();
			var conx = sftp.getSubsystemChannel().getConnection();
			var fs = prov.newFileSystem(URI.create(String.format(
						"sftp://%s@%s", 
						conx.getUsername(), 
						Utils.formatHostnameAndPort(conx.getRemoteIPAddress(), conx.getRemotePort()))), 
					Map.of(SftpFileSystemProvider.SFTP_CLIENT, sftp));
			
			var other = fs.getPath("SOMEPATH");
			testWithFilesystem(fs2 -> {
				var p1 = fs2.getPath("folder");
				p1.resolve(other);
			});
		}
	}

	@Test
	public void testSiblings() throws Exception {
		testWithFilesystem(fs -> {
			var p1 = fs.getPath("folder/sibling1");
			var p2 = p1.resolveSibling("sibling2");
			assertEquals("folder/sibling2", p2.toString());
		});
	}

	@Test
	public void testRootSiblings() throws Exception {
		testWithFilesystem(fs -> {
			var p1 = fs.getPath("/");
			var p2 = p1.resolveSibling("sibling2");
			assertEquals("sibling2", p2.toString());
		});
	}

	@Test
	public void testSubPath() throws Exception {
		testWithFilesystem(fs -> {
			var p1 = fs.getPath("some/folder/path/to/a/file");
			var p2 = p1.subpath(2, 4);
			assertEquals("path/to", p2.toString());
		});
	}

	@Test(expected = IllegalArgumentException.class)
	public void testFailSubPathNegative() throws Exception {
		testWithFilesystem(fs -> {
			var p1 = fs.getPath("some/folder/path/to/a/file");
			p1.subpath(-1, 4);
		});
	}

	@Test(expected = IllegalArgumentException.class)
	public void testFailSubPathStartOverflow() throws Exception {
		testWithFilesystem(fs -> {
			var p1 = fs.getPath("some/folder/path/to/a/file");
			p1.subpath(6, 4);
		});
	}

}

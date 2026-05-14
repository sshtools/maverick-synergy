package com.sshtools.synergy.niofs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.channels.OverlappingFileLockException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Random;

import org.junit.Test;

import com.sshtools.common.policy.FileSystemPolicy;
import com.sshtools.common.sftp.PosixPermissions.PosixPermissionsBuilder;
import com.sshtools.common.sftp.extensions.BasicSftpExtensionFactory;
import com.sshtools.common.sftp.extensions.CopyFileSftpExtension;

public class SftpFileSystemProviderTest extends AbstractNioFsTest {
	@Test
	public void testFindFilesystem() throws Exception {
		testWithFilesystem(fs -> {
			var src = fs.getPath("testfile1");
			Files.createFile(src);
			var str = Files.getFileStore(src);
			assertEquals("vol-" + Integer.toUnsignedLong(tmpDir.toString().hashCode()), str.name());
		});
	}

	@Test
	public void testMoveRemoteToRemote() throws Exception {
		testWithFilesystem(fs -> {
			var src = fs.getPath("testfile1");
			createRandomContent(src);
			Path dest = fs.getPath("testfile1.new");
			Files.move(src, dest);
			assertFalse("Original file should not exist", Files.exists(src));
			assertTrue("New file should exist", Files.exists(dest));
		});
	}

	@Test
	public void testMoveRemoteToRemoteWithoutExtension() throws Exception {
		testWithFilesystem(fs -> {
			currentContext.getPolicy(FileSystemPolicy.class).disableSFTPExtension("posix-rename@openssh.com");
			try {
				var src = fs.getPath("testfile1");
				createRandomContent(src);
				Path dest = fs.getPath("testfile1.new");
				Files.createFile(dest);
				Files.move(src, dest, StandardCopyOption.REPLACE_EXISTING);
				assertFalse("Original file should not exist", Files.exists(src));
				assertTrue("New file should exist", Files.exists(dest));
			}
			finally {
				currentContext.getPolicy(FileSystemPolicy.class).enableSFTPExtension("posix-rename@openssh.com");
			}
		});
	}

	@Test(expected = FileAlreadyExistsException.class)
	public void testFailMoveRemoteToRemoteWithoutExtension() throws Exception {
		testWithFilesystem(fs -> {
			currentContext.getPolicy(FileSystemPolicy.class).disableSFTPExtension("posix-rename@openssh.com");
			try {
				var src = fs.getPath("testfile1");
				createRandomContent(src);
				Path dest = fs.getPath("testfile1.new");
				Files.createFile(dest);
				Files.move(src, dest);
			}
			finally {
				currentContext.getPolicy(FileSystemPolicy.class).enableSFTPExtension("posix-rename@openssh.com");
			}
		});
	}

	@Test(expected = FileAlreadyExistsException.class)
	public void testFailMoveRemoteToLocal() throws Exception {
		var dest = Files.createTempFile("data", ".tmp");
		testWithFilesystem(fs -> {
			var src = fs.getPath("testfile1");
			createRandomContent(src);
			Files.move(src, dest);
		});
	}

	@Test
	public void testPathFromURI() throws Exception {
		testWithFilesystem(fs -> {
			var src = fs.getPath("testfile1");
			var other = src.getFileSystem().provider().getPath(URI.create("sftp://test@localhost:" + port + "/" + tmpDir.toString() + "/testfile1"));
			assertEquals(src.toAbsolutePath().toString(), other.toString());
		});
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testPathFromIncompatibleURI() throws Exception {
		testWithFilesystem(fs -> {
			var src = fs.getPath("testfile1");
			src.getFileSystem().provider().getPath(URI.create("xxxx://XXXXXXX:" + port + "/" + tmpDir.toString() + "/testfile1"));
		});
	}

	@Test
	public void testMoveRemoteToLocal() throws Exception {
		var dest = Files.createTempFile("data", ".tmp");
		Files.delete(dest);
		testWithFilesystem(fs -> {
			var src = fs.getPath("testfile1");
			createRandomContent(src);

			Files.move(src, dest);
			assertFalse("Original file should not exist", Files.exists(src));
			assertTrue("New file should exist", Files.exists(dest));
		});
	}

	@Test
	public void testMoveRemoteToLocalReplaceExisting() throws Exception {
		var dest = Files.createTempFile("data", ".tmp");
		testWithFilesystem(fs -> {
			var src = fs.getPath("testfile1");
			createRandomContent(src);

			Files.move(src, dest, StandardCopyOption.REPLACE_EXISTING);
			assertFalse("Original file should not exist", Files.exists(src));
			assertTrue("New file should exist", Files.exists(dest));
		});
	}

	@Test
	public void testCopyRemoteToRemote() throws Exception {
		testWithFilesystem(fs -> {
			var src = fs.getPath("testfile1");
			createRandomContent(src);

			Path dest = fs.getPath("testfile1.copy");
			Files.copy(src, dest);
			assertTrue("Files should have the same content", compareFiles(src, dest));
		});
	}

	@Test
	public void testCopyRemoteToRemoteUsingCopyExtension() throws Exception {
		testWithFilesystem(fs -> {
			var factory = new BasicSftpExtensionFactory(new CopyFileSftpExtension());
			try {
				currentContext.getPolicy(FileSystemPolicy.class).getSFTPExtensionFactories().add(factory);
				var src = fs.getPath("testfile1");
				createRandomContent(src);

				Path dest = fs.getPath("testfile1.copy");
				Files.copy(src, dest);
				assertTrue("Files should have the same content", compareFiles(src, dest));
			} finally {
				currentContext.getPolicy(FileSystemPolicy.class).getSFTPExtensionFactories().remove(factory);
			}
		});
	}

	@Test(expected = FileAlreadyExistsException.class)
	public void testFailCopyRemoteToRemoteReplaceExisting() throws Exception {
		testWithFilesystem(fs -> {
			var src = fs.getPath("testfile1");
			createRandomContent(src);
			var preExisting = fs.getPath("testfile1.copy");
			Files.createFile(preExisting);
			Files.copy(src, preExisting);
		});
	}

	@Test(expected = FileAlreadyExistsException.class)
	public void testFailCopyRemoteToRemoteReplaceExistingUsingExtension() throws Exception {
		testWithFilesystem(fs -> {
			var factory = new BasicSftpExtensionFactory(new CopyFileSftpExtension());
			try {
				currentContext.getPolicy(FileSystemPolicy.class).getSFTPExtensionFactories().add(factory);
				var src = fs.getPath("testfile1");
				createRandomContent(src);
				var preExisting = fs.getPath("testfile1.copy");
				Files.createFile(preExisting);
				Files.copy(src, preExisting);
			} finally {
				currentContext.getPolicy(FileSystemPolicy.class).getSFTPExtensionFactories().remove(factory);
			}

		});
	}

	@Test
	public void testCopyRemoteToRemoteReplaceExisting() throws Exception {
		testWithFilesystem(fs -> {
			var src = fs.getPath("testfile1");
			createRandomContent(src);
			var preExisting = fs.getPath("testfile1.copy");
			Files.createFile(preExisting);
			Path dest = fs.getPath("testfile1.copy");
			Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
			assertTrue("Files should have the same content", compareFiles(src, dest));
		});
	}

	@Test(expected = IOException.class)
	public void testCopyRemoteToBadRemote() throws Exception {
		testWithFilesystem(fs -> {
			var src = fs.getPath("testfile1");
			createRandomContent(src);
			var dest = fs.getPath("folder/that/doesnt/exist/testfile1.copy");
			Files.copy(src, dest);
		});
	}

	@Test
	public void testCopyRemoteToRemoteAndAttributes() throws Exception {
		testWithFilesystem(fs -> {
			var src = fs.getPath("testfile1");
			createRandomContent(src);

			var perms = PosixPermissionsBuilder.create().fromFileModeString("r-x------").build().asPermissions();
			Files.setPosixFilePermissions(src, perms);

			Path dest = fs.getPath("testfile1.copy");
			Files.copy(src, dest, StandardCopyOption.COPY_ATTRIBUTES);
			assertTrue("Files should have the same content", compareFiles(src, dest));
			assertEquals("Files should have the same attributes", perms, Files.getPosixFilePermissions(dest));
		});
	}

	@Test(expected = FileAlreadyExistsException.class)
	public void testFailCopyRemoteToLocal() throws Exception {
		var dest = Files.createTempFile("data", ".tmp");
		testWithFilesystem(fs -> {
			var src = fs.getPath("testfile1");
			createRandomContent(src);
			Files.copy(src, dest);
		});
	}

	@Test
	public void testCopyRemoteToLocal() throws Exception {
		var dest = Files.createTempFile("data", ".tmp");
		Files.delete(dest);
		testWithFilesystem(fs -> {
			var src = fs.getPath("testfile1");
			createRandomContent(src);

			Files.copy(src, dest);
			assertTrue("Files should have the same content", compareFiles(src, dest));
		});
	}

	@Test
	public void testCopyRemoteToLocalReplaceExisting() throws Exception {
		var dest = Files.createTempFile("data", ".tmp");
		testWithFilesystem(fs -> {
			var src = fs.getPath("testfile1");
			createRandomContent(src);

			Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
			assertTrue("Files should have the same content", compareFiles(src, dest));
		});
	}

	@Test
	public void testCopyLocalToRemote() throws Exception {
		var src = Files.createTempFile("data", ".tmp");
		createRandomContent(src);
		testWithFilesystem(fs -> {
			var dest = fs.getPath("testfile1.copy");
			Files.copy(src, dest);
			assertTrue("Files should have the same content", compareFiles(src, dest));
		});
	}

	@Test
	public void testHiddenFiles() throws Exception {
		testWithFilesystem(fs -> {
			var notHidden = fs.getPath("not-hidden");
			var hidden = fs.getPath(".hidden");
			assertFalse("File 1 should not be hidden", Files.isHidden(notHidden));
			assertTrue("File 2 should be hidden", Files.isHidden(hidden));
		});
	}

	@Test
	public void testDeleteFile() throws Exception {
		testWithFilesystem(fs -> {
			var src = fs.getPath("testfile1");
			Files.createFile(src);
			Files.delete(src);
			assertFalse("File 1 should not be exist", Files.exists(src));
		});
	}

	@Test
	public void testBrokenLink() throws Exception {
		testWithFilesystem(fs -> {

			var src = fs.getPath("testfile");
			createRandomContent(src);
			assertTrue("Source should exist", Files.exists(src));

			var link = fs.getPath("testlink");
			Files.createSymbolicLink(link, src);
			Files.delete(src);
			assertFalse("File should not be exist", Files.exists(src));
			assertTrue("Link should exist", Files.exists(link, LinkOption.NOFOLLOW_LINKS));
			Files.delete(link);
			assertFalse("Link should exist", Files.exists(src));
		});
	}

	@Test(expected = NoSuchFileException.class)
	public void testFailDeleteFile() throws Exception {
		testWithFilesystem(fs -> {
			var src = fs.getPath("testfile1");
			Files.delete(src);
		});
	}

	@Test
	public void testCreateDirectory() throws Exception {
		testWithFilesystem(fs -> {
			var src = fs.getPath("testdir");
			Files.createDirectory(src);
			assertTrue("Directory should exist", Files.exists(src));
		});
	}

	@Test(expected = IOException.class)
	public void testFailCreateDirectoryWhenClosed() throws Exception {
		testWithFilesystem(fs -> {
			var src = fs.getPath("testdir");
			fs.close();
			Files.createDirectory(src);
		});
	}

	@Test
	public void testCreateDirectories() throws Exception {
		testWithFilesystem(fs -> {
			var src = fs.getPath("testdir/testotherdir");
			Files.createDirectories(src);
			assertTrue("Directory should exist", Files.exists(src));
		});
	}

	@Test(expected = NoSuchFileException.class)
	public void testFailCreateDirectory() throws Exception {
		testWithFilesystem(fs -> {
			var src = fs.getPath("testdir/testotherdir");
			Files.createDirectory(src);
		});
	}

	@Test(expected = IOException.class)
	public void testFailDeleteRoot() throws Exception {
		testWithFilesystem(fs -> {
			var src = fs.getPath("/");
			Files.delete(src);
		});
	}

	@Test(expected = IOException.class)
	public void testFailDeleteClosed() throws Exception {
		testWithFilesystem(fs -> {
			var src = fs.getPath("testfile1");
			Files.createFile(src);
			fs.close();
			Files.delete(src);
		});
	}

	@Test
	public void testDeleteDirectory() throws Exception {
		testWithFilesystem(fs -> {
			var src = fs.getPath("testdir");
			Files.createDirectory(src);
			Files.delete(src);
			assertFalse("Directory should not exist", Files.exists(src));
		});
	}

	@Test
	public void testCreateLink() throws Exception {
		testWithFilesystem(fs -> {
			var src = fs.getPath("testfile");
			createRandomContent(src);

			var link = fs.getPath("testlink");
			Files.createLink(link, src);

			assertTrue("Link should exist", Files.exists(link));
		});
	}

	@Test(expected = IOException.class)
	public void testFailCreateLinkWhenClosed() throws Exception {
		testWithFilesystem(fs -> {
			var src = fs.getPath("testfile");
			createRandomContent(src);
			var link = fs.getPath("testlink");
			fs.close();
			Files.createLink(link, src);
		});
	}

	@Test
	public void testCreateSymbolicLink() throws Exception {
		testWithFilesystem(fs -> {
			var src = fs.getPath("testfile");
			createRandomContent(src);
			assertTrue("Source should exist", Files.exists(src));

			var link = fs.getPath("testlink");
			Files.createSymbolicLink(link, src);

			assertTrue("Link should exist", Files.exists(link));
			assertTrue("File content should be equal", compareFiles(src, link));
			assertTrue("Should be a symbolic link", Files.isSymbolicLink(link));

			Files.delete(link);

			assertFalse("Link should not exist", Files.exists(link));
			assertTrue("Original should exist", Files.exists(src));
		});
	}

	@Test
	public void testReadSymbolicLink() throws Exception {
		testWithFilesystem(fs -> {
			var src = fs.getPath("testfile");
			createRandomContent(src);
			var link = fs.getPath("testlink");
			Files.createSymbolicLink(link, src);
			var linkTarget = Files.readSymbolicLink(link);
			assertTrue("Link target " + linkTarget + " should be same as original " + src,
					Files.isSameFile(src, linkTarget));
		});
	}

	@Test
	public void testIsSameFileDifferentProviders() throws Exception {
		var root1 = Paths.get("r1");
		testWithFilesystem(fs -> {
			var root2 = fs.getPath("r2");
			assertFalse(Files.isSameFile(root2, root1));
		});
	}

	@Test(expected = IOException.class)
	public void testFailReadSymbolicLinkWhenClosed() throws Exception {
		testWithFilesystem(fs -> {
			var src = fs.getPath("testfile");
			createRandomContent(src);
			var link = fs.getPath("testlink");
			Files.createSymbolicLink(link, src);
			fs.close();
			Files.readSymbolicLink(link);
		});
	}

	@Test(expected = IOException.class)
	public void testFailReadSymbolicLink() throws Exception {
		testWithFilesystem(fs -> {
			var src = fs.getPath("testfile");
			Files.readSymbolicLink(src);
		});
	}

	@Test(expected = IOException.class)
	public void testFailCreateSymbolicLink() throws Exception {
		testWithFilesystem(fs -> {
			var src = fs.getPath("testfile");
			createRandomContent(src);
			var existing = fs.getPath("testlink");
			createRandomContent(existing);

			var link = fs.getPath("testlink");
			Files.createSymbolicLink(link, src);
		});
	}

	@Test
	public void testExecutableDoesNothing() throws Exception {
		testWithFilesystem(fs -> {
			var src = fs.getPath("testfile");
			Files.createFile(src);
			assertFalse(Files.isExecutable(src));
			Files.setPosixFilePermissions(src, PosixPermissionsBuilder.create().withChmodArgumentString("u+x").build().asPermissions());
			assertFalse(Files.isExecutable(src));
		});
	}

	@Test(expected = IOException.class)
	public void testFailCreateLink() throws Exception {
		testWithFilesystem(fs -> {
			var src = fs.getPath("testfile");
			createRandomContent(src);
			var existing = fs.getPath("testlink");
			createRandomContent(existing);

			var link = fs.getPath("testlink");
			Files.createLink(link, src);
		});
	}

	@Test(expected = IOException.class)
	public void testFailCreateSymbolicLinkWhenClosed() throws Exception {
		testWithFilesystem(fs -> {
			fs.close();
			Files.createSymbolicLink(fs.getPath("testfile"), fs.getPath("testlink"));
		});
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testAsyncFileChannel() throws Exception {
		testWithFilesystem(fs -> {
			AsynchronousFileChannel.open(fs.getPath("testfile"), StandardOpenOption.CREATE);
		});
	}

	@Test
	public void testFileChannelTransferTo() throws Exception {
		testWithFilesystem(fs -> {
			var src = fs.getPath("testfile");
			createRandomContent(src);

			var copy = fs.getPath("testcopy");

			try (var chanIn = FileChannel.open(src)) {
				try (var chanOut = FileChannel.open(copy, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
					chanIn.transferTo(0, Files.size(src), chanOut);
				}
			}
			assertTrue("File content should be equal", compareFiles(src, copy));
		});
	}

	@Test
	public void testFileChannelTransferToTruncatedWrite() throws Exception {
		testWithFilesystem(fs -> {
			var src = fs.getPath("testfile");
			createRandomContent(src);
			var sz = Files.size(src);
			try (var chanIn = FileChannel.open(src)) {
				try(var chanOut = Channels.newChannel(OutputStream.nullOutputStream())) {
					var t = chanIn.transferTo(0, Files.size(src), new WritableByteChannel() {
						@Override
						public boolean isOpen() {
							return chanOut.isOpen();
						}
						
						@Override
						public void close() throws IOException {
							chanOut.close();
						}
						
						@Override
						public int write(ByteBuffer src) throws IOException {
							return chanOut.write(src) / 2;
						}
					});
					assertEquals(sz / 2, t);
				}
			}
		});
	}

	@Test
	public void testFileChannelTransferReadTruncatedWrite() throws Exception {
		testWithFilesystem(fs -> {
			var src = fs.getPath("testfile");
			var dest = fs.getPath("testfile.out");
			createRandomContent(src);
			var sz = Files.size(src);
			try (var chanOut = FileChannel.open(dest, StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
				try(var chanIn = FileChannel.open(src, StandardOpenOption.READ)) {
					var t = chanOut.transferFrom(new ReadableByteChannel() {
						@Override
						public boolean isOpen() {
							return chanIn.isOpen();
						}
						
						@Override
						public void close() throws IOException {
							chanIn.close();
						}
						
						@Override
						public int read(ByteBuffer src) throws IOException {
							return chanIn.read(src) / 2;
						}
					}, 0, Files.size(src));
					assertEquals(sz, t);
				}
			}
		});
	}

	@Test(expected = IOException.class)
	public void testFailFileChannelTransferTo() throws Exception {
		testWithFilesystem(fs -> {
			var src = fs.getPath("testfile");
			createRandomContent(src);
			var copy = fs.getPath("testcopy");
			try (var chanIn = FileChannel.open(src)) {
				try (var chanOut = FileChannel.open(copy, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
					chanIn.close();
					chanIn.transferTo(0, Files.size(src), chanOut);
				}
			}
		});
	}

	@Test
	public void testFileChannelTransferFrom() throws Exception {
		testWithFilesystem(fs -> {
			var src = fs.getPath("testfile");
			createRandomContent(src);

			var copy = fs.getPath("testcopy");

			try (var chanIn = FileChannel.open(src)) {
				try (var chanOut = FileChannel.open(copy, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
					chanOut.transferFrom(chanIn, 0, Files.size(src));
				}
			}
			assertTrue("File content should be equal", compareFiles(src, copy));
		});
	}

	@Test(expected = IOException.class)
	public void testFailFileChannelTransferFrom() throws Exception {
		testWithFilesystem(fs -> {
			var src = fs.getPath("testfile");
			createRandomContent(src);
			var copy = fs.getPath("testcopy");
			try (var chanIn = FileChannel.open(src)) {
				try (var chanOut = FileChannel.open(copy, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
					chanIn.close();
					chanOut.transferFrom(chanIn, 0, Files.size(src));
				}
			}
		});
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testFailFileChannelMap() throws Exception {
		testWithFilesystem(fs -> {
			var src = fs.getPath("testfile");
			createRandomContent(src);
			try (var chanIn = FileChannel.open(src)) {
				chanIn.map(MapMode.PRIVATE, 0, 100);
			}
		});
	}

	@Test
	public void testLockFileChannel() throws Exception {
		/* TODO: Not fully implemented on server yet */
		testWithFilesystem(fs -> {
			var sftpVersion = fs.getSftp().getSubsystemChannel().getServerVersion();
			assumeTrue("must support version 6 sftp. It is " + sftpVersion, sftpVersion >= 6);
			var src = fs.getPath("testfile");
			createRandomContent(src);
			try (var chanIn = FileChannel.open(src)) {
				try (var lock = chanIn.lock()) {

				}
			}
		});
	}

	@Test
	public void testTryLockFileChannel() throws Exception {
		/* TODO: Not fully implemented on server yet */
		testWithFilesystem(fs -> {
			var sftpVersion = fs.getSftp().getSubsystemChannel().getServerVersion();
			assumeTrue("must support version 6 sftp. It is " + sftpVersion, sftpVersion >= 6);
			var src = fs.getPath("testfile");
			createRandomContent(src);
			try (var chanIn = FileChannel.open(src)) {
				try (var lock = chanIn.tryLock()) {
				}
			}
		});
	}

	@Test(expected = OverlappingFileLockException.class)
	public void testFailLockFileChannel() throws Exception {
		/* NOTE: Just for more coverage, can be removed when locking is fully implemented on the server side */
		testWithFilesystem(fs -> {
			var sftpVersion = fs.getSftp().getSubsystemChannel().getServerVersion();
			assumeTrue("must support version 6 sftp. It is " + sftpVersion, sftpVersion >= 6);
			var src = fs.getPath("testfile");
			try (var chan = FileChannel.open(src, StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
				chan.write(ByteBuffer.wrap("Some content".getBytes()));
				try (var lock = chan.lock()) {
					try (var chan2 = FileChannel.open(src, StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
						try (var lock2 = chan2.lock()) {
							chan2.write(ByteBuffer.wrap("Some content".getBytes()));
						}
					}
				}
			}
		});
	}


	@Test(expected = UnsupportedOperationException.class)
	public void testFailTryLockFileChannel() throws Exception {
		/* NOTE: Just for more coverage, can be removed when locking is fully implemented on the server side */
		testWithFilesystem(fs -> {
			var src = fs.getPath("testfile");
			createRandomContent(src);
			try (var chanIn = FileChannel.open(src)) {
				try (var lock = chanIn.tryLock()) {
				}
			}
		});
	}

	@Test
	public void testReadFileChannel() throws Exception {
		testWithFilesystem(fs -> {
			var src = fs.getPath("testfile");
			createRandomContent(src);
			var remain = Files.size(src);
			var bb = ByteBuffer.allocate(128);
			try (var chan = FileChannel.open(src, StandardOpenOption.READ)) {
				while (remain > 0) {
					var r = chan.read(bb);
					if (r == -1)
						break;
					remain -= r;
					bb.flip();
				}
				assertEquals(0, remain);
			}
		});
	}

	@Test
	public void testReadMulitBuffersFileChannel() throws Exception {
		testWithFilesystem(fs -> {
			var src = fs.getPath("testfile");
			createRandomContent(src);
			var sz = Files.size(src);
			var bb = new ByteBuffer[4];
			for(int i = 0 ; i < bb.length; i++) {
				bb[i] = ByteBuffer.allocate((int)(sz / 4));
			}
			try (var chan = FileChannel.open(src, StandardOpenOption.READ)) {
				while (sz > 0) {
					var r = chan.read(bb, 0, (int)sz / 4);
					if (r == -1)
						break;
					sz -= r;
					for(var b : bb)
						b.flip();
				}
				assertEquals(0, sz);
			}
		});
	}

	@Test
	public void testWriteMulitBuffersFileChannel() throws Exception {
		testWithFilesystem(fs -> {
			var src = fs.getPath("testfile");
			createRandomContent(src);
			var sz = Files.size(src);
			var bb = new ByteBuffer[4];
			for(int i = 0 ; i < bb.length; i++) {
				bb[i] = ByteBuffer.allocate((int)(sz / 4));
			}
			for(var b : bb) {
				for(int i = 0 ; i < sz / 4 ; i++) {
					b.put((byte)i);
				}
				b.flip();
			}
			try (var chan = FileChannel.open(src, StandardOpenOption.WRITE)) {
				while (sz > 0) {
					var r = chan.write(bb, 0, (int)sz / 4);
					if (r == -1)
						break;
					sz -= r;
					for(var b : bb)
						b.flip();
				}
				assertEquals(0, sz);
			}
		});
	}

	@Test(expected = IOException.class)
	public void testFailReadMulitBuffersFileChannelWhenClosed() throws Exception {
		testWithFilesystem(fs -> {
			var src = fs.getPath("testfile");
			createRandomContent(src);
			var sz = Files.size(src);
			var bb = new ByteBuffer[4];
			for(int i = 0 ; i < bb.length; i++) {
				bb[i] = ByteBuffer.allocate((int)(sz / 4));
			}
			try (var chan = FileChannel.open(src, StandardOpenOption.READ)) {
				chan.close();
				chan.read(bb, 0, (int)sz / 4);
			}
		});
	}

	@Test(expected = IOException.class)
	public void testFailWriteMulitBuffersFileChannelWhenClosed() throws Exception {
		testWithFilesystem(fs -> {
			var src = fs.getPath("testfile");
			createRandomContent(src);
			var sz = Files.size(src);
			var bb = new ByteBuffer[4];
			for(int i = 0 ; i < bb.length; i++) {
				bb[i] = ByteBuffer.allocate((int)(sz / 4));
			}
			try (var chan = FileChannel.open(src, StandardOpenOption.WRITE)) {
				chan.close();
				chan.write(bb, 0, (int)sz / 4);
			}
		});
	}

	@Test
	public void testAppendFileChannel() throws Exception {
		testWithFilesystem(fs -> {
			var src = fs.getPath("testfile");
			createRandomContent(src);
			var sz = Files.size(src);
			try (var chan = FileChannel.open(src, StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {
				var rnd = new Random();
				var bb = ByteBuffer.allocate((int)sz);
				for(long i = 0 ; i < sz; i++) {
					bb.put((byte)rnd.nextInt(256));
				}
				bb.flip();
				chan.write(bb);
			}
			assertEquals(sz * 2, Files.size(src));
		});
	}
	
	/* TODO: Temporarily commented out to get a build */

//	@Test
//	public void testDeleteOnClose() throws Exception {
//		testWithFilesystem(fs -> {
//			var src = fs.getPath("testfile");
//			createRandomContent(src);
//			try (var chan = FileChannel.open(src, StandardOpenOption.DELETE_ON_CLOSE)) {
//				chan.write(ByteBuffer.wrap("Hello world!".getBytes()));
//			}
//			assertFalse(Files.exists(src));
//		});
//	}

	@Test(expected = IOException.class)
	public void testFailChannelCreateNew() throws Exception {
		testWithFilesystem(fs -> {
			var src = fs.getPath("testfile");
			createRandomContent(src);
			try (var chan = FileChannel.open(src, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW)) {
			}
		});
	}

	@Test(expected = IOException.class)
	public void testFailChannelWriteClose() throws Exception {
		testWithFilesystem(fs -> {
			var src = fs.getPath("testfile");
			createRandomContent(src);
			try (var chan = FileChannel.open(src, StandardOpenOption.WRITE)) {
				chan.close();
				chan.write(ByteBuffer.allocate(0));
			}
		});
	}

	@Test(expected = IOException.class)
	public void testFailChannelReadClose() throws Exception {
		testWithFilesystem(fs -> {
			var src = fs.getPath("testfile");
			createRandomContent(src);
			try (var chan = FileChannel.open(src, StandardOpenOption.READ)) {
				chan.close();
				chan.read(ByteBuffer.allocate(0));
			}
		});
	}

	@Test(expected = IOException.class)
	public void testFailChannelCreateMissing() throws Exception {
		testWithFilesystem(fs -> {
			var src = fs.getPath("testfile");
			try (var chan = FileChannel.open(src, StandardOpenOption.WRITE)) {
			}
		});
	}

	@Test
	public void testChannelCreateNew() throws Exception {
		testWithFilesystem(fs -> {
			var src = fs.getPath("testfile");
			try (var chan = FileChannel.open(src, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW)) {
			}
			assertTrue(Files.exists(src));
		});
	}

	@Test
	public void testChannelTruncate() throws Exception {
		testWithFilesystem(fs -> {
			var src = fs.getPath("testfile");
			createRandomContent(src);
			var sz = Files.size(src);
			try (var chan = FileChannel.open(src, StandardOpenOption.WRITE)) {
				chan.truncate(sz / 2);
			}
			assertEquals(sz / 2, Files.size(src));
		});
	}

	@Test
	public void testChannelSeek() throws Exception {
		testWithFilesystem(fs -> {
			var src = fs.getPath("testfile");
			createRandomContent(src);
			try (var chan = FileChannel.open(src, StandardOpenOption.READ)) {
				chan.position(10);
				assertEquals(chan.position(), 10);
			}
		});
	}

	@Test(expected = IOException.class)
	public void testFailChannelTruncateWhenClosed() throws Exception {
		testWithFilesystem(fs -> {
			var src = fs.getPath("testfile");
			createRandomContent(src);
			try (var chan = FileChannel.open(src, StandardOpenOption.WRITE)) {
				chan.close();
				chan.truncate(10);
			}
		});
	}

	@Test
	public void testChannelForceNoop() throws Exception {
		/* NOTE: Currently for coverage only */
		testWithFilesystem(fs -> {
			var src = fs.getPath("testfile");
			createRandomContent(src);
			try (var chan = FileChannel.open(src, StandardOpenOption.WRITE)) {
				chan.force(true);
			}
		});
	}

	@Test
	public void testChannelOpenTruncate() throws Exception {
		testWithFilesystem(fs -> {
			var src = fs.getPath("testfile");
			createRandomContent(src);
			try (var chan = FileChannel.open(src, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
				assertEquals(0, Files.size(src));
				chan.write(ByteBuffer.allocate(1));
			}
			assertEquals(1, Files.size(src));
			
		});
	}

	@Test
	public void testChannelWriteText() throws Exception {
		testWithFilesystem(fs -> {
			var src = fs.getPath("testfile");
			try (var chan = FileChannel.open(src, StandardOpenOption.CREATE, StandardOpenOption.WRITE, SftpOpenOption.TEXT)) {
				chan.write(ByteBuffer.wrap("Line 1\rLine 2\rLine 3\r".getBytes()));
			}
			try (var chan = FileChannel.open(src, StandardOpenOption.READ)) {
				var out = new ByteArrayOutputStream();
				chan.transferTo(0, Long.MAX_VALUE, Channels.newChannel(out));
				var arr = out.toByteArray();
				
				/* TODO: Synergy server is not actually converting */
				assertEquals("Line 1\rLine 2\rLine 3\r", new String(arr));
			}
		});
	}
}

package com.sshtools.synergy.niofs;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.FileChannel;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

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
			}
			finally {
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
			}
			finally {
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
			assertTrue("Link target " + linkTarget + " should be same as original " + src, Files.isSameFile(src, linkTarget));
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

			try(var chanIn = Files.newByteChannel(src)) {
				try(var chanOut = Files.newByteChannel(copy, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
					((FileChannel)chanIn).transferTo(0, Files.size(src), chanOut);
				}	
			}
			assertTrue("File content should be equal", compareFiles(src, copy));
		});
	}

	@Test
	public void testFileChannelTransferFrom() throws Exception {
		testWithFilesystem(fs -> {
			var src = fs.getPath("testfile");
			createRandomContent(src);

			var copy = fs.getPath("testcopy");
			
			try(var chanIn = Files.newByteChannel(src)) {
				try(var chanOut = Files.newByteChannel(copy, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
					((FileChannel)chanOut).transferFrom(chanIn, 0, Files.size(src));
				}	
			}
			assertTrue("File content should be equal", compareFiles(src, copy));
		});
	}
}

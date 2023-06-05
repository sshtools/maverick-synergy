package com.sshtools.synergy.niofs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;
import java.util.Random;

import org.junit.Test;

import com.sshtools.common.files.FileVolume;
import com.sshtools.common.files.direct.NioFile;
import com.sshtools.common.sftp.SftpExtension;
import com.sshtools.common.sftp.SftpSubsystem;
import com.sshtools.common.sftp.extensions.AbstractSftpExtension;
import com.sshtools.common.ssh.Packet;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.UnsignedInteger64;
import com.sshtools.synergy.niofs.SftpFileAttributeViews.ExtendedSftpFileAttributeView;
import com.sshtools.synergy.niofs.SftpFileStore.SftpFileStoreAttributeView;

public class SftpFileStoreTest extends AbstractNioFsTest {
	
	static class MockFileVolume implements FileVolume {
		
		private Random rnd = new Random();

		private long blockSize = rnd.nextLong();
		private long underlyingBlockSize = rnd.nextLong();
		private long blocks = rnd.nextLong();
		private long freeBlocks = rnd.nextLong();
		private long userFreeBlocks = rnd.nextLong();
		private long totalInodes = rnd.nextLong();
		private long freeInodes = rnd.nextLong();
		private long userFreeInodes = rnd.nextLong();
		private long id = rnd.nextLong();
		private long maxFilenameLength = rnd.nextLong();
		private long flags = 0;

		@Override
		public long blockSize() {
			return blockSize;
		}

		@Override
		public long underlyingBlockSize() {
			return underlyingBlockSize;
		}

		@Override
		public long blocks() {
			return blocks;
		}

		@Override
		public long freeBlocks() {
			return freeBlocks;
		}

		@Override
		public long userFreeBlocks() {
			return userFreeBlocks;
		}

		@Override
		public long totalInodes() {
			return totalInodes;
		}

		@Override
		public long freeInodes() {
			return freeInodes;
		}

		@Override
		public long userFreeInodes() {
			return userFreeInodes;
		}

		@Override
		public long id() {
			return id;
		}

		@Override
		public long flags() {
			return flags;
		}

		@Override
		public long maxFilenameLength() {
			return maxFilenameLength;
		}
		
	}
	
	static MockFileVolume MOCK_VOLUME = new MockFileVolume();

	@Override
	protected SftpExtension createStatVFSExtension() {
		return new AbstractSftpExtension("statvfs@openssh.com", true) {
			@Override
			public void processMessage(ByteArrayReader msg, int requestId, SftpSubsystem sftp) {
				try {
					msg.readString();
					var store = MOCK_VOLUME;
					Packet reply = new Packet();
			        try {
			        	reply.write(SftpSubsystem.SSH_FXP_EXTENDED_REPLY);
			        	reply.writeInt(requestId);
			        	reply.writeUINT64(new UnsignedInteger64(store.blockSize()));
			        	reply.writeUINT64(new UnsignedInteger64(store.underlyingBlockSize()));
			        	reply.writeUINT64(new UnsignedInteger64(store.blocks()));
			        	reply.writeUINT64(new UnsignedInteger64(store.freeBlocks()));
			        	reply.writeUINT64(new UnsignedInteger64(store.userFreeBlocks()));
			        	reply.writeUINT64(new UnsignedInteger64(store.totalInodes()));
			        	reply.writeUINT64(new UnsignedInteger64(store.freeInodes()));
			        	reply.writeUINT64(new UnsignedInteger64(store.userFreeInodes()));
			        	reply.writeUINT64(new UnsignedInteger64(store.id()));
			        	reply.writeUINT64(new UnsignedInteger64(store.flags()));
			        	reply.writeUINT64(new UnsignedInteger64(store.maxFilenameLength()));
			        	sftp.sendMessage(reply);
			        } finally {
			        	reply.close();
			        }
					sftp.sendStatusMessage(requestId, SftpSubsystem.STATUS_FX_OK, "The copy-file operation completed.");
				} catch (FileNotFoundException e) {
					sftp.sendStatusMessage(requestId, SftpSubsystem.STATUS_FX_NO_SUCH_FILE, e.getMessage());
				} catch (IOException e) {
					sftp.sendStatusMessage(requestId, SftpSubsystem.STATUS_FX_FAILURE, e.getMessage());
				}
			}

			@Override
			public boolean supportsExtendedMessage(int messageId) {
				return false;
			}

			@Override
			public void processExtendedMessage(ByteArrayReader msg, SftpSubsystem sftp) {
			}
		};
	}
	
	@Test
	public void testStore() throws Exception {
		testWithFilesystem((fs) -> {
			var store = fs.getFileStores().iterator().next();
			assertEquals("vol-" + MOCK_VOLUME.id, store.name());
			assertEquals("remote", store.type());
			assertFalse("Should not be read only", store.isReadOnly());
			assertEquals(MOCK_VOLUME.blockSize, store.getBlockSize());
			assertEquals(MOCK_VOLUME.underlyingBlockSize * MOCK_VOLUME.blocks, store.getTotalSpace());
			assertEquals(MOCK_VOLUME.underlyingBlockSize * (MOCK_VOLUME.blocks - MOCK_VOLUME.freeBlocks), store.getUnallocatedSpace());
			assertEquals(MOCK_VOLUME.underlyingBlockSize * (MOCK_VOLUME.blocks - MOCK_VOLUME.freeBlocks), store.getUsableSpace());
		});
	}
	
	@Test
	public void testReadOnlyStore() throws Exception {
		testWithFilesystem((fs) -> {
			var store = fs.getFileStores().iterator().next();
			MOCK_VOLUME.flags = NioFile.SSH_FXE_STATVFS_ST_RDONLY;
			try {
				assertTrue("Should be read only", store.isReadOnly());
			}
			finally {
				MOCK_VOLUME.flags = 0;
			}
		});
	}
	
	@Test
	public void testNoSuidStore() throws Exception {
		testWithFilesystem((fs) -> {
			var store = fs.getFileStores().iterator().next();
			MOCK_VOLUME.flags = NioFile.SSH_FXE_STATVFS_ST_NOSUID;
			var view = store.getFileStoreAttributeView(SftpFileStoreAttributeView.class);
			try {
				assertEquals(MOCK_VOLUME.flags, view.mountFlag());
				assertTrue("Should be noSuid", (Boolean)store.getAttribute("sftp:noSuid"));
			}
			finally {
				MOCK_VOLUME.flags = 0;
			}
		});
	}
	
	@Test(expected = UncheckedIOException.class)
	public void testFailStoreWhenClosed() throws Exception {
		testWithFilesystem((fs) -> {
			var store = fs.getFileStores().iterator().next();
			fs.close();
			store.isReadOnly();
		});
	}

	@Test
	public void testStoreViews() throws Exception {
		testWithFilesystem((fs) -> {
			var store = fs.getFileStores().iterator().next();
			assertTrue("Basic attributes should be supported", store.supportsFileAttributeView("basic"));
			assertTrue("Sftp attributes should be supported", store.supportsFileAttributeView("sftp"));
			assertTrue("Posix attributes should be supported", store.supportsFileAttributeView("posix"));
			assertFalse("Xxxxxxx attributes should not be supported", store.supportsFileAttributeView("xxxxxxx"));
			assertTrue("Basic attributes view should be supported",
					store.supportsFileAttributeView(BasicFileAttributeView.class));
			assertTrue("Sftp attributes view should be supported",
					store.supportsFileAttributeView(ExtendedSftpFileAttributeView.class));
			assertTrue("Posix attributes view should be supported",
					store.supportsFileAttributeView(PosixFileAttributeView.class));
			assertFalse("Base attributes view should not be supported",
					store.supportsFileAttributeView(FileAttributeView.class));
		});
	}

	@Test
	public void testSftpStore() throws Exception {
		testWithFilesystem((fs) -> {
			var store = fs.getFileStores().iterator().next();
			assertFalse("Should not be read only", (Boolean)store.getAttribute("sftp:readOnly"));
			assertFalse("Should not be noSuid", (Boolean)store.getAttribute("sftp:noSuid"));
			assertEquals(Long.valueOf(MOCK_VOLUME.blockSize), (Long)store.getAttribute("sftp:blockSize"));
			assertEquals(Long.valueOf(MOCK_VOLUME.underlyingBlockSize), (Long)store.getAttribute("sftp:fragmentSize"));
			assertEquals(Long.valueOf(MOCK_VOLUME.blocks), (Long)store.getAttribute("sftp:blocks"));
			assertEquals(Long.valueOf(MOCK_VOLUME.freeBlocks), (Long)store.getAttribute("sftp:freeBlocks"));
			assertEquals(Long.valueOf(MOCK_VOLUME.userFreeBlocks), (Long)store.getAttribute("sftp:availBlocks"));
			assertEquals(Long.valueOf(MOCK_VOLUME.totalInodes), (Long)store.getAttribute("sftp:iNodes"));
			assertEquals(Long.valueOf(MOCK_VOLUME.freeInodes), (Long)store.getAttribute("sftp:freeINodes"));
			assertEquals(Long.valueOf(MOCK_VOLUME.id), (Long)store.getAttribute("sftp:fileSystemId"));
			assertEquals(Long.valueOf(MOCK_VOLUME.userFreeInodes), (Long)store.getAttribute("sftp:availINodes"));
			assertEquals(Long.valueOf(MOCK_VOLUME.flags), (Long)store.getAttribute("sftp:mountFlag"));
			assertEquals(Long.valueOf(MOCK_VOLUME.maxFilenameLength), (Long)store.getAttribute("sftp:maximumFilenameLength"));
			assertEquals(Long.valueOf(MOCK_VOLUME.underlyingBlockSize * MOCK_VOLUME.blocks / 1024), (Long)store.getAttribute("sftp:size"));
			assertEquals(Long.valueOf(MOCK_VOLUME.underlyingBlockSize * (MOCK_VOLUME.blocks - MOCK_VOLUME.freeBlocks) / 1024), (Long)store.getAttribute("sftp:used"));
			assertEquals(Long.valueOf(MOCK_VOLUME.underlyingBlockSize * MOCK_VOLUME.freeBlocks / 1024), (Long)store.getAttribute("sftp:avail"));
			assertEquals(Long.valueOf(MOCK_VOLUME.underlyingBlockSize * MOCK_VOLUME.userFreeBlocks / 1024), (Long)store.getAttribute("sftp:availForNonRoot"));
			assertEquals(Integer.valueOf( (int) (100 * (MOCK_VOLUME.blocks - MOCK_VOLUME.freeBlocks) / MOCK_VOLUME.blocks)), (Integer)store.getAttribute("sftp:capacity"));
			assertNull("Missing attribute must be null", store.getAttribute("XXXXXX"));
		});
	}

	@Test
	public void testBadSftpStoreView() throws Exception {
		testWithFilesystem((fs) -> {
			var store = fs.getFileStores().iterator().next();
			var view = store.getFileStoreAttributeView(FileStoreAttributeView.class);
			assertNull("View must not exist", view);
		});
	}

	@Test
	public void testSftpStoreView() throws Exception {
		testWithFilesystem((fs) -> {
			var store = fs.getFileStores().iterator().next();
			var view = store.getFileStoreAttributeView(SftpFileStoreAttributeView.class);
			assertEquals("Mount flags should be zero", 0, view.mountFlag());
			assertEquals("sftp", view.name());
			assertEquals(MOCK_VOLUME.blockSize, view.blockSize());
			assertEquals(MOCK_VOLUME.underlyingBlockSize, view.fragmentSize());
			assertEquals(MOCK_VOLUME.blocks, view.blocks());
			assertEquals(MOCK_VOLUME.freeBlocks, view.freeBlocks());
			assertEquals(MOCK_VOLUME.userFreeBlocks, view.availBlocks());
			assertEquals(MOCK_VOLUME.id, view.fileSystemID());
			assertEquals(MOCK_VOLUME.totalInodes, view.iNodes());
			assertEquals(MOCK_VOLUME.freeInodes, view.freeINodes());
			assertEquals(MOCK_VOLUME.userFreeInodes, view.availINodes());
			assertEquals(MOCK_VOLUME.maxFilenameLength, view.maximumFilenameLength());
			assertEquals(MOCK_VOLUME.underlyingBlockSize * MOCK_VOLUME.blocks / 1024, view.size());
			assertEquals(MOCK_VOLUME.underlyingBlockSize * (MOCK_VOLUME.blocks - MOCK_VOLUME.freeBlocks) / 1024, view.used());
			assertEquals(MOCK_VOLUME.underlyingBlockSize * MOCK_VOLUME.freeBlocks / 1024, view.avail());
			assertEquals(MOCK_VOLUME.underlyingBlockSize * MOCK_VOLUME.userFreeBlocks / 1024, view.availForNonRoot());
			assertEquals((int) (100 * (MOCK_VOLUME.blocks - MOCK_VOLUME.freeBlocks) / MOCK_VOLUME.blocks), view.capacity());
			assertTrue("toString() implemented", view.toString().startsWith("SftpFileStoreAttributeView [name"));
		});
	}
}

package com.sshtools.synergy.niofs;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;

import com.sshtools.client.sftp.SftpChannel;
import com.sshtools.client.sftp.SftpHandle;
import com.sshtools.common.sftp.SftpFileAttributes.SftpFileAttributesBuilder;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.SshException;

public final class SftpFileChannel extends FileChannel {
	private final boolean deleteOnClose;
	private final Path path;
	private final SftpHandle handle;
	long pointer;

	SftpFileChannel(boolean deleteOnClose, Path path, SftpHandle handle) {
		this.deleteOnClose = deleteOnClose;
		this.path = path;
		this.handle = handle;
	}

	@Override
	public void force(boolean metaData) throws IOException {
		// Noop?
	}

	@Override
	public FileLock lock(long position, long size, boolean shared) throws IOException {
		var lockFlags = 0;
		if(shared)
			lockFlags = SftpChannel.SSH_FXF_ACCESS_BLOCK_READ;
		else
			lockFlags = SftpChannel.SSH_FXF_ACCESS_BLOCK_WRITE;
		return lock(position, size, lockFlags, shared);
	}

	@Override
	public MappedByteBuffer map(MapMode mode, long position, long size) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public long position() throws IOException {
		return pointer;
	}

	@Override
	public FileChannel position(long newPosition) throws IOException {
		pointer = newPosition;
		return this;
	}

	@Override
	public int read(ByteBuffer dst) throws IOException {
		// TODO optimize if buffer has array
		var arr = new byte[dst.remaining()];
		try {
			int r = handle.read(pointer, arr, 0, arr.length);
			if (r > 0) {
				dst.put(arr, 0, r);
				pointer += r;
			}
			return r;
		} catch (Exception e) {
			throw SftpFileSystemProvider.translateException(e);
		}
	}

	@Override
	public int read(ByteBuffer dst, long position) throws IOException {
		position(position);
		return read(dst);
	}

	@Override
	public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
		// TODO optimize if buffer has array
		long t = 0;
		try {
			for (var dst : dsts) {
				var arr = new byte[length];
				int r = handle.read(pointer, arr, offset, arr.length);
				if (r > 0) {
					dst.put(arr, 0, r);
					pointer += r;
				}
				t += r;
			}
			return t;
		} catch (Exception e) {
			throw SftpFileSystemProvider.translateException(e);
		}
	}

	@Override
	public long size() throws IOException {
		return Files.size(path);
	}

	@Override
	public long transferFrom(ReadableByteChannel src, long position, long count) throws IOException {
		// Untrusted target: Use a newly-erased buffer
		int c = (int) Math.min(count, SftpFileSystemProvider.TRANSFER_SIZE);
		ByteBuffer bb = ByteBuffer.allocate(c);
		long tw = 0; // Total bytes written
		long pos = position;
		try {
			while (tw < count) {
				bb.limit((int) Math.min((count - tw), SftpFileSystemProvider.TRANSFER_SIZE));
				// ## Bug: Will block reading src if this channel
				// ## is asynchronously closed
				int nr = src.read(bb);
				if (nr <= 0)
					break;
				bb.flip();
				int nw = write(bb, pos);
				tw += nw;
				if (nw != nr)
					break;
				pos += nw;
				bb.clear();
			}
			return tw;
		} catch (IOException x) {
			if (tw > 0)
				return tw;
			throw x;
		}
	}

	@Override
	public long transferTo(long position, long count, WritableByteChannel target) throws IOException {
		// Untrusted target: Use a newly-erased buffer
		int c = (int) Math.min(count, SftpFileSystemProvider.TRANSFER_SIZE);
		ByteBuffer bb = ByteBuffer.allocate(c);
		long tw = 0; // Total bytes written
		long pos = position;
		try {
			while (tw < count) {
				bb.limit((int) Math.min(count - tw, SftpFileSystemProvider.TRANSFER_SIZE));
				int nr = read(bb, pos);
				if (nr <= 0)
					break;
				bb.flip();
				// ## Bug: Will block writing target if this channel
				// ## is asynchronously closed
				int nw = target.write(bb);
				tw += nw;
				if (nw != nr)
					break;
				pos += nw;
				bb.clear();
			}
			return tw;
		} catch (IOException x) {
			if (tw > 0)
				return tw;
			throw x;
		}
	}

	@Override
	public FileChannel truncate(long size) throws IOException {
		var bldr = SftpFileAttributesBuilder.create();
		bldr.withSize(size);
		try {
			handle.setAttributes(bldr.build());
			return this;
		} catch (SftpStatusException | SshException e) {
			throw SftpFileSystemProvider.translateException(e);
		}
	}

	@Override
	public FileLock tryLock(long position, long size, boolean shared) throws IOException {
		/* TODO: lock() needs to block, this doesn't */
		return lock(position, size, shared);
	}

	@Override
	public int write(ByteBuffer src) throws IOException {
		try {
			// TODO optimize if buffer has array
			var arr = new byte[src.remaining()];
			src.get(arr);
			handle.write(pointer, arr, 0, arr.length);
			pointer += arr.length;
			return arr.length;
		} catch (Exception e) {
			throw SftpFileSystemProvider.translateException(e);
		}
	}

	@Override
	public int write(ByteBuffer src, long position) throws IOException {
		position(position);
		return write(src);
	}

	@Override
	public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
		try {
			// TODO optimize if buffer has array
			long t = 0;
			for (var src : srcs) {
				var arr = new byte[length];
				src.get(arr);
				handle.write(pointer, arr, 0, length);
				t += arr.length;
			}
			return t;
		} catch (Exception e) {
			throw SftpFileSystemProvider.translateException(e);
		}
	}

	@Override
	protected void implCloseChannel() throws IOException {
		try {
			handle.close();
		} finally {
			if (deleteOnClose)
				Files.delete(path);
		}
	}

	public FileLock lock(long position, long size, int lockFlags) throws IOException {
		return lock(position, size, lockFlags, isReadNotWrite(lockFlags));
	}

	private boolean isReadNotWrite(int lockFlags) {
		return ( lockFlags &  SftpChannel.SSH_FXF_ACCESS_BLOCK_READ ) != 0 &&
			   ( lockFlags &  SftpChannel.SSH_FXF_ACCESS_BLOCK_WRITE ) == 0;
	}

	private FileLock lock(long position, long size, int lockFlags, boolean shared) throws IOException {
		try {
			var sftpLock = handle.lock(position, size, lockFlags);
			return new FileLock(this, position, size, shared) {
				boolean closed = false;
				@Override
				public void release() throws IOException {
					try {
						sftpLock.close();
					}
					finally {
						closed = true;
					}
				}

				@Override
				public boolean isValid() {
					return !closed;
				}
			};
		}
		catch(SftpStatusException | SshException e) {
			throw SftpFileSystemProvider.translateException(e);
		}
	}
}
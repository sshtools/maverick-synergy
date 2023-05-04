package com.sshtools.fuse.fs;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import com.sshtools.client.sftp.SftpChannel;
import com.sshtools.client.sftp.SftpClientTask;
import com.sshtools.client.sftp.SftpFile;
import com.sshtools.common.logger.Log;
import com.sshtools.common.sftp.SftpFileAttributes;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.SshException;

import jnr.ffi.Pointer;
import jnr.ffi.Struct.Signed32;
import jnr.ffi.types.mode_t;
import jnr.ffi.types.off_t;
import jnr.ffi.types.size_t;
import ru.serce.jnrfuse.ErrorCodes;
import ru.serce.jnrfuse.FuseFillDir;
import ru.serce.jnrfuse.FuseStubFS;
import ru.serce.jnrfuse.struct.FileStat;
import ru.serce.jnrfuse.struct.FuseFileInfo;

public class FuseSFTP extends FuseStubFS implements Closeable {

	private static final int MAX_READ_BUFFER_SIZE = 65536;
	private static final int MAX_WRITE_BUFFER_SIZE = 65536;
	private AtomicLong fileHandle = new AtomicLong();
	private Map<Long, SftpFile> handles = new ConcurrentHashMap<>();
	private Map<Long, Integer> flags = new ConcurrentHashMap<>();
	private Map<String, List<Long>> handlesByPath = new ConcurrentHashMap<>();
	private SftpClientTask sftp;
	private int timeout = 10;
	
	ExecutorService executor;
	
	public FuseSFTP(SftpClientTask sftp, ExecutorService executor) throws SftpStatusException, IOException, SshException {
		this.sftp = sftp;
		this.executor = executor;
	}
	
	protected Integer execute(Callable<Integer> task) {
	
		try {
			Future<Integer> result = executor.submit(task);
			return result.get(timeout, TimeUnit.MINUTES);
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			return ErrorCodes.ETIMEDOUT();
		}
	}
	
	@Override
	public int chmod(String path, @mode_t long mode) {
		int ex = exists(path);
		if (ex != -ErrorCodes.EEXIST())
			return ex;
		
		return execute(() -> {
			try {
				sftp.chmod((int) mode, path);
				return 0;
			} catch (SftpStatusException e) {
				Log.error("Failed to chmod {} to {}",e,  path, mode);
				return toErr(e);
			} catch (SshException e) {
				Log.error("Failed to chmod {} to {}", e, path, mode);
				return -ErrorCodes.EFAULT();
			}
		});
	}

	@Override
	public int chown(String path, long uid, long gid) {
		int ex = exists(path);
		if (ex != -ErrorCodes.EEXIST())
			return ex;

		return execute(() -> {
			try {
				sftp.chown(String.valueOf(uid), path);
				sftp.chgrp(String.valueOf(gid), path);
				return 0;
			} catch (SftpStatusException e) {
				Log.error("Failed to chown {} to {}:{}", e, path, uid, gid);
				return toErr(e);
			} catch (SshException e) {
				Log.error("Failed to chmod {} to {}:{}", e, path, uid, gid);
				return -ErrorCodes.EFAULT();
			}
		});
	}

	@Override
	public int create(String path, @mode_t long mode, FuseFileInfo fi) {
		int ex = exists(path);
		if (ex == -ErrorCodes.EEXIST())
			return ex;
		
		return execute(() -> {
			fi.flags.set(fi.flags.get() | 0x0100);
			return open(path, fi);
		});
	}

	@Override
	public int getattr(String path, FileStat stat) {
		
		return execute(() -> {
			try {
				return fillStat(stat, sftp.stat(path), path);
			} catch (SftpStatusException sftpse) {
				if (Log.isDebugEnabled() && (Log.isTraceEnabled() || sftpse.getStatus() != SftpStatusException.SSH_FX_NO_SUCH_FILE))
					Log.debug("Error retrieving attributes for {}", sftpse, path);
				return toErr(sftpse);
			} catch (Exception e) {
				Log.error("Error retrieving attributes for {}", e, path);
			}
			return -ErrorCodes.EREMOTEIO();
		});
	}

	@Override
	public int mkdir(String path, @mode_t long mode) {
		int ex = exists(path);
		if (ex != -ErrorCodes.ENOENT())
			return ex;
		
		return execute(() -> {
			try {
				sftp.mkdirs(path);
				
				return 0;
			} catch (SftpStatusException e) {
				Log.error("Failed to create directory {}", e, path);
				return toErr(e);
			} catch (SshException e) {
				Log.error("Failed to create directory {}", e, path);
				return -ErrorCodes.EFAULT();
			}
		});
	}

	@Override
	public int open(String path, FuseFileInfo fi) {
		
		return execute(() -> {
			try {
				long handle = fileHandle.getAndIncrement();
				int flgs = convertFlags(fi.flags);
				SftpFile file;
				file = sftp.openFile(path, flgs);

				fi.fh.set(handle);

				handles.put(handle, file);
				flags.put(handle, flgs);
				List<Long> l = handlesByPath.get(path);
				if (l == null) {
					l = new ArrayList<Long>();
					handlesByPath.put(path, l);
				}
				l.add(handle);
				
				return 0;
			} catch (SftpStatusException e) {
				Log.error("Failed to open {}", e, path);
				return toErr(e);
			} catch (SshException e) {
				Log.error("Failed to open {}", e, path);
				return -ErrorCodes.EFAULT();
			}
		});
	}

	@Override
	public int truncate(String path, long size) {
		
		return execute(() -> {
			try {
				/*
				 * This is a bit of a pain. truncate() may occur after an open(),
				 * but this is too late to send an O_TRUNC flag. So instead, we
				 * close the original handle, then re-open it truncated.
				 * 
				 * There could be multiple handles open, so we need to deal with all
				 * of them. The open flags for each are also remembered and used to
				 * re-open (minus the truncate flag for the 2nd handle up to the
				 * last).
				 * 
				 * We also don't get given FuseFileInfo, so need to maintain our own
				 * state of what files are open for a path.
				 * 
				 * If the file is not open then just truncate by opening a new file
				 * with O_TRUN.
				 */

				List<Long> pathHandles = handlesByPath.get(path);
				int idx = 0;
				for (Long l : pathHandles) {
					SftpFile file = handles.get(l);
					file.close();
					int flgs = flags.get(l);
					if (idx == 0) {
						// For the first handle, re-open with truncate,

						file = sftp.openFile(path, flgs | SftpChannel.OPEN_TRUNCATE | SftpChannel.OPEN_CREATE);

						handles.put(l, file);
					} else {
						file = sftp.openFile(path, flgs ^ SftpChannel.OPEN_TRUNCATE ^ SftpChannel.OPEN_CREATE);
					}
					handles.put(l, file);
					idx++;
				}
				if (idx == 0) {
					// No open files
					SftpFile file = sftp.openFile(path, SftpChannel.OPEN_TRUNCATE | SftpChannel.OPEN_CREATE);
					file.close();
				}
				
				return 0;
			} catch (SftpStatusException e) {
				Log.error("Failed to open {}", e, path);
				return toErr(e);
			} catch (SshException e) {
				Log.error("Failed to open {}", e, path);
				return -ErrorCodes.EFAULT();
			}
		});
	}

	@Override
	public int read(String path, Pointer buf, @size_t long size, @off_t long offset, FuseFileInfo fi) {
		
		return execute(() -> {
			try {
				SftpFile file = handles.get(fi.fh.longValue());
				if (file == null)
					return -ErrorCodes.ESTALE();
				byte[] b = new byte[Math.min(MAX_READ_BUFFER_SIZE, (int) size)];
				int read;
				read = file.read(offset, b, 0, b.length);
				buf.put(0, b, 0, read);
				return read;
			} catch (SftpStatusException e) {
				Log.error("Failed to open {}", e, path);
				return toErr(e);
			} catch (SshException e) {
				Log.error("Failed to open {}", e, path);
				return -ErrorCodes.EFAULT();
			}
		});
	}

	@Override
	public int readlink(String path, Pointer buf, long size) {
		
		return execute(() -> {
			try {
				buf.putString(0, sftp.getSymbolicLinkTarget(path), 0, Charset.defaultCharset());
				return 0;
			} catch (SftpStatusException sftpse) {
				if (Log.isDebugEnabled() && (Log.isTraceEnabled() || sftpse.getStatus() != SftpStatusException.SSH_FX_NO_SUCH_FILE))
					Log.debug("Error retrieving attributes for {}.", sftpse, path);;
				return toErr(sftpse);
			} catch (Exception e) {
				Log.error("Error retrieving attributes for {}.", e, path);
			}
			return -ErrorCodes.ENOENT();
		});
	}

	@Override
	public int opendir(String path, FuseFileInfo fi) {
		
		return execute(() -> {
			try {
				long handle = fileHandle.getAndIncrement();
				int flgs = convertFlags(fi.flags);
				SftpFile file;
				file = sftp.openDirectory(path);
				fi.fh.set(handle);

				handles.put(handle, file);
				flags.put(handle, flgs);
				List<Long> l = handlesByPath.get(path);
				if (l == null) {
					l = new ArrayList<Long>();
					handlesByPath.put(path, l);
				}
				l.add(handle);
				return 0;
			} catch (SftpStatusException e) {
				Log.error("Failed to open dir {}", e, path);
				return toErr(e);
			} catch (SshException e) {
				Log.error("Failed to open dir {}", e, path);
				return -ErrorCodes.EFAULT();
			}
		});
	}

	@Override
	public int releasedir(String path, FuseFileInfo fi) {
		return release(path, fi);
	}

	@Override
	public int readdir(String path, Pointer buf, FuseFillDir filter, @off_t long offset, FuseFileInfo fi) {

		AtomicLong _offset = new AtomicLong(offset);
		
		int ret =  execute(() -> {
			if (Log.isDebugEnabled()) {
				Log.debug("Reading directory {} at offset {}", path, _offset.get());
			}
			try {
				SftpFile file = handles.get(fi.fh.longValue());
				if (file == null) {
					if(Log.isDebugEnabled()) {
						Log.debug("File handle is invalid");
					}
					return -ErrorCodes.ESTALE();
				}
				
				file.setProperty("reeaddir_state", sftp.readDirectory(file));
				
				@SuppressWarnings("unchecked")
				List<SftpFile> results = (List<SftpFile>) file.getProperty("reeaddir_state");
				
				if(Objects.isNull(results)) {
					if (Log.isDebugEnabled()) {
						Log.debug("No results for {}", path);
					}
					return 0;
				}
				
				if (Log.isDebugEnabled()) {
					Log.debug("Got {} results remaining", results.size());
				}
			
				if(_offset.get() == 0) {
					filter.apply(buf, ".", null, _offset.incrementAndGet());
					if (!path.equals("/"))
						filter.apply(buf, "..", null, _offset.incrementAndGet());
				}

				while(!results.isEmpty()) {
					SftpFile f = results.remove(0);
					
					if(Log.isDebugEnabled()) {
						Log.debug("Got child path {}", f.getFilename());
					}
					
					if(filter.apply(buf, f.getFilename(), null, _offset.incrementAndGet()) == 1) {
			
						if (Log.isDebugEnabled()) {
							Log.debug("Temporary end of results {}", f.getFilename());
						}
						/**
						 * According to https://www.cs.hmc.edu/~geoff/classes/hmc.cs135.201001/homework/fuse/fuse_doc.html#readdir-details
						 * we return zero when the buffer is full. We need to store the current page and offset for resumption
						 */
						return 0;
					}
				}
				
				if (Log.isDebugEnabled()) {
					Log.debug("End of results for {}", path);
				}
				return 0;
			} catch (SftpStatusException e) {
				Log.error("Failed to open {}", e, path);
				return toErr(e);
			} catch (SshException e) {
				Log.error("Failed to open {}", e, path);
				return -ErrorCodes.EFAULT();
			}
		});
		
		offset = _offset.get();
		return ret;
	}

	@Override
	public int release(String path, FuseFileInfo fi) {
		
		return execute(() -> {
			try {

				SftpFile file = handles.remove(fi.fh.longValue());
				List<Long> l = handlesByPath.get(path);
				flags.remove(fi.fh.longValue());
				if (l != null) {
					l.remove(fi.fh.longValue());
					if (l.isEmpty())
						handlesByPath.remove(path);
				}
				if (file == null)
					return -ErrorCodes.ESTALE();
				file.close();
				return 0;
				
			} catch (SftpStatusException e) {
				Log.error("Failed to open {}", e, path);
				return toErr(e);
			} catch (SshException e) {
				Log.error("Failed to open {}", e, path);
				return -ErrorCodes.EFAULT();
			}
		});
	}

	@Override
	public int rename(String oldpath, String newpath) {
		
		return execute(() -> {
			// TODO forgiveness / permission?
			int ex = exists(oldpath);
			if (ex != -ErrorCodes.EEXIST())
				return ex;
			try {
				sftp.rename(oldpath, newpath);
				return 0;
			} catch (SftpStatusException e) {
				Log.error("Failed to rename {} to {}", e, oldpath, newpath);
				return toErr(e);
			} catch (SshException e) {
				Log.error("Failed to rename {} to {}", e, oldpath, newpath);
				return -ErrorCodes.EFAULT();
			}
		});
	}

	@Override
	public int rmdir(String path) {
		return unlink(path);
	}

	@Override
	public int symlink(String oldpath, String newpath) {
		
		return execute(() -> {
			int ex = exists(oldpath);
			if (ex != -ErrorCodes.EEXIST())
				return ex;
			try {
				sftp.symlink(oldpath, newpath);
				return 0;
			} catch (SftpStatusException e) {
				Log.error("Failed to symlink {} to {}", e, oldpath, newpath);
				return toErr(e);
			} catch (SshException e) {
				Log.error("Failed to remove {} to {}", e, oldpath, newpath);
				return -ErrorCodes.EFAULT();
			}
		});
	}

	@Override
	public int unlink(String path) {
		
		int ex = exists(path);
		if (ex != -ErrorCodes.EEXIST())
			return ex;
		
		return execute(() -> {
			try {
				sftp.rm(path);
				return 0;
			} catch (SftpStatusException e) {
				Log.error("Failed to remove {}", e, path);
				return toErr(e);
			} catch (SshException e) {
				Log.error("Failed to remove {}", e, path);
				return -ErrorCodes.EFAULT();
			}
		});
	}

	@Override
	public int write(String path, Pointer buf, long size, long offset, FuseFileInfo fi) {
		
		return execute(() -> {
			try {
				SftpFile file = handles.get(fi.fh.longValue());
				if (file == null)
					return -ErrorCodes.ESTALE();
				byte[] b = new byte[Math.min(MAX_WRITE_BUFFER_SIZE, (int) size)];
				buf.get(0, b, 0, b.length);
				file.write(offset, b, 0, b.length);
				return b.length;
			} catch (SftpStatusException e) {
				Log.error("Failed to open {}", e, path);
				return toErr(e);
			} catch (SshException e) {
				Log.error("Failed to open {}", e, path);
				return -ErrorCodes.EFAULT();
			}
		});
	}

	int exists(String path) {
		
		return execute(() -> {
			try {
				sftp.stat(path);
				return -ErrorCodes.EEXIST();
			} catch (SftpStatusException sftpse) {
				if (sftpse.getStatus() == SftpStatusException.SSH_FX_INVALID_FILENAME) {
					return -ErrorCodes.ENOENT();
				} else {
					return -ErrorCodes.EFAULT();
				}
			} catch (Exception e) {
				Log.error("Error checking for existance for {}.", e, path);
				return -ErrorCodes.EFAULT();
			}
		});
	}

	private int convertFlags(Signed32 flags) {
		int f = 0;
		int fv = flags.get();
		if ((fv & 0x0001) > 0 || (fv & 0x0002) > 0)
			f = f | SftpChannel.OPEN_WRITE;
		if ((fv & 0x0008) > 0)
			f = f | SftpChannel.OPEN_TEXT;
		if ((fv & 0x0100) > 0)
			f = f | SftpChannel.OPEN_CREATE;
		if ((fv & 0x0200) > 0)
			f = f | SftpChannel.OPEN_EXCLUSIVE;
		if ((fv & 0x0800) > 0)
			f = f | SftpChannel.OPEN_TRUNCATE;
		if ((fv & 0x1000) > 0)
			f = f | SftpChannel.OPEN_APPEND;
		if (f == 0 || fv == 0 || (fv & 0x0002) > 0)
			f = f | SftpChannel.OPEN_READ;
		return f;
	}

	private int fillStat(FileStat stat, SftpFileAttributes file, String path) throws SftpStatusException {
		// TODO can probably do more linux for unix-to-unix file systems
		
		/**
		 * There are issues here. If you set the ownership to the uid on the remote server then
		 * the permissions may not apply to the user the file system is running for. Since
		 * we cannot rely on uid to be a valid uid on this system we have to use something
		 * consistent.
		 */
		stat.st_uid.set(0);
		stat.st_gid.set(0);
		 
		if(file.hasAccessTime()) {
			stat.st_atim.tv_sec.set(file.getAccessedTime().longValue() / 1000);
		}
		if(file.hasModifiedTime()) {
			stat.st_mtim.tv_sec.set(file.getModifiedTime().longValue() / 1000);
		}
		if(file.hasCreateTime()) {
			stat.st_ctim.tv_sec.set(file.getCreationTime().longValue() / 1000);
		}
		
		if(file.hasSize()) {
			stat.st_size.set(file.getSize().longValue());
		} else {
			stat.st_size.set(0L);
		}
		
		/**
		 * This could probably be more intelligent and set "other" permissions,
		 * which is the permissions this user is likely to access them under given
		 * the note above about uid. Setting to all permissions ensures the user
		 *  on this device can access the file but the remote server may deny access.
		 */
		if(file.isDirectory()) {
			stat.st_mode.set(file.getModeType() | 0777);
		} else {
			stat.st_mode.set(file.getModeType() | 0666);
		}

		return 0;
	}

	private int toErr(SftpStatusException e) {
		if (e.getStatus() == SftpStatusException.SSH_FX_OK)
			return 0;
		else if (e.getStatus() == SftpStatusException.SSH_FX_NO_SUCH_FILE)
			return -ErrorCodes.ENOENT();
		else if (e.getStatus() == SftpStatusException.SSH_FX_NOT_A_DIRECTORY)
			return -ErrorCodes.ENOTDIR();
		else if (e.getStatus() == SftpStatusException.SSH_FX_PERMISSION_DENIED)
			return -ErrorCodes.EPERM();
		else if (e.getStatus() == SftpStatusException.SSH_FX_NO_CONNECTION)
			return -ErrorCodes.ENOTCONN();
		else if (e.getStatus() == SftpStatusException.SSH_FX_CONNECTION_LOST)
			return -ErrorCodes.ECONNRESET();
		else if (e.getStatus() == SftpStatusException.SSH_FX_OP_UNSUPPORTED)
			return -ErrorCodes.ENOSYS();
		else if (e.getStatus() == SftpStatusException.SSH_FX_FILE_ALREADY_EXISTS)
			return -ErrorCodes.EEXIST();
		else if (e.getStatus() == SftpStatusException.SSH_FX_BAD_MESSAGE)
			return -ErrorCodes.EBADMSG();
		else if (e.getStatus() == SftpStatusException.SSH_FX_DIR_NOT_EMPTY)
			return -ErrorCodes.ENOTEMPTY();
		else if (e.getStatus() == SftpStatusException.SSH_FX_FILE_IS_A_DIRECTORY)
			return -ErrorCodes.EISDIR();
		else if (e.getStatus() == SftpStatusException.SSH_FX_NO_SPACE_ON_FILESYSTEM)
			return -ErrorCodes.ENOSPC();
		else if (e.getStatus() == SftpStatusException.SSH_FX_QUOTA_EXCEEDED)
			return -ErrorCodes.EDQUOT();
		else if (e.getStatus() == SftpStatusException.SSH_FX_INVALID_PARAMETER)
			return -ErrorCodes.EINVAL();
		else
			return -ErrorCodes.EFAULT();
		// public static final int SSH_FX_EOF = 1;
		// /** No such file was found **/
		// public static final int SSH_FX_FAILURE = 4;
		// /** The client sent a bad protocol message **/
		// public static final int SSH_FX_INVALID_HANDLE = 9;
		// /** The path is invalid */
		// public static final int SSH_FX_NO_SUCH_PATH = 10;
		// /** Cannot write to remote location */
		// public static final int SSH_FX_WRITE_PROTECT = 12;
		// /** There is no media available at the remote location */
		// public static final int SSH_FX_NO_MEDIA = 13;
		//
		// // These error codes are not part of the supported versions however
		// are
		// // included as some servers are returning them.
		// public static final int SSH_FX_UNKNOWN_PRINCIPAL = 16;
		// public static final int SSH_FX_LOCK_CONFLICT = 17;
		// public static final int SSH_FX_INVALID_FILENAME = 20;
		// public static final int SSH_FX_LINK_LOOP = 21;
		// public static final int SSH_FX_CANNOT_DELETE = 22;
		// public static final int SSH_FX_BYTE_RANGE_LOCK_CONFLICT = 25;
		// public static final int SSH_FX_BYTE_RANGE_LOCK_REFUSED = 26;
		// public static final int SSH_FX_DELETE_PENDING = 27;
		// public static final int SSH_FX_FILE_CORRUPT = 28;
		// public static final int SSH_FX_OWNER_INVALID = 29;
		// public static final int SSH_FX_GROUP_INVALID = 30;
		// public static final int SSH_FX_NO_MATCHING_BYTE_RANGE_LOCK = 31;
	}

	@Override
	public void close() throws IOException {
		
		execute(() -> {
			umount();
			return 0;
		});

	}
}
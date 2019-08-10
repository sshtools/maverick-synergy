package com.sshtools.fuse.fs;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import com.sshtools.client.sftp.AbstractSftpTask;
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
	private Map<Long, SftpFile> handles = new HashMap<>();
	private Map<Long, Integer> flags = new HashMap<>();
	private Map<String, List<Long>> handlesByPath = new HashMap<>();
	private SftpClientTask sftp;
	private Object sftpLock = new Object();

	public FuseSFTP(SftpClientTask sftp) throws SftpStatusException, IOException, SshException {
		this.sftp = sftp;
	}
	
	@Override
	public int chmod(String path, @mode_t long mode) {
		// TODO forgiveness / permission?
		int ex = exists(path);
		if (ex != -ErrorCodes.EEXIST())
			return ex;
		try {
			synchronized (sftpLock) {
				sftp.chmod((int) mode, path);
			}
			return 0;
		} catch (SftpStatusException e) {
			Log.error(String.format("Failed to chmod %s to %d", path, mode), e);
			return toErr(e);
		} catch (SshException e) {
			Log.error(String.format("Failed to chmod %s to %d", path, mode), e);
			return -ErrorCodes.EFAULT();
		}
	}

	@Override
	public int chown(String path, long uid, long gid) {
		int ex = exists(path);
		if (ex != -ErrorCodes.EEXIST())
			return ex;
		try {
			sftp.chown(String.valueOf(uid), path);
			sftp.chgrp(String.valueOf(gid), path);
			return 0;
		} catch (SftpStatusException e) {
			Log.error(String.format("Failed to chown %s to %d:%d", path, uid, gid), e);
			return toErr(e);
		} catch (SshException e) {
			Log.error(String.format("Failed to chmod %s to %d:%d", path, uid, gid), e);
			return -ErrorCodes.EFAULT();
		}
	}

	@Override
	public int create(String path, @mode_t long mode, FuseFileInfo fi) {
		synchronized (sftpLock) {
			int ex = exists(path);
			if (ex == -ErrorCodes.EEXIST())
				return ex;
			fi.flags.set(fi.flags.get() | 0x0100);
		}
		return open(path, fi);
	}

	@Override
	public int getattr(String path, FileStat stat) {
		try {
			return fillStat(stat, sftp.stat(path), path);
		} catch (SftpStatusException sftpse) {
			if (Log.isDebugEnabled() && (Log.isTraceEnabled() || sftpse.getStatus() != SftpStatusException.SSH_FX_NO_SUCH_FILE))
				Log.debug(String.format("Error retrieving attributes for %s.", path), sftpse);
			return toErr(sftpse);
		} catch (Exception e) {
			Log.error(String.format("Error retrieving attributes for %s.", path), e);
		}
		return -ErrorCodes.EREMOTEIO();
	}

	@Override
	public int mkdir(String path, @mode_t long mode) {
		// TODO forgiveness / permission?
		int ex = exists(path);
		if (ex != -ErrorCodes.ENOENT())
			return ex;
		try {
			synchronized (sftpLock) {
				sftp.mkdirs(path);
			}
			return 0;
		} catch (SftpStatusException e) {
			Log.error(String.format("Failed to create directory %s", path), e);
			return toErr(e);
		} catch (SshException e) {
			Log.error(String.format("Failed to create directory %s", path), e);
			return -ErrorCodes.EFAULT();
		}
	}

	// @Override
	// public int readlink(String path, Pointer buf, long size) {
	// try {
	// SftpFileAttributes attr = sftp.statLink(path);
	// return 0;
	// } catch (SftpStatusException e) {
	// Log.error(String.format("Failed to open %s", path), e);
	// return toErr(e);
	// } catch (SshException e) {
	// Log.error(String.format("Failed to open %s", path), e);
	// return -ErrorCodes.EFAULT();
	// }
	// }
	@Override
	public int open(String path, FuseFileInfo fi) {
		try {
			long handle = fileHandle.getAndIncrement();
			int flgs = convertFlags(fi.flags);
			SftpFile file;
			synchronized (sftpLock) {
				file = sftp.openFile(path, flgs);
			}
			fi.fh.set(handle);
			synchronized (handles) {
				handles.put(handle, file);
				flags.put(handle, flgs);
				List<Long> l = handlesByPath.get(path);
				if (l == null) {
					l = new ArrayList<Long>();
					handlesByPath.put(path, l);
				}
				l.add(handle);
			}
			return 0;
		} catch (SftpStatusException e) {
			Log.error(String.format("Failed to open %s", path), e);
			return toErr(e);
		} catch (SshException e) {
			Log.error(String.format("Failed to open %s", path), e);
			return -ErrorCodes.EFAULT();
		}
	}

	@Override
	public int truncate(String path, long size) {
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
			synchronized (handles) {
				List<Long> pathHandles = handlesByPath.get(path);
				int idx = 0;
				for (Long l : pathHandles) {
					SftpFile file = handles.get(l);
					file.close();
					int flgs = flags.get(l);
					if (idx == 0) {
						// For the first handle, re-open with truncate,
						synchronized (sftpLock) {
							file = sftp.openFile(path, flgs | AbstractSftpTask.OPEN_TRUNCATE | AbstractSftpTask.OPEN_CREATE);
						}
						handles.put(l, file);
					} else {
						synchronized (sftpLock) {
							file = sftp.openFile(path, flgs ^ AbstractSftpTask.OPEN_TRUNCATE ^ AbstractSftpTask.OPEN_CREATE);
						}
					}
					handles.put(l, file);
					idx++;
				}
				if (idx == 0) {
					// No open files
					synchronized (sftpLock) {
						SftpFile file = sftp.openFile(path, AbstractSftpTask.OPEN_TRUNCATE | AbstractSftpTask.OPEN_CREATE);
						file.close();
					}
				}
			}
			return 0;
		} catch (SftpStatusException e) {
			Log.error(String.format("Failed to open %s", path), e);
			return toErr(e);
		} catch (SshException e) {
			Log.error(String.format("Failed to open %s", path), e);
			return -ErrorCodes.EFAULT();
		}
	}

	@Override
	public int read(String path, Pointer buf, @size_t long size, @off_t long offset, FuseFileInfo fi) {
		try {
			SftpFile file = handles.get(fi.fh.longValue());
			if (file == null)
				return -ErrorCodes.ESTALE();
			byte[] b = new byte[Math.min(MAX_READ_BUFFER_SIZE, (int) size)];
			int read;
			synchronized (sftpLock) {
				read = file.read(offset, b, 0, b.length);
			}
			buf.put(0, b, 0, read);
			return read;
		} catch (SftpStatusException e) {
			Log.error(String.format("Failed to open %s", path), e);
			return toErr(e);
		} catch (SshException e) {
			Log.error(String.format("Failed to open %s", path), e);
			return -ErrorCodes.EFAULT();
		}
	}

	@Override
	public int readlink(String path, Pointer buf, long size) {
		try {
			sftp.statLink(path);
			buf.putString(0, "", 0, Charset.defaultCharset());
			return 0;
		} catch (SftpStatusException sftpse) {
			if (Log.isDebugEnabled() && (Log.isTraceEnabled() || sftpse.getStatus() != SftpStatusException.SSH_FX_NO_SUCH_FILE))
				Log.debug(String.format("Error retrieving attributes for %s.", path), sftpse);
			return toErr(sftpse);
		} catch (Exception e) {
			Log.error(String.format("Error retrieving attributes for %s.", path), e);
		}
		return -ErrorCodes.ENOENT();
	}

	@Override
	public int opendir(String path, FuseFileInfo fi) {
		try {
			long handle = fileHandle.getAndIncrement();
			int flgs = convertFlags(fi.flags);
			SftpFile file;
			synchronized (sftpLock) {
				file = sftp.openDirectory(path);
			}
			fi.fh.set(handle);
			synchronized (handles) {
				handles.put(handle, file);
				flags.put(handle, flgs);
				List<Long> l = handlesByPath.get(path);
				if (l == null) {
					l = new ArrayList<Long>();
					handlesByPath.put(path, l);
				}
				l.add(handle);
			}
			return 0;
		} catch (SftpStatusException e) {
			Log.error(String.format("Failed to open dir %s", path), e);
			return toErr(e);
		} catch (SshException e) {
			Log.error(String.format("Failed to open dir %s", path), e);
			return -ErrorCodes.EFAULT();
		}
	}

	@Override
	public int releasedir(String path, FuseFileInfo fi) {
		return release(path, fi);
	}

	@Override
	public int readdir(String path, Pointer buf, FuseFillDir filter, @off_t long offset, FuseFileInfo fi) {

		if (Log.isInfoEnabled()) {
			Log.info(String.format("Reading directory %s", path));
		}
		try {
			SftpFile file = handles.get(fi.fh.longValue());
			if (file == null)
				return -ErrorCodes.ESTALE();
			
			@SuppressWarnings("unchecked")
			List<SftpFile> results = (List<SftpFile>) file.getProperty("reeaddir_state");
			
			do {
				if(!Objects.isNull(results)) {
					if(offset==0) {
						filter.apply(buf, ".", null, ++offset);
						if (!path.equals("/"))
							filter.apply(buf, "..", null, ++offset);
					}
					synchronized (sftpLock) {
						while(!results.isEmpty()) {
							SftpFile f = results.remove(0);
							if(filter.apply(buf, f.getFilename(), null, ++offset) == 1) {
								/**
								 * According to https://www.cs.hmc.edu/~geoff/classes/hmc.cs135.201001/homework/fuse/fuse_doc.html#readdir-details
								 * we return zero when the buffer is full. We need to store the current page and offset for resumption
								 */
								file.setProperty("reeaddir_state", results);
								return 0;
							}
						}
					}
				}
					
				results = sftp.readDirectory(file);
				
			} while(!Objects.isNull(results));
			
			return 0;
		} catch (SftpStatusException e) {
			Log.error(String.format("Failed to open %s", path), e);
			return toErr(e);
		} catch (SshException e) {
			Log.error(String.format("Failed to open %s", path), e);
			return -ErrorCodes.EFAULT();
		}
	}

	@Override
	public int release(String path, FuseFileInfo fi) {
		try {
			synchronized (handles) {
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
			}
		} catch (SftpStatusException e) {
			Log.error(String.format("Failed to open %s", path), e);
			return toErr(e);
		} catch (SshException e) {
			Log.error(String.format("Failed to open %s", path), e);
			return -ErrorCodes.EFAULT();
		}
	}

	@Override
	public int rename(String oldpath, String newpath) {
		// TODO forgiveness / permission?
		int ex = exists(oldpath);
		if (ex != -ErrorCodes.EEXIST())
			return ex;
		try {
			synchronized (sftpLock) {
				sftp.rename(oldpath, newpath);
			}
			return 0;
		} catch (SftpStatusException e) {
			Log.error(String.format("Failed to rename %s to %s", oldpath, newpath), e);
			return toErr(e);
		} catch (SshException e) {
			Log.error(String.format("Failed to rename %s to %s", oldpath, newpath), e);
			return -ErrorCodes.EFAULT();
		}
	}

	@Override
	public int rmdir(String path) {
		return unlink(path);
	}

	@Override
	public int symlink(String oldpath, String newpath) {
		// TODO forgiveness / permission?
		int ex = exists(oldpath);
		if (ex != -ErrorCodes.EEXIST())
			return ex;
		try {
			synchronized (sftpLock) {
				sftp.symlink(oldpath, newpath);
			}
			return 0;
		} catch (SftpStatusException e) {
			Log.error(String.format("Failed to symlink %s to %s", oldpath, newpath), e);
			return toErr(e);
		} catch (SshException e) {
			Log.error(String.format("Failed to remove %s to %s", oldpath, newpath), e);
			return -ErrorCodes.EFAULT();
		}
	}

	@Override
	public int unlink(String path) {
		// TODO forgiveness / permission?
		int ex = exists(path);
		if (ex != -ErrorCodes.EEXIST())
			return ex;
		try {
			synchronized (sftpLock) {
				sftp.rm(path);
			}
			return 0;
		} catch (SftpStatusException e) {
			Log.error(String.format("Failed to remove %s", path), e);
			return toErr(e);
		} catch (SshException e) {
			Log.error(String.format("Failed to remove %s", path), e);
			return -ErrorCodes.EFAULT();
		}
	}

	@Override
	public int write(String path, Pointer buf, long size, long offset, FuseFileInfo fi) {
		try {
			SftpFile file = handles.get(fi.fh.longValue());
			if (file == null)
				return -ErrorCodes.ESTALE();
			byte[] b = new byte[Math.min(MAX_WRITE_BUFFER_SIZE, (int) size)];
			buf.get(0, b, 0, b.length);
			synchronized (sftpLock) {
				file.write(offset, b, 0, b.length);
			}
			return b.length;
		} catch (SftpStatusException e) {
			Log.error(String.format("Failed to open %s", path), e);
			return toErr(e);
		} catch (SshException e) {
			Log.error(String.format("Failed to open %s", path), e);
			return -ErrorCodes.EFAULT();
		}
	}

	int exists(String path) {
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
			Log.error(String.format("Error checking for existance for %s.", path), e);
			return -ErrorCodes.EFAULT();
		}
	}

	private int convertFlags(Signed32 flags) {
		int f = 0;
		int fv = flags.get();
		if ((fv & 0x0001) > 0 || (fv & 0x0002) > 0)
			f = f | AbstractSftpTask.OPEN_WRITE;
		if ((fv & 0x0008) > 0)
			f = f | AbstractSftpTask.OPEN_TEXT;
		if ((fv & 0x0100) > 0)
			f = f | AbstractSftpTask.OPEN_CREATE;
		if ((fv & 0x0200) > 0)
			f = f | AbstractSftpTask.OPEN_EXCLUSIVE;
		if ((fv & 0x0800) > 0)
			f = f | AbstractSftpTask.OPEN_TRUNCATE;
		if ((fv & 0x1000) > 0)
			f = f | AbstractSftpTask.OPEN_APPEND;
		if (f == 0 || fv == 0 || (fv & 0x0002) > 0)
			f = f | AbstractSftpTask.OPEN_READ;
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
		umount();
	}
}
package com.sshtools.server.vsession.commands.sftp;

import java.io.IOException;

import com.sshtools.client.sftp.TransferCancelledException;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.SshException;
import com.sshtools.server.vsession.UsageException;
import com.sshtools.server.vsession.VirtualConsole;

/**
 * get [-afPpr] remote-path [local-path] Retrieve the remote-path and store it
 * on the local machine. If the local path name is not specified, it is given
 * the same name it has on the remote machine. remote-path may contain glob(7)
 * characters and may match multiple files. If it does and local-path is
 * specified, then local-path must specify a direc‐ tory.
 * 
 * If the -a flag is specified, then attempt to resume partial transfers of
 * existing files. Note that resumption assumes that any partial copy of the
 * local file matches the remote copy. If the remote file contents differ from
 * the partial local copy then the resultant file is likely to be corrupt.
 * 
 * If the -f flag is specified, then fsync(2) will be called after the file
 * transfer has completed to flush the file to disk.
 * 
 * If either the -P or -p flag is specified, then full file permis‐ sions and
 * access times are copied too.
 * 
 * If the -r flag is specified then directories will be copied recursively. Note
 * that sftp does not follow symbolic links when performing recursive transfers.
 * 
 * @author user
 *
 */
public class Get extends SftpCommand {

	public Get() {
		super("get", "SFTP", "get", "Transfers a file from remote to local server.");
	}

	@Override
	public void run(String[] args, VirtualConsole console)
			throws IOException, PermissionDeniedException, UsageException {
		try {
			if (args[1].startsWith("-")) {
				// we have options
				String options = args[1].substring(1);
				SftpFileTransferOptions transferOptions = SftpFileTransferOptions.parse(options);
				if (args.length == 4) {
					if (transferOptions.isRecurse()) {
						this.sftp.getRemoteDirectory(args[2], args[3], true, false, true, null);
					} else {
						this.sftp.get(args[2], args[3], transferOptions.isResume());
					}
				} else if (args.length == 3) {
					this.sftp.get(args[2], transferOptions.isResume());
				} else {
					throw new IllegalStateException("Cannot understand `get` command.");
				}
			} else {
				// no options
				if (args.length == 3) {
					this.sftp.get(args[1], args[2]);
				} else if (args.length == 2) {
					this.sftp.get(args[1]);
				} else {
					throw new IllegalStateException("Cannot understand `get` command.");
				}
			}

		} catch (SftpStatusException | SshException | TransferCancelledException | IOException
				| PermissionDeniedException e) {
			throw new IllegalStateException("Problem in transfer for file", e);
		}
	}

}

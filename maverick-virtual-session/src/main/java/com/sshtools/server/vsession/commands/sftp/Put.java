package com.sshtools.server.vsession.commands.sftp;

import java.io.IOException;

import com.sshtools.client.sftp.TransferCancelledException;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.SshException;
import com.sshtools.server.vsession.UsageException;
import com.sshtools.server.vsession.VirtualConsole;

/**
 * put [-afPpr] local-path [remote-path]
             Upload local-path and store it on the remote machine.  If the
             remote path name is not specified, it is given the same name it
             has on the local machine.  local-path may contain glob(7) charac‐
             ters and may match multiple files.  If it does and remote-path is
             specified, then remote-path must specify a directory.

             If the -a flag is specified, then attempt to resume partial
             transfers of existing files.  Note that resumption assumes that
             any partial copy of the remote file matches the local copy.  If
             the local file contents differ from the remote local copy then
             the resultant file is likely to be corrupt.

             If the -f flag is specified, then a request will be sent to the
             server to call fsync(2) after the file has been transferred.
             Note that this is only supported by servers that implement the
             "fsync@openssh.com" extension.

             If either the -P or -p flag is specified, then full file permis‐
             sions and access times are copied too.

             If the -r flag is specified then directories will be copied
             recursively.  Note that sftp does not follow symbolic links when
             performing recursive transfers.
 * @author user
 *
 */
public class Put extends SftpCommand {

	public Put() {
		super("put", "SFTP", "put", "Transfers a file from local to remote server.");
	}

	@Override
	public void run(String[] args, VirtualConsole console)
			throws IOException, PermissionDeniedException, UsageException {
		try {
			if (args.length == 4) {
				
			} else if (args.length == 3) {
				
			} else if (args.length == 2) {
				this.sftp.put(args[1]);
			}
		
		} catch (SftpStatusException | SshException | TransferCancelledException | IOException
				| PermissionDeniedException e) {
			throw new IllegalStateException("Problem in transfer for file", e);
		}
	}

}

package com.sshtools.common.sftp.extensions.filter;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;

import com.sshtools.common.events.Event;
import com.sshtools.common.events.EventCodes;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.AbstractFileSystem;
import com.sshtools.common.sftp.GlobSftpFileFilter;
import com.sshtools.common.sftp.RegexSftpFileFilter;
import com.sshtools.common.sftp.SftpExtension;
import com.sshtools.common.sftp.SftpStatusEventException;
import com.sshtools.common.sftp.SftpSubsystem;
import com.sshtools.common.util.ByteArrayReader;

public class OpenDirectoryWithFilterExtension implements SftpExtension {

	public static final String EXTENSION_NAME = "open-directory-with-filter@sshtools.com";
	
	@Override
	public void processMessage(ByteArrayReader bar, int requestId, SftpSubsystem sftp) {
		
		/**
		 * Open a directory with a filter applied so that read dir requests only
		 * return files that match the filter.
		 */
		String path = null;
		String filter = null;
		Date started = new Date();
		
		try {

			AbstractFileSystem fs = sftp.getFileSystem();
			
			path = sftp.checkDefaultPath(bar.readString(sftp.getCharsetEncoding()));
			filter = bar.readString(sftp.getCharsetEncoding());
			boolean regex = bar.readBoolean();

			
			byte[] handle = fs.openDirectory(path, 
					regex ? new RegexSftpFileFilter(filter) : new GlobSftpFileFilter(filter));

			try {
				fireOpenDirectoryEvent(sftp, path, filter, started, handle, null);
				sftp.sendHandleMessage(requestId, handle);
			} catch (SftpStatusEventException ex) {
				sftp.sendStatusMessage(requestId, ex.getStatus(), ex.getMessage());
			}
		} catch (FileNotFoundException ioe) {
			fireOpenDirectoryEvent(sftp, path, filter, started, null, ioe);
			sftp.sendStatusMessage(requestId, SftpSubsystem.STATUS_FX_NO_SUCH_FILE, ioe.getMessage());
		} catch (IOException ioe2) {
			fireOpenDirectoryEvent(sftp, path, filter, started, null, ioe2);
			sftp.sendStatusMessage(requestId, SftpSubsystem.STATUS_FX_FAILURE, ioe2.getMessage());
		} catch (PermissionDeniedException pde) {
			fireOpenDirectoryEvent(sftp, path, filter, started, null, pde);
			sftp.sendStatusMessage(requestId, SftpSubsystem.STATUS_FX_PERMISSION_DENIED,
					pde.getMessage());
		} finally {
			bar.close();
		}
	}

	public void fireOpenDirectoryEvent(SftpSubsystem sftp, String path, 
				String filter, Date started, byte[] handle, Exception error) {
		sftp.fireEvent(new Event(sftp, EventCodes.EVENT_SFTP_DIR,
								error)
								.addAttribute(
										EventCodes.ATTRIBUTE_CONNECTION,
										sftp.getConnection())
								.addAttribute(
										EventCodes.ATTRIBUTE_BYTES_TRANSFERED,
										new Long(0))
								.addAttribute(
										EventCodes.ATTRIBUTE_HANDLE,
										handle)
								.addAttribute(
										EventCodes.ATTRIBUTE_FILE_NAME,
										path)
								.addAttribute(
										EventCodes.ATTRIBUTE_OPERATION_STARTED,
										started)
								.addAttribute(
										EventCodes.ATTRIBUTE_OPERATION_FINISHED,
										new Date()));
	}
	
	@Override
	public boolean supportsExtendedMessage(int messageId) {
		return false;
	}

	@Override
	public void processExtendedMessage(ByteArrayReader msg, SftpSubsystem sftp) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isDeclaredInVersion() {
		return false;
	}

	@Override
	public byte[] getDefaultData() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getName() {
		return EXTENSION_NAME;
	}

}

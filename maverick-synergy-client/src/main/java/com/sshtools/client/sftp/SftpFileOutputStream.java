package com.sshtools.client.sftp;

/*-
 * #%L
 * Client API
 * %%
 * Copyright (C) 2002 - 2024 JADAPTIVE Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.io.IOException;
import java.io.OutputStream;
import java.util.Vector;

import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.SshIOException;
import com.sshtools.common.util.UnsignedInteger32;

/**
 * An OutputStream to write data to a remote file.
 */
public class SftpFileOutputStream extends OutputStream {

	private final SftpHandle handle;
	private final SftpChannel sftp;
	private final Vector<UnsignedInteger32> outstandingRequests = new Vector<UnsignedInteger32>();

	private long position;
	private boolean error = false;

	/**
	 * Creates a new SftpFileOutputStream object.
	 *
	 * @param file
	 *
	 * @throws SftpStatusException
	 * @throws SshException
	 * @deprecated
	 * @see SftpClient#getInputStream(String)
	 */
	@Deprecated(since = "3.1.0", forRemoval = true)
	public SftpFileOutputStream(SftpFile file) throws SftpStatusException, SshException {
		sftp = file.getSFTPChannel();
		handle = file.openFile(SftpChannel.OPEN_CREATE | SftpChannel.OPEN_TRUNCATE | SftpChannel.OPEN_WRITE);
	}

	/**
	 * Creates a new SftpFileOutputStream object.
	 *
	 * @param handle
	 *
	 * @throws SftpStatusException
	 * @throws SshException
	 */
	SftpFileOutputStream(SftpHandle handle) throws SftpStatusException, SshException {
		this.handle = handle;
		this.sftp = handle.getSFTPChannel();
	}

	/**
	 *
	 */
	public void write(byte[] buffer, int offset, int len) throws IOException {
		try {

			int count;
			while (len > 0) {

				count = Math.min(32768, len);

				// Post a request
				outstandingRequests
						.addElement(handle.postWriteRequest(position, buffer, offset, count));

				processNextResponse(100);

				// Update our positions
				offset += count;
				len -= count;
				position += count;
			}

		} catch (SshException ex) {
			throw new SshIOException(ex);
		} catch (SftpStatusException ex) {
			throw new IOException(ex.getMessage());
		}

	}

	/**
	 *
	 */
	public void write(int b) throws IOException {
		try {

			byte[] array = new byte[] { (byte) b };

			// Post a request
			outstandingRequests.addElement(handle.postWriteRequest(position, array, 0, 1));

			processNextResponse(100);

			// Update our positions
			position += 1;

		} catch (SshException ex) {
			throw new SshIOException(ex);
		} catch (SftpStatusException ex) {
			throw new IOException(ex.getMessage());
		}
	}

	private boolean processNextResponse(int numOutstandingRequests) throws SftpStatusException, SshException {
		try {
			// Maybe look for a response
			if (outstandingRequests.size() > numOutstandingRequests) {
				UnsignedInteger32 requestid = (UnsignedInteger32) outstandingRequests.elementAt(0);
				sftp.getOKRequestStatus(requestid, handle.getFile().getAbsolutePath());
				outstandingRequests.removeElementAt(0);
			}

			return outstandingRequests.size() > 0;
		} catch (SshException e) {
			error = true;
			throw e;
		} catch (SftpStatusException e) {
			error = true;
			throw e;
		}
	}

	/**
	 * Closes the file's handle
	 */
	public void close() throws IOException {
		try {
			while (!error && processNextResponse(0))
				;
			handle.close();
		} catch (SshException ex) {
			throw new SshIOException(ex);
		} catch (SftpStatusException ex) {
			throw new IOException(ex.getMessage());
		}
	}
}

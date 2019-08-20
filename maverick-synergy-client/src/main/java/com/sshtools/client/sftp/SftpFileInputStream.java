/**
 * (c) 2002-2019 JADAPTIVE Limited. All Rights Reserved.
 *
 * This file is part of the Maverick Synergy Java SSH API.
 *
 * Maverick Synergy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Maverick Synergy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.sshtools.client.sftp;

import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.SshIOException;
import com.sshtools.common.util.UnsignedInteger32;

/**
 * An InputStream to read the contents of a remote file.
 */
public class SftpFileInputStream extends InputStream {

	SftpFile file;
	AbstractSftpTask sftp;
	long position;
	Vector<UnsignedInteger32> outstandingRequests = new Vector<UnsignedInteger32>();
	SftpMessage currentMessage;
	int currentMessageRemaining;
	boolean isEOF = false;
	boolean error = false;
	
	/**
	 * 
	 * @param file
	 * @throws SftpStatusException
	 * @throws SshException
	 */
	public SftpFileInputStream(SftpFile file) throws SftpStatusException,
			SshException {
		this(file, 0);
	}

	/**
	 * Creates a new SftpFileInputStream object.
	 * 
	 * @param file
	 * @param position
	 *            at which to start reading
	 * @throws SftpStatusException
	 * @throws SshException
	 */
	public SftpFileInputStream(SftpFile file, long position)
			throws SftpStatusException, SshException {
		if (file.getHandle() == null) {
			throw new SftpStatusException(SftpStatusException.INVALID_HANDLE,
					"The file does not have a valid handle!");
		}

		if (file.getSFTPChannel() == null) {
			throw new SshException(
					"The file is not attached to an SFTP subsystem!",
					SshException.BAD_API_USAGE);
		}

		this.file = file;
		this.position = position;
		this.sftp = file.getSFTPChannel();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.InputStream#read(byte[], int, int)
	 */
	public int read(byte[] buffer, int offset, int len) throws IOException {

		try {

			if (isEOF && currentMessageRemaining == 0) {
				currentMessage.release();
				return -1;
			}

			int read = 0;
			int wantsLength = len;
			while (read < wantsLength && !isEOF) {

				if (currentMessage == null || currentMessageRemaining == 0) {
					bufferNextMessage();
					if (isEOF && read == 0) {
						return -1;
					}
				}

				if (currentMessage == null)
					throw new IOException(
							"Failed to obtain file data or status from the SFTP server!");

				int count = Math.min(currentMessageRemaining, len);

				System.arraycopy(currentMessage.array(),
						currentMessage.getPosition(), buffer, offset, count);

				currentMessageRemaining -= count;
				currentMessage.skip(count);

				if (currentMessageRemaining == 0) {
					bufferNextMessage();
				}
				read += count;
				len -= count;
				offset += count;

			}

			return read;
		} catch (SshException ex) {
			throw new SshIOException(ex);
		} catch (SftpStatusException ex) {
			throw new IOException(ex.getMessage());
		}
	}

	private void bufferNextMessage() throws SshException, IOException,
			SftpStatusException {

		try {
			if(currentMessage!=null) {
				currentMessage.release();
			}
			
			bufferMoreData();

			UnsignedInteger32 requestid = outstandingRequests.remove(0);

			currentMessage = sftp.getResponse(requestid);

			if (currentMessage.getType() == AbstractSftpTask.SSH_FXP_DATA) {
				currentMessageRemaining = (int) currentMessage.readInt();
			} else if (currentMessage.getType() == AbstractSftpTask.SSH_FXP_STATUS) {
				
				try {
					int status = (int) currentMessage.readInt();
					if (status == SftpStatusException.SSH_FX_EOF) {
						isEOF = true;
						return;
					}
					if (sftp.getVersion() >= 3) {
						String desc = currentMessage.readString();
						throw new IOException(desc);
					}
					throw new IOException("Unexpected status " + status);
				} finally {
					currentMessage.release();
				}
			} else {
				close();
				throw new IOException(
						"The server responded with an unexpected SFTP protocol message! type="
								+ currentMessage.getType());
			}
		} catch (SshException e) {
			error = true;
			throw e;
		} catch(SftpStatusException e) {
			error = true;
			throw e;
		}
	}

	private void bufferMoreData() throws SftpStatusException, SshException {
		while (outstandingRequests.size() < 100) {
			outstandingRequests.addElement(sftp.postReadRequest(
					file.getHandle(), position, 32768));
			position += 32768;
		}
	}
	
	public int available() {
		return currentMessageRemaining;
	}

	/**
   *
   */
	public int read() throws java.io.IOException {
		byte[] b = new byte[1];
		if (read(b) == 1) {
			return (b[0] & 0xFF);
		}

		return -1;
	}

	/**
	 * Closes the SFTP file handle.
	 */
	public void close() throws IOException {
		try {
			file.close();

			UnsignedInteger32 requestid;
			while (!error && outstandingRequests.size() > 0) {
				requestid = (UnsignedInteger32) outstandingRequests
						.elementAt(0);
				outstandingRequests.remove(0);
				sftp.getResponse(requestid).release();
			}
		} catch (SshException ex) {
			throw new SshIOException(ex);
		} catch (SftpStatusException ex) {
			throw new IOException(ex.getMessage());
		}
	}

	/**
	 * This method will only be available in J2SE builds
	 */
	protected void finalize() throws IOException {
		if (file.getHandle() != null) {
			close();
		}
	}
}

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
import java.io.InputStream;
import java.util.Vector;

import com.sshtools.common.logger.Log;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.SshIOException;
import com.sshtools.common.util.UnsignedInteger32;
import com.sshtools.common.util.UnsignedInteger64;

/**
 * An InputStream to read the contents of a remote file.
 */
public class SftpFileInputStream extends InputStream {

	private final SftpHandle handle;
	private final SftpChannel sftp;
	private final Vector<UnsignedInteger32> outstandingRequests = new Vector<UnsignedInteger32>();
	private long position;
	private SftpMessage currentMessage;
	private int currentMessageRemaining;
	private boolean isEOF = false;
	private boolean error = false;
	private UnsignedInteger64 length;
	
	/**
	 * 
	 * Creates a new SftpFileInputStream object.
	 * 
	 * @param file file
	 * @throws SftpStatusException
	 * @throws SshException
	 * @deprecated
	 * @see SftpClient#getInputStream(String)
	 */
	@Deprecated(since = "3.1.0", forRemoval = true)
	public SftpFileInputStream(SftpFile file) throws SftpStatusException,
			SshException {
		this(file, 0);
	}

	/**
	 * Creates a new SftpFileInputStream object.
	 * 
	 * @param file file
	 * @param position
	 *            at which to start reading
	 * @throws SftpStatusException
	 * @throws SshException
	 * @deprecated
	 * @see SftpClient#getInputStream(String)
	 */
	@Deprecated(since = "3.1.0", forRemoval = true)
	public SftpFileInputStream(SftpFile file, long position) throws SftpStatusException, SshException {
		this.sftp = file.getSFTPChannel();
		this.length = file.attributes().size();
		this.handle = file.openFile(SftpChannel.OPEN_READ);
		this.position = position;
		
	}

	/**
	 * Creates a new SftpFileInputStream object.
	 * 
	 * @param handle handle
	 * @param position
	 *            at which to start reading
	 * @throws SftpStatusException
	 * @throws SshException
	 */
	SftpFileInputStream(SftpHandle handle, long position) throws SftpStatusException, SshException {
		this.handle = handle;
		this.position = position;
		this.sftp = handle.getSFTPChannel();
		this.length = handle.getFile().attributes().size();
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

			if (currentMessage.getType() == SftpChannel.SSH_FXP_DATA) {
				if(Log.isDebugEnabled()) {
					Log.debug("Received SSH_FXP_DATA for {}", handle.getFile().getFilename());
				}
				currentMessageRemaining = (int) currentMessage.readInt();
			} else if (currentMessage.getType() == SftpChannel.SSH_FXP_STATUS) {
				
				try {
					int status = (int) currentMessage.readInt();
					if (status == SftpStatusException.SSH_FX_EOF) {
						if(Log.isDebugEnabled()) {
							Log.debug("Received SSH_FX_EOF for {}", handle.getFile().getFilename());
						}
						isEOF = true;
						return;
					}
					if (sftp.getVersion() >= 3) {
						String desc = currentMessage.readString();
						if(Log.isDebugEnabled()) {
							Log.debug("Received SSH_FXP_STATUS {}/{} for {}", 
									status, 
									desc,
									handle.getFile().getFilename());
						}
						
						throw new IOException(desc);
					}
					if(Log.isDebugEnabled()) {
						Log.debug("Received SSH_FXP_STATUS {} for {}", 
								status, handle.getFile().getFilename());
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
		
		/** 
		 * Read up to length of file
		 */
		while (outstandingRequests.size() < 100 && length.longValue() > position) {
			outstandingRequests.addElement(handle.postReadRequest(position, 32768));
			position += 32768;
		}
		
		/**
		 * If there are no requests then add one to ensure we are still reading if file size
		 * has changed.
		 */
		if(outstandingRequests.isEmpty()) {
			outstandingRequests.addElement(handle.postReadRequest(position, 32768));
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
	@Override
	public void close() throws IOException {
		try {
			handle.close();

			UnsignedInteger32 requestid;
			while (!error && outstandingRequests.size() > 0) {
				requestid = (UnsignedInteger32) outstandingRequests
						.elementAt(0);
				outstandingRequests.remove(0);
				sftp.getResponse(requestid).release();
			}
		} catch (SshException ex) {
			throw new SshIOException(ex);
		}
	}
}

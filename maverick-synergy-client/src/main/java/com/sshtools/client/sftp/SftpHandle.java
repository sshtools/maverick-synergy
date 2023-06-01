package com.sshtools.client.sftp;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import com.sshtools.client.tasks.FileTransferProgress;
import com.sshtools.common.events.Event;
import com.sshtools.common.events.EventCodes;
import com.sshtools.common.events.EventServiceImplementation;
import com.sshtools.common.logger.Log;
import com.sshtools.common.sftp.SftpFileAttributes;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.Packet;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.SshIOException;
import com.sshtools.common.util.ByteArrayWriter;
import com.sshtools.common.util.IOUtils;
import com.sshtools.common.util.UnsignedInteger32;
import com.sshtools.common.util.UnsignedInteger64;

public final class SftpHandle implements Closeable {

	static SftpHandle of(byte[] handle, SftpChannel sftp) {
		return new SftpHandle(handle, sftp, null);
	}

	private final byte[] handle;
	private final SftpChannel sftp;
	private final SftpFile file;

	private volatile boolean closed;
	private volatile boolean performVerification = false;

	SftpHandle(byte[] handle, SftpChannel sftp, SftpFile file) {
		super();
		this.handle = handle;
		this.sftp = sftp;
		this.file = file;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(handle);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SftpHandle other = (SftpHandle) obj;
		return Arrays.equals(handle, other.handle);
	}

	/**
	 * Get the file associated with this handle.
	 * 
	 * @return file
	 */
	public SftpFile getFile() {
		return file;
	}

	public void copyTo(SftpHandle destinationHandle, UnsignedInteger64 fromOffset, UnsignedInteger64 length,
			UnsignedInteger64 toOffset)
			throws SftpStatusException, SshException, IOException {

		if (!isOpen() || !destinationHandle.isOpen()) {
			throw new SftpStatusException(SftpStatusException.SSH_FX_INVALID_HANDLE,
					"source and desintation files must be open");
		}

		try (ByteArrayWriter msg = new ByteArrayWriter()) {
			msg.writeBinaryString(handle);
			msg.writeUINT64(fromOffset);
			msg.writeUINT64(length);
			msg.writeBinaryString(destinationHandle.getHandle());
			msg.writeUINT64(toOffset);

			sftp.getOKRequestStatus(sftp.sendExtensionMessage("copy-data", msg.toByteArray()));

		}
	}

	/**
	 * <p>
	 * Read bytes directly from this file. This is a low-level operation, you may
	 * only need to use {@link SftpClientTask#get(String)} methods instead if you
	 * just want to download files.
	 * </p>
	 * 
	 * @param offset       offset in remote file to read from
	 * @param output       output buffer to place read bytes in
	 * @param outputOffset offset in output buffer to write bytes to
	 * @param len          number of bytes to read
	 * @return int number of bytes read
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 */
	public int read(long offset, byte[] output, int outputOffset, int len) throws SftpStatusException, SshException {
		checkValidHandle();
		return readFile(new UnsignedInteger64(offset), output, outputOffset, len);
	}

	/**
	 * <p>
	 * Write bytes directly to this file. This is a low-level operation, you may
	 * only need to use {@link SftpClientTask#put(String)} methods instead if you
	 * just want to upload files.
	 * </p>
	 * 
	 * @param offset      offset in remote file to write to
	 * @param input       input buffer to retrieve bytes from to write
	 * @param inputOffset offset in output buffer to write bytes to
	 * @param len         number of bytes to write
	 * 
	 * @throws SftpStatusException
	 * @throws SshException
	 */
	public void write(long offset, byte[] input, int inputOffset, int len) throws SftpStatusException, SshException {
		checkValidHandle();
		sftp.writeFile(handle, new UnsignedInteger64(offset), input, inputOffset, len);
	}

	/**
	 * Determine whether the file is open.
	 *
	 * @return boolean
	 */
	public boolean isOpen() {
		return !closed;
	}

	/**
	 * Get the open file handle
	 *
	 * @return byte[]
	 */
	public byte[] getHandle() {
		return handle;
	}

	/**
	 * Close the handle.
	 *
	 * @throws SshException
	 * @throws SftpStatusException
	 */
	@Override
	public void close() throws IOException {
		if (!closed) {
			closed = true;
			try {
				UnsignedInteger32 requestId = sftp.nextRequestId();
				Packet msg = sftp.createPacket();
				msg.write(SftpChannel.SSH_FXP_CLOSE);
				msg.writeInt(requestId.longValue());
				msg.writeBinaryString(handle);

				sftp.sendMessage(msg);

				sftp.getOKRequestStatus(requestId);
			} catch (SshException | SshIOException | SftpStatusException ex) {
				throw new IOException("Failed to close handle.", ex);
			}
			EventServiceImplementation.getInstance()
					.fireEvent((new Event(this, EventCodes.EVENT_SFTP_FILE_CLOSED, true))
							.addAttribute(EventCodes.ATTRIBUTE_FILE_NAME, file.getAbsolutePath()));
		}
	}

	/**
	 * <p>
	 * List the children of a directory.
	 * </p>
	 * <p>
	 * To use this method first open a directory with the
	 * <a href="#openDirectory(java.lang.String)"> openDirectory</a> method and then
	 * create a Vector to store the results. To retrieve the results keep calling
	 * this method until it returns -1 which indicates no more results will be
	 * returned. <blockquote>
	 * 
	 * <pre>
	 * SftpFile dir = sftp.openDirectory(&quot;code/foobar&quot;);
	 * Vector results = new Vector();
	 * while (sftp.listChildren(dir, results) &gt; -1)
	 * 	;
	 * sftp.closeFile(dir);
	 * </pre>
	 * 
	 * </blockquote>
	 * 
	 * </p>
	 * 
	 * @param handle
	 * @param children
	 * @return int
	 * @throws SftpStatusException , SshException
	 */
	public int listChildren(List<SftpFile> children) throws SftpStatusException, SshException {

		checkValidHandle();
		
		try {
			UnsignedInteger32 requestId = sftp.nextRequestId();
			Packet msg = sftp.createPacket();
			msg.write(SftpChannel.SSH_FXP_READDIR);
			msg.writeInt(requestId.longValue());
			msg.writeBinaryString(handle);

			sftp.sendMessage(msg);

			if (Log.isDebugEnabled()) {
				Log.debug("Sending list children request");
			}
			SftpMessage bar = sftp.getResponse(requestId);

			try {
				if (bar.getType() == SftpChannel.SSH_FXP_NAME) {

					if (Log.isDebugEnabled()) {
						Log.debug("Received results");
					}

					SftpFile[] files = sftp.extractFiles(bar, file.getAbsolutePath());

					if (Log.isDebugEnabled()) {
						Log.debug("THere are {} results in this packet", files.length);
					}

					for (int i = 0; i < files.length; i++) {
						children.add(files[i]);
					}
					return files.length;
				} else if (bar.getType() == SftpChannel.SSH_FXP_STATUS) {
					int status = (int) bar.readInt();

					if (Log.isDebugEnabled()) {
						Log.debug("Received status {}", status);
					}
					if (status == SftpStatusException.SSH_FX_EOF) {
						return -1;
					}

					if (sftp.version >= 3) {
						String desc = bar.readString();
						throw new SftpStatusException(status, desc);
					}
					throw new SftpStatusException(status);

				} else {
					close();
					throw new SshException("The server responded with an unexpected message",
							SshException.CHANNEL_FAILURE);
				}
			} finally {
				bar.release();
			}
		} catch (SshIOException ex) {
			throw ex.getRealException();
		} catch (IOException ex) {
			throw new SshException(ex);
		}

	}

	/**
	 * Sets the attributes of a file.
	 * 
	 * @param handle the file object.
	 * @param attrs  the new attributes.
	 * 
	 * @throws SshException
	 */
	public void setAttributes(SftpFileAttributes attrs) throws SftpStatusException, SshException {

		checkValidHandle();
		
		try {
			UnsignedInteger32 requestId = sftp.nextRequestId();
			Packet msg = sftp.createPacket();
			msg.write(SftpChannel.SSH_FXP_FSETSTAT);
			msg.writeInt(requestId.longValue());
			msg.writeBinaryString(handle);
			msg.write(attrs.toByteArray(sftp.getVersion()));

			sftp.sendMessage(msg);

			sftp.getOKRequestStatus(requestId);
		} catch (SshIOException ex) {
			throw ex.getRealException();
		} catch (IOException ex) {
			throw new SshException(ex);
		}
	}

	/**
	 * Get the attributes of a file.
	 * 
	 * @param handle
	 * @return SftpFileAttributes
	 * @throws SftpStatusException , SshException
	 */
	public SftpFileAttributes getAttributes() throws SftpStatusException, SshException {

		checkValidHandle();
		
		try {
			UnsignedInteger32 requestId = sftp.nextRequestId();
			Packet msg = sftp.createPacket();
			msg.write(SftpChannel.SSH_FXP_FSTAT);
			msg.writeInt(requestId.longValue());
			msg.writeBinaryString(handle);
			if (sftp.version > 3) {
				msg.writeInt(SftpFileAttributes.SSH_FILEXFER_ATTR_SIZE
						| SftpFileAttributes.SSH_FILEXFER_ATTR_PERMISSIONS
						| SftpFileAttributes.SSH_FILEXFER_ATTR_ACCESSTIME
						| SftpFileAttributes.SSH_FILEXFER_ATTR_CREATETIME
						| SftpFileAttributes.SSH_FILEXFER_ATTR_MODIFYTIME | SftpFileAttributes.SSH_FILEXFER_ATTR_ACL
						| SftpFileAttributes.SSH_FILEXFER_ATTR_OWNERGROUP
						| SftpFileAttributes.SSH_FILEXFER_ATTR_SUBSECOND_TIMES
						| SftpFileAttributes.SSH_FILEXFER_ATTR_EXTENDED);
			}
			sftp.sendMessage(msg);

			SftpMessage attrMessage = sftp.getResponse(requestId);
			try {
				return sftp.extractAttributes(attrMessage);
			} finally {
				attrMessage.release();
			}
		} catch (SshIOException ex) {
			throw ex.getRealException();
		} catch (IOException ex) {
			throw new SshException(ex);
		}

	}

	/**
	 * Performs an optimized write of a file through asynchronous messaging and
	 * through buffering the local file into memory.
	 * 
	 * @param blocksize           the block size to send data, should be between
	 *                            4096 and 65536
	 * @param outstandingRequests the maximum number of requests that can be
	 *                            outstanding at any one time
	 * @param in                  the InputStream to read from
	 * @param buffersize          the size of the temporary buffer to read from the
	 *                            InputStream. Data is buffered into a temporary
	 *                            buffer so that the number of local filesystem
	 *                            reads is reducted to a minimum. This increases
	 *                            performance and so the buffer size should be as
	 *                            high as possible. The default operation, if
	 *                            buffersize <= 0 is to allocate a buffer the same
	 *                            size as the blocksize, meaning no buffer
	 *                            optimization is performed.
	 * @param progress            provides progress information, may be null.
	 * @param position            the position in the file to start writing to.
	 * @throws SshException
	 */
	public void performOptimizedWrite(String filename, int blocksize, int maxAsyncRequests, java.io.InputStream in,
			int buffersize, FileTransferProgress progress, long position)
			throws SftpStatusException, SshException, TransferCancelledException {

		checkValidHandle();
		
		long started = System.currentTimeMillis();
		long transfered = position;

		try {
			if (blocksize > 0 && blocksize < 4096) {
				throw new SshException("Block size cannot be less than 4096", SshException.BAD_API_USAGE);
			}

			if (blocksize <= 0 || blocksize > 65536) {
				blocksize = sftp.getSession().getMaximumRemotePacketLength() - 13;
			} else if (blocksize + 13 > sftp.getSession().getMaxiumRemotePacketSize()) {
				blocksize = sftp.getSession().getMaximumRemotePacketLength() - 13;
			}

			int calculatedRequestsMax = (int) ((sftp.getSession().getRemoteWindow().longValue() * 0.9D) / blocksize);

			if (maxAsyncRequests <= 0) {
				maxAsyncRequests = calculatedRequestsMax;
			}

			System.setProperty("maverick.write.optimizedBlock", String.valueOf(blocksize));
			System.setProperty("maverick.write.asyncRequestsMax", String.valueOf(maxAsyncRequests));

			if (Log.isTraceEnabled()) {
				Log.trace("Performing optimized write length=" + in.available() + " postion=" + position + " blocksize="
						+ blocksize + " maxAsyncRequests=" + maxAsyncRequests);
			}

			if (position < 0)
				throw new SshException("Position value must be greater than zero!", SshException.BAD_API_USAGE);

			if (position > 0) {
				if (progress != null)
					progress.progressed(position);
			}

			if (buffersize <= 0) {
				buffersize = blocksize;
			}

			byte[] buf = new byte[blocksize];

			int buffered = 0;

			buffered = in.read(buf);
			if (buffered != -1) {

				long time = System.currentTimeMillis();
				sftp.writeFile(handle, new UnsignedInteger64(position), buf, 0, buffered);
				time = System.currentTimeMillis() - time;

				System.setProperty("maverick.write.blockRoundtrip", String.valueOf(time));

				transfered += buffered;

				if (progress != null) {
					if (progress.isCancelled())
						throw new TransferCancelledException();
					progress.progressed(transfered);
				}

				Vector<UnsignedInteger32> requests = new Vector<UnsignedInteger32>();
				// BufferedInputStream is not in J2ME, whatever type of input stream
				// has been passed in can be used in conjunction with the abstract
				// InputStream class.
				in = new BufferedInputStream(in, buffersize);

				while (true) {

					buffered = in.read(buf);
					if (buffered == -1)
						break;

					requests.addElement(postWriteRequest(transfered, buf, 0, buffered));

					transfered += buffered;

					if (progress != null) {

						if (progress.isCancelled())
							throw new TransferCancelledException();

						progress.progressed(transfered);
					}

					if (requests.size() > maxAsyncRequests) {
						sftp.requestId = (UnsignedInteger32) requests.elementAt(0);
						requests.removeElementAt(0);
						sftp.getOKRequestStatus(sftp.requestId);

					}

				}

				while (requests.size() > 0) {
					sftp.getOKRequestStatus(requests.remove(0));
				}
			}

		} catch (IOException ex) {
			throw new TransferCancelledException();
		} catch (OutOfMemoryError ex) {
			throw new SshException("Resource Shortage: try reducing the local file buffer size",
					SshException.BAD_API_USAGE);
		} finally {
			long finished = System.currentTimeMillis();
			long transferTime = finished - started;
			double seconds = transferTime > 1000 ? transferTime / 1000 : 1D;
			if (Log.isInfoEnabled()) {
				if (transfered > 0) {
					Log.info("Optimized write of {} to {} took {} seconds at {} per second",
							IOUtils.toByteSize(transfered), filename, seconds,
							IOUtils.toByteSize(transfered / seconds, 1));
				} else {
					Log.info("Optimized write did not transfer any data");
				}
			}
		}

	}

	/**
	 * Read a block of data from an open file.
	 * 
	 * @param handle the open file handle
	 * @param offset the offset to start reading in the file
	 * @param output a buffer to write the returned data to
	 * @param off    the starting offset in the output buffer
	 * @param len    the length of data to read
	 * @return int
	 * @throws SshException
	 */
	public int readFile(UnsignedInteger64 offset, byte[] output, int off, int len)
			throws SftpStatusException, SshException {

		checkValidHandle();
		
		try {
			if ((output.length - off) < len) {
				throw new IndexOutOfBoundsException("Output array size is smaller than read length!");
			}

			UnsignedInteger32 requestId = sftp.nextRequestId();
			Packet msg = sftp.createPacket();
			msg.write(SftpChannel.SSH_FXP_READ);
			msg.writeInt(requestId.longValue());
			msg.writeBinaryString(handle);
			msg.write(offset.toByteArray());
			msg.writeInt(len);

			sftp.sendMessage(msg);

			SftpMessage bar = sftp.getResponse(requestId);

			try {
				if (bar.getType() == SftpChannel.SSH_FXP_DATA) {
					byte[] msgdata = bar.readBinaryString();
					System.arraycopy(msgdata, 0, output, off, msgdata.length);

					if (Log.isDebugEnabled()) {
						Log.debug("Received SSH_FXP_DATA channel={} requestId={} offset={} blocksize={}",
								sftp.getSession().getLocalId(), requestId.toString(), offset.toString(), msgdata.length);
					}
					return msgdata.length;
				} else if (bar.getType() == SftpChannel.SSH_FXP_STATUS) {
					int status = (int) bar.readInt();
					if (status == SftpStatusException.SSH_FX_EOF)
						return -1;
					if (sftp.getVersion() >= 3) {
						String desc = bar.readString();
						throw new SftpStatusException(status, desc);
					}
					throw new SftpStatusException(status);
				} else {
					close();
					throw new SshException("The server responded with an unexpected message",
							SshException.CHANNEL_FAILURE);
				}
			} finally {
				bar.release();
			}
		} catch (SshIOException ex) {
			throw ex.getRealException();
		} catch (IOException ex) {
			throw new SshException(ex);
		}

	}

	/**
	 * Post a read request to the server and return the request id; this is used to
	 * optimize file downloads. In normal operation the files are transfered by
	 * using a synchronous set of requests, however this slows the download as the
	 * client has to wait for the servers response before sending another request.
	 * 
	 * @param handle
	 * @param offset
	 * @param len
	 * @return UnsignedInteger32
	 * @throws SshException
	 */
	public UnsignedInteger32 postReadRequest(long offset, int len) throws SftpStatusException, SshException {

		checkValidHandle();
		
		try {
			UnsignedInteger32 requestId = sftp.nextRequestId();
			Packet msg = sftp.createPacket();
			msg.write(SftpChannel.SSH_FXP_READ);
			msg.writeInt(requestId.longValue());
			msg.writeBinaryString(handle);
			msg.writeUINT64(offset);
			msg.writeInt(len);

			if (Log.isDebugEnabled()) {
				Log.debug("Sending SSH_FXP_READ channel={} requestId={} offset={} blocksize={}", sftp.getSession().getLocalId(),
						requestId.toString(), offset, len);
			}
			sftp.sendMessage(msg);

			return requestId;
		} catch (SshIOException ex) {
			throw ex.getRealException();
		} catch (IOException ex) {
			throw new SshException(ex);
		}

	}

	/**
	 * Performs an optimized read of a file through use of asynchronous messages.
	 * The total number of outstanding read requests is configurable. This should be
	 * safe on file objects as the SSH protocol states that file read operations
	 * should return the exact number of bytes requested in each request. However
	 * the server is not required to return the exact number of bytes on device
	 * files and so this method should not be used for device files.
	 * 
	 * @param handle              the open files handle
	 * @param length              the amount of the file file to be read, equal to
	 *                            the file length when reading the whole file
	 * @param blocksize           the blocksize to read
	 * @param out                 an OutputStream to output the file into
	 * @param outstandingRequests the maximum number of read requests to
	 * @param progress
	 * @param position            the postition from which to start reading the file
	 * @throws SshException
	 */
	public void performOptimizedRead(String filename, long length, int blocksize, OutputStream out,
			int outstandingRequests, FileTransferProgress progress, long position)
			throws SftpStatusException, SshException, TransferCancelledException {

		checkValidHandle();
		
		long transfered = 0;
		boolean reachedEOF = false;
		long started = System.currentTimeMillis();

		if (blocksize > 0 && blocksize < 4096) {
			throw new SshException("Block size cannot be less than 4096", SshException.BAD_API_USAGE);
		}

		if (blocksize <= 0 || blocksize > 65536) {
			blocksize = sftp.getSession().getMaximumLocalPacketLength() - 13;
		} else if (blocksize + 13 > sftp.getSession().getMaximumLocalPacketLength()) {
			blocksize = sftp.getSession().getMaximumLocalPacketLength() - 13;
		}

		int calculatedRequests = (int) ((sftp.getSession().getMaximumWindowSpace().longValue() * 0.9D) / blocksize);
		if (outstandingRequests <= 0 || calculatedRequests < outstandingRequests) {
			outstandingRequests = calculatedRequests / 2;
		}

		System.setProperty("maverick.read.optimizedBlock", String.valueOf(blocksize));
		System.setProperty("maverick.read.asyncRequests", String.valueOf(outstandingRequests));

		if (Log.isTraceEnabled()) {
			Log.trace("Performing optimized read length=" + length + " postion=" + position + " blocksize=" + blocksize
					+ " outstandingRequests=" + outstandingRequests);
		}

		if (length <= 0) {
			// We cannot perform an optimised read on this file since we don't
			// know its length so
			// here we assume its very large
			length = Long.MAX_VALUE;
		}

		MessageDigest md5 = null;
		OutputStream originalStream = null;

		if (performVerification) {
			try {
				md5 = MessageDigest.getInstance("MD5");
			} catch (NoSuchAlgorithmException ex) {
				throw new SshException(ex);
			}
			originalStream = out;
			out = new DigestOutputStream(out, md5);
		}

		/**
		 * LDP - Obtain the first block using a synchronous call. We do this to
		 * determine if the server is conforming to the spec and returning as much data
		 * as we have asked for. If not we reconfigure the block size to the number of
		 * bytes returned.
		 */

		if (position < 0) {
			throw new SshException("Position value must be greater than zero!", SshException.BAD_API_USAGE);
		}

		try {
			byte[] tmp = new byte[blocksize];

			long time = System.currentTimeMillis();
			int i = readFile(new UnsignedInteger64(0), tmp, 0, tmp.length);
			time = System.currentTimeMillis() - time;

			System.setProperty("maverick.read.blockRoundtrip", String.valueOf(time));

			// if i=-1 then eof so return, maybe should throw exception on null
			// files?
			if (i == -1) {
				return;
			}
			// if the first block contains required data, write to the output
			// buffer,
			// write the portion of tmp needed to out
			// change position
			if (i > position) {
				try {
					out.write(tmp, (int) position, (int) (i - position));
				} catch (IOException e) {
					throw new TransferCancelledException();
				}
				length = length - (i - position);
				transfered += (i - position);
				if (progress != null) {
					progress.progressed(transfered);
				}
				position = i;

			}

			// if the first block contains the whole portion of the file to be
			// read, then return
			if ((position + length) <= i) {
				return;
			}

			// reconfigure the blocksize if necessary
			if (i < blocksize && length > i) {
				blocksize = i;
				System.setProperty("maverick.read.optimizedBlock", String.valueOf(blocksize));
			}

			Vector<UnsignedInteger32> requests = new Vector<UnsignedInteger32>(outstandingRequests);

			int osr = calculatedRequests;

			long offset = position;
			UnsignedInteger32 requestId;
			int dataLen;

			while (true) {

				while (requests.size() < osr) {

					if (i > 0 && sftp.getSession().getRemoteWindow().longValue() < 29) {
						if (Log.isDebugEnabled())
							Log.debug("Deferring post requests due to lack of remote window");
						break;
					}
					if (Log.isTraceEnabled())
						Log.trace("Posting request for file offset " + offset);

					requests.addElement(postReadRequest(offset, blocksize));
					offset += blocksize;

					if (progress != null && progress.isCancelled()) {
						throw new TransferCancelledException();
					}
				}

				requestId = (UnsignedInteger32) requests.elementAt(0);
				requests.removeElementAt(0);
				SftpMessage bar = sftp.getResponse(requestId);
				try {
					if (bar.getType() == SftpChannel.SSH_FXP_DATA) {
						dataLen = (int) bar.readInt();

						if (Log.isTraceEnabled())
							Log.trace("Got " + dataLen + " bytes of data");

						try {
							out.write(bar.array(), bar.getPosition(), dataLen);
						} catch (IOException e) {
							throw new TransferCancelledException();
						}
						transfered += dataLen;
						if (progress != null) {
							progress.progressed(transfered);
						}
					} else if (bar.getType() == SftpChannel.SSH_FXP_STATUS) {
						int status = (int) bar.readInt();
						if (status == SftpStatusException.SSH_FX_EOF) {

							if (Log.isTraceEnabled())
								Log.trace("Received file EOF");
							reachedEOF = true; // Hack for bad servers
							return;
						}
						if (sftp.version >= 3) {
							String desc = bar.readString();

							if (Log.isTraceEnabled())
								Log.trace("Received status " + desc);

							throw new SftpStatusException(status, desc);
						}

						if (Log.isTraceEnabled())
							Log.trace("Received status " + status);

						throw new SftpStatusException(status);
					} else {
						throw new SshException("The server responded with an unexpected message",
								SshException.CHANNEL_FAILURE);
					}
				} catch (IOException ex) {
					throw new SshException("Failed to read expected data from server response",
							SshException.CHANNEL_FAILURE);
				} finally {
					bar.release();
				}

				if (osr < calculatedRequests) {
					osr++;
				}
			}

		} finally {

			long finished = System.currentTimeMillis();
			long transferTime = finished - started;
			double seconds = transferTime > 1000 ? transferTime / 1000 : 1D;
			if (transfered > 0) {
				Log.info("Optimized read of {} from {} took seconds {} at {} per second",
						IOUtils.toByteSize(transfered), filename, seconds, IOUtils.toByteSize(transfered / seconds, 1));
			} else {
				Log.info("Optimized read did not transfer any data");
			}

			if (reachedEOF && performVerification && transfered > 0) {
				try {
					out.flush();
					out.close();
					try {
						originalStream.close();
					} catch (IOException e) {
					}

					byte[] digest = md5.digest();

					ByteArrayWriter baw = new ByteArrayWriter();

					try {
						baw.writeBinaryString(handle);
						baw.writeUINT64(0);
						baw.writeUINT64(transfered);
						baw.writeBinaryString(new byte[0]);

						SftpMessage reply = sftp.getExtensionResponse(
								sftp.sendExtensionMessage("md5-hash-handle", baw.toByteArray()));

						reply.readString();
						byte[] remoteDigest = reply.readBinaryString();

						if (!Arrays.equals(digest, remoteDigest)) {
							throw new SshException("Remote file digest does not match local digest",
									SshException.POSSIBLE_CORRUPT_FILE);
						}
					} finally {
						baw.close();
					}
				} catch (IOException e) {
					Log.error("Error processing remote digest", e);
				} catch (SftpStatusException e) {
					if (e.getStatus() == SftpStatusException.SSH_FX_OP_UNSUPPORTED) {
						performVerification = false;
					} else {
						Log.error("Could not verify file", e);
					}
				} catch (SshException e) {
					if (reachedEOF && e.getReason() == SshException.POSSIBLE_CORRUPT_FILE) {
						throw e;
					} else if (!reachedEOF) {
						throw e;
					}
				}
			}
		}
	}

	/**
	 * Perform a synchronous read of a file from the remote file system. This
	 * implementation waits for acknowledgement of every data packet before
	 * requesting additional data.
	 * 
	 * @param handle
	 * @param blocksize
	 * @param out
	 * @param progress
	 * @param position
	 * @throws SftpStatusException
	 * @throws SshException
	 * @throws TransferCancelledException
	 */
	public void performSynchronousRead(int blocksize, OutputStream out, FileTransferProgress progress, long position)
			throws SftpStatusException, SshException, TransferCancelledException {

		checkValidHandle();
		
		if (Log.isTraceEnabled())
			Log.trace("Performing synchronous read postion=" + position + " blocksize=" + blocksize);

		if (blocksize < 1 || blocksize > 65536) {
			blocksize = sftp.getSession().getMaximumRemotePacketLength() - 13;
		} else if (blocksize + 13 < sftp.getSession().getMaxiumRemotePacketSize()) {
			blocksize = sftp.getSession().getMaximumLocalPacketLength() - 13;
		}

		if (Log.isInfoEnabled()) {
			Log.info("Optimised block size will be {}", blocksize);
		}

		if (position < 0) {
			throw new SshException("Position value must be greater than zero!", SshException.BAD_API_USAGE);
		}

		byte[] tmp = new byte[blocksize];

		int read;
		UnsignedInteger64 offset = new UnsignedInteger64(position);

		if (position > 0) {
			if (progress != null)
				progress.progressed(position);
		}

		try {
			while ((read = readFile(offset, tmp, 0, tmp.length)) > -1) {
				if (progress != null && progress.isCancelled()) {
					throw new TransferCancelledException();
				}
				out.write(tmp, 0, read);
				offset = UnsignedInteger64.add(offset, read);
				if (progress != null)
					progress.progressed(offset.longValue());
			}
		} catch (IOException e) {
			throw new SshException(e);
		}
	}

	/**
	 * Send a write request for an open file but do not wait for the response from
	 * the server.
	 * 
	 * @param handle
	 * @param position
	 * @param data
	 * @param off
	 * @param len
	 * @return UnsignedInteger32
	 * @throws SshException
	 */
	public UnsignedInteger32 postWriteRequest(long position, byte[] data, int off, int len)
			throws SftpStatusException, SshException {
		checkValidHandle();
		
		if ((data.length - off) < len) {
			throw new IndexOutOfBoundsException("Incorrect data array size!");
		}

		try {
			UnsignedInteger32 requestId = sftp.nextRequestId();
			Packet msg = sftp.createPacket();
			msg.write(SftpChannel.SSH_FXP_WRITE);
			msg.writeInt(requestId.longValue());
			msg.writeBinaryString(handle);
			msg.writeUINT64(position);
			msg.writeBinaryString(data, off, len);

			sftp.sendMessage(msg);

			return requestId;
		} catch (SshIOException ex) {
			throw ex.getRealException();
		} catch (IOException ex) {
			throw new SshException(ex);
		}
	}

	SftpChannel getSFTPChannel() {
		return sftp;
	}
	
	private void checkValidHandle() throws SftpStatusException {
		if (closed) {
			throw new SftpStatusException(SftpStatusException.INVALID_HANDLE,
					"The handle is not an open file handle!");
		}	
	}

	
}

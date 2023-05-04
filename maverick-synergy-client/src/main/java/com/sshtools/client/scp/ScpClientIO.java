/*
 * (c) 2002-2023 JADAPTIVE Limited. All Rights Reserved.
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
package com.sshtools.client.scp;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.sshtools.client.SessionChannelNG;
import com.sshtools.client.SshClient;
import com.sshtools.client.tasks.FileTransferProgress;
import com.sshtools.common.ssh.ChannelOpenException;
import com.sshtools.common.ssh.RequestFuture;
import com.sshtools.common.ssh.SessionChannel;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.SshIOException;

/**
 * <p>
 * Implements the IO of a Secure Copy (SCP) client. This has no dependencies
 * upon Files.
 * </p>
 * 
 * @author Lee David Painter
 */
public class ScpClientIO {
	protected SshClient ssh;
	boolean first = true;
	
	/**
	 * <p>
	 * Creates an SCP client.
	 * </p>
	 * 
	 * @param ssh
	 *            a connected SshClient
	 */
	public ScpClientIO(SshClient ssh) {
		this.ssh = ssh;
	}

	/**
	 * <p>
	 * Uploads a <code>java.io.InputStream</code> to a remote server as a file.
	 * You <strong>must</strong> supply the correct number of bytes that will be
	 * written.
	 * </p>
	 * 
	 * @param in
	 *            stream providing file
	 * @param length
	 *            number of bytes that will be written
	 * @param localFile
	 *            local file name
	 * @param remoteFile
	 *            remote file name
	 * 
	 * @throws IOException
	 *             on any error
	 */
	public void put(InputStream in, long length, String localFile,
			String remoteFile) throws SshException, ChannelOpenException {
		put(in, length, localFile, remoteFile, false, null);
	}

	/**
	 * <p>
	 * Uploads a <code>java.io.InputStream</code> to a remote server as a file.
	 * You <strong>must</strong> supply the correct number of bytes that will be
	 * written.
	 * </p>
	 * 
	 * @param in
	 *            stream providing file
	 * @param length
	 *            number of bytes that will be written
	 * @param localFile
	 *            local file name
	 * @param remoteFile
	 *            remote file name
	 * @param progress
	 *            a file transfer progress implementation
	 * 
	 * @throws IOException
	 *             on any error
	 */
	public void put(InputStream in, long length, String localFile,
			String remoteFile, FileTransferProgress progress)
			throws SshException, ChannelOpenException {
		put(in, length, localFile, remoteFile, false, progress);
		
	}
	
	/**
	 * 
	 * @param in
	 * @param length
	 * @param localFile
	 * @param remoteFile
	 * @param remoteIsDir
	 * @param progress
	 * @throws SshException
	 * @throws ChannelOpenException
	 */
	public void put(InputStream in, long length, String localFile,
			String remoteFile, boolean remoteIsDir, FileTransferProgress progress)
			throws SshException, ChannelOpenException {
		ScpEngineIO scp = new ScpEngineIO("scp " + (remoteIsDir ? "-d " : "") + "-t " /* + (verbose ? "-v " : "") */
				+ remoteFile, ssh.openSessionChannel());
		try {

			scp.waitForResponse();

			if (progress != null) {
				progress.started(length, remoteFile);
			}

			scp.writeStreamToRemote(in, length, localFile, progress);

			if (progress != null)
				progress.completed();

			scp.close();
		} catch (IOException ex) {
			scp.close();
			throw new SshException(ex, SshException.CHANNEL_FAILURE);
		}
	}

	/**
	 * <p>
	 * Gets a remote file as a <code>java.io.InputStream</code>.
	 * </p>
	 * 
	 * @param remoteFile
	 *            remote file name
	 * @return ScpInputStream
	 * 
	 * @throws IOException
	 *             on any error
	 */
	public InputStream get(String remoteFile) throws SshException,
			ChannelOpenException {
		return get(remoteFile, null);
	}

	/**
	 * <p>
	 * Gets a remote file as a <code>java.io.InputStream</code>.
	 * </p>
	 * 
	 * @param remoteFile
	 *            remote file name
	 * @param progress
	 *            a file transfer progress implementation.
	 * @return ScpInputStream
	 * 
	 * @throws IOException
	 *             on any error
	 */
	public InputStream get(String remoteFile, FileTransferProgress progress)
			throws SshException, ChannelOpenException {

		ScpEngineIO scp = new ScpEngineIO("scp " + "-f "
		/* + (verbose ? "-v " : "") */
		+ remoteFile, ssh.openSessionChannel());
		try {
			return scp.readStreamFromRemote(remoteFile, progress);
		} catch (IOException ex) {
			scp.close();
			throw new SshException(ex, SshException.CHANNEL_FAILURE);
		}
	}

	/**
	 * <p>
	 * Implements an SCP engine.
	 * </p>
	 */
	public class ScpEngineIO {
		protected byte[] buffer = new byte[32768];
		protected String cmd;
		protected SessionChannel session;
		protected OutputStream out;
		protected InputStream in;

		/**
		 * <p>
		 * Contruct the channel with the specified scp command.
		 * </p>
		 * 
		 * @param cmd
		 *            the scp command
		 * @param session
		 *            the session to scp over
		 */
		protected ScpEngineIO(String cmd, SessionChannelNG session)
				throws SshException {

			this.session = session;
			this.cmd = cmd;
			this.in = session.getInputStream();
			this.out = session.getOutputStream();
			RequestFuture future = session.executeCommand(cmd);
			future.waitFor(10000);
			if(!future.isSuccess()) {
				session.close();
				throw new SshException("Failed to execute the command "
						+ cmd, SshException.CHANNEL_FAILURE);
			}
			
		}

		/**
		 * Close the SCP engine and underlying session.
		 * 
		 * @throws SshException
		 */
		public void close() throws SshException {
			try {
				session.getOutputStream().close();
			} catch (IOException ex) {
				throw new SshException(ex);
			}

			// This helps some Cisco routers operate correctly.. otherwise they
			// moan about disconnection errors!!
			try {
				Thread.sleep(500);
			} catch (Throwable t) {
			}

			session.close();
		}

		/**
		 * <p>
		 * Write a stream as a file to the remote server. You
		 * <strong>must</strong> supply the correct number of bytes that will be
		 * written.
		 * </p>
		 * 
		 * @param in
		 *            stream
		 * @param length
		 *            number of bytes to write
		 * @param localName
		 *            local file name
		 * 
		 * @throws IOException
		 *             if an IO error occurs
		 */
		protected void writeStreamToRemote(InputStream in, long length,
				String localName, FileTransferProgress progress)
				throws IOException {
			String cmd = "C0644 " + length + " " + localName + "\n";
			out.write(cmd.getBytes());

			waitForResponse();

			writeCompleteFile(in, length, progress);

			writeOk();

			waitForResponse();
		}

		/**
		 * Open an InputStream.
		 * 
		 * @param remoteFile
		 * @param progress
		 * @return ScpInputStream
		 * @throws IOException
		 */
		protected InputStream readStreamFromRemote(String remoteFile,
				FileTransferProgress progress) throws IOException {
			String cmd;
			String[] cmdParts = new String[3];
			writeOk();

			while (true) {
				try {
					cmd = readString();
				} catch (EOFException e) {
					return null;
				} catch (SshIOException e2) {
					return null;
				}

				char cmdChar = cmd.charAt(0);

				switch (cmdChar) {
				case 'E':
					writeOk();

					return null;

				case 'T':
					continue;

				case 'D':
					throw new IOException(
							"Directories cannot be copied to a stream");

				case 'C':
					parseCommand(cmd, cmdParts);
					long len = Long.parseLong(cmdParts[1]);

					writeOk();

					if (progress != null) {
						progress.started(len, remoteFile);
					}

					return new ScpInputStream(len, in, this, progress,
							remoteFile) /*
										 * , 16 * 1024)
										 */
					;

				default:
					writeError("Unexpected cmd: " + cmd);
					throw new IOException("SCP unexpected cmd: " + cmd);
				}
			}
		}

		/**
		 * Parse an SCP command
		 * 
		 * @param cmd
		 * @param cmdParts
		 * @throws IOException
		 */
		protected void parseCommand(String cmd, String[] cmdParts)
				throws IOException {
			int l;
			int r;
			l = cmd.indexOf(' ');
			r = cmd.indexOf(' ', l + 1);

			// a command must have the following format
			// "commandtype source dest", and therefore must have 2 spaces
			if ((l == -1) || (r == -1)) {
				writeError("Syntax error in cmd");
				throw new IOException("Syntax error in cmd");
			}

			// extract command
			cmdParts[0] = cmd.substring(1, l);
			// extract source
			cmdParts[1] = cmd.substring(l + 1, r);
			// extract destination
			cmdParts[2] = cmd.substring(r + 1);
		}

		/**
		 * read the inputstream until come to an end of line character '\n', and
		 * return the bytes read as a string
		 * 
		 * @return String
		 * @throws IOException
		 */
		protected String readString() throws IOException {
			int ch;
			int i = 0;

			while (((ch = in.read()) != ('\n')) && (ch >= 0)) {
				buffer[i++] = (byte) ch;
			}

			if (ch == -1) {
				throw new EOFException("Unexpected EOF");
			}

			if (buffer[0] == (byte) '\n') {
				throw new IOException("Unexpected <NL>");
			}

			// skip first character if it equals '\02' or '\01'
			if ((buffer[0] == (byte) '\02') || (buffer[0] == (byte) '\01')) {
				String msg = new String(buffer, 1, i - 1);

				if (buffer[0] == (byte) '\02') {
					throw new IOException(msg);
				}

				throw new IOException("SCP returned an unexpected error: "
						+ msg);
			}

			if(buffer[0]==0) {
				System.out.println("GOT ZERO AT 0 INDEX");
			}
			return new String(buffer, 0, i);
		}

		public void waitForResponse() throws IOException {
			int r = in.read();

			if (first) {
				first = false;
			}
			
			if (r == 0) {
				// All is well, no error
				return;
			}

			if (r == -1) {
				throw new EOFException("SCP returned unexpected EOF");
			}

			String msg = readString();

			if (r == (byte) '\02') {
				throw new IOException(msg);
			}

			throw new IOException("SCP returned an unexpected error: " + msg);
		}

		protected void writeOk() throws IOException {
			out.write(0);
		}

		protected void writeError(String reason) throws IOException {
			out.write(1);
			out.write(reason.getBytes());
		}

		protected void writeCompleteFile(InputStream in, long size,
				FileTransferProgress progress) throws IOException {
			long count = 0;
			int read;

			try {
				// write in blocks of buffer.length size
				while (count < size) {
					read = in
							.read(buffer,
									0,
									(int) (((size - count) < buffer.length) ? (size - count)
											: buffer.length));

					if (read == -1) {
						throw new EOFException("SCP received an unexpected EOF");
					}

					count += read;

					out.write(buffer, 0, read);

					if (progress != null) {

						if (progress.isCancelled())
							throw new SshIOException(new SshException(
									"SCP transfer was cancelled by user",
									SshException.SCP_TRANSFER_CANCELLED));

						progress.progressed(count);

					}
				}
			} finally {
				in.close();
			}
		}

		protected void readCompleteFile(OutputStream out, long size,
				FileTransferProgress progress) throws IOException {
			long count = 0;
			int read;

			try {
				// read in blocks of buffer.length size
				while (count < size) {
					read = in
							.read(buffer,
									0,
									(int) (((size - count) < buffer.length) ? (size - count)
											: buffer.length));

					if (read == -1) {
						throw new EOFException("SCP received an unexpected EOF");
					}

					count += read;
					out.write(buffer, 0, read);

					if (progress != null) {
						if (progress.isCancelled())
							throw new SshIOException(new SshException(
									"SCP transfer was cancelled by user",
									SshException.SCP_TRANSFER_CANCELLED));

						progress.progressed(count);
					}
				}
			} finally {
				out.close();
			}
		}
	
	}
	static class ScpInputStream extends InputStream {
		long length;
		InputStream in;
		long count;
		ScpEngineIO engine;
		FileTransferProgress progress;
		String remoteFile;

		ScpInputStream(long length, InputStream in, ScpEngineIO engine,
				FileTransferProgress progress, String remoteFile) {
			this.length = length;
			this.in = in;
			this.engine = engine;
			this.progress = progress;
			this.remoteFile = remoteFile;
		}

		public int read() throws IOException {

			if (count == length) {
				return -1;
			}

			if (count >= length) {
				throw new EOFException("End of file.");
			}

			int r = in.read();

			if (r == -1) {
				throw new EOFException("Unexpected EOF.");
			}

			count++;

			if (count == length) {
				engine.waitForResponse();
				engine.writeOk();
				if (progress != null)
					progress.completed();

			}

			if (progress != null) {
				if (progress.isCancelled())
					throw new SshIOException(new SshException(
							"SCP transfer was cancelled by user",
							SshException.SCP_TRANSFER_CANCELLED));

				progress.progressed(count);
			}
			return r;
		}

		public int available() throws IOException {

			if (count == length)
				return 0;
			return (int) (length - count);
		}

		public long getFileSize() {
			return length;
		}

		public int read(byte[] buf, int off, int len) throws IOException {

			if (count >= length) {
				return -1;
			}

			int r = in.read(buf, off, (int) (length - count > len ? len
					: length - count));

			if (r == -1) {
				throw new EOFException("Unexpected EOF.");
			}

			count += r;

			if (count >= length) {
				engine.waitForResponse();
				engine.writeOk();
				if (progress != null)
					progress.completed();
			}

			if (progress != null) {
				if (progress.isCancelled())
					throw new SshIOException(new SshException(
							"SCP transfer was cancelled by user",
							SshException.SCP_TRANSFER_CANCELLED));

				progress.progressed(count);
			}
			return r;
		}

		public void close() throws IOException {

			try {
				engine.close();
			} catch (SshException ex) {
				throw new SshIOException(ex);
			}
		}
	}
}

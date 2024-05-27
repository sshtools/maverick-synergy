package com.sshtools.client;

import java.io.IOException;

import com.sshtools.common.nio.WriteOperationRequest;
import com.sshtools.common.ssh.ChannelOpenException;
import com.sshtools.common.ssh.ChannelRequestFuture;
import com.sshtools.common.ssh.RequestFuture;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;
import com.sshtools.common.util.UnsignedInteger32;
import com.sshtools.synergy.ssh.ChannelNG;
import com.sshtools.synergy.ssh.TerminalModes;

/**
 * Implements the client side of the SSH Connection protocol session channel
 */
public abstract  class AbstractSessionChannel extends ChannelNG<SshClientContext> {

	public static final int EXITCODE_NOT_RECEIVED = Integer.MIN_VALUE;
	public static final int SSH_EXTENDED_DATA_STDERR = 1;

	private int exitcode = EXITCODE_NOT_RECEIVED;
	private String exitsignalinfo;
	private boolean flowControlEnabled;
	private boolean singleSession = false;
	

	public AbstractSessionChannel(int maximumPacketSize, UnsignedInteger32 initialWindowSize, UnsignedInteger32 maximumWindowSpace,
			UnsignedInteger32 minimumWindowSpace, boolean autoConsume) {
		super("session", maximumPacketSize, initialWindowSize, maximumWindowSpace, minimumWindowSpace,
				new ChannelRequestFuture(), autoConsume);
	}

	public AbstractSessionChannel(int maximumPacketSize, UnsignedInteger32 initialWindowSize, UnsignedInteger32 maximumWindowSpace,
			UnsignedInteger32 minimumWindowSpace, ChannelRequestFuture closeFuture, boolean autoConsume) {
		super("session", maximumPacketSize, initialWindowSize, maximumWindowSpace, minimumWindowSpace, closeFuture, autoConsume);
	}

	public boolean isSingleSession() {
		return singleSession;
	}

	public void setSingleSession(boolean singleSession) {
		this.singleSession = singleSession;
	}

	@Override
	protected void onChannelFree() {

	}

	@Override
	protected byte[] createChannel() throws IOException {
		return null;
	}

	@Override
	protected byte[] openChannel(byte[] requestdata) throws WriteOperationRequest, ChannelOpenException {
		return null;
	}

	@Override
	protected void onChannelOpenConfirmation() {

	}

	@Override
	protected void onChannelError(Throwable e) {

	}
	
	@Override
	protected void onChannelClosed() {
		if(singleSession) {
			con.disconnect();
		}
	}

	@Override
	protected void onChannelOpen() {

	}
	
	public RequestFuture allocatePseudoTerminal(String type) {
		return allocatePseudoTerminal(type, 80, 25);
	}

	public RequestFuture allocatePseudoTerminal(String type, int cols, int rows) {
		return allocatePseudoTerminal(type, cols, rows, 0, 0, (TerminalModes)null);
	}

	public RequestFuture allocatePseudoTerminal(String type, int cols, int rows, TerminalModes modes) {
		return allocatePseudoTerminal(type, cols, rows, 0, 0, modes);
	}

	public void changeTerminalDimensions(int cols, int rows, int width, int height) {
		ByteArrayWriter request = new ByteArrayWriter();

		try {

			request.writeInt(cols);
			request.writeInt(rows);
			request.writeInt(width);
			request.writeInt(height);

			sendChannelRequest("window-change", false, request.toByteArray());

		} catch (IOException ex) {
			throw new IllegalStateException(ex.getMessage(), ex);
		} finally {
			try {
				request.close();
			} catch (IOException e) {
			}
		}
	}

	/**
	 * Send a signal to the remote process. A signal can be delivered to the
	 * remote process using this method, some systems may not implement signals.
	 * The signal name should be one of the following values: <blockquote>
	 * 
	 * <pre>
	 * ABRT
	 * ALRM
	 * FPE
	 * HUP
	 * ILL
	 * INT
	 * KILL
	 * PIPE
	 * QUIT
	 * SEGV
	 * TERM
	 * USR1
	 * USR2
	 * </pre>
	 * 
	 * </blockquote>
	 * 
	 * @param signal
	 * @return future
	 * @throws IOException
	 */
	public RequestFuture signal(String signal) {
		try(ByteArrayWriter request = new ByteArrayWriter()) {
			request.writeString(signal);
			ChannelRequestFuture future = new ChannelRequestFuture();
			sendChannelRequest("signal", true, request.toByteArray(), future);
			return future;
		} catch (IOException ex) {
			throw new IllegalStateException(ex.getMessage(), ex);
		} 
	}

	public RequestFuture allocatePseudoTerminal(String type, int cols, int rows, int width, int height,
			TerminalModes modes) {

		ByteArrayWriter request = new ByteArrayWriter();

		try {

			request.writeString(type);
			request.writeInt(cols);
			request.writeInt(rows);
			request.writeInt(width);
			request.writeInt(height);
			if (modes == null) {
				request.writeInt(0);
			} else {
				request.writeBinaryString(modes.toByteArray());
			}

			ChannelRequestFuture future = new ChannelRequestFuture();
			sendChannelRequest("pty-req", true, request.toByteArray(), future);
			return future;
		} catch (IOException ex) {
			throw new IllegalStateException(ex.getMessage(), ex);
		} finally {
			try {
				request.close();
			} catch (IOException e) {
			}
		}
	}
	
	public RequestFuture setEnvironmentVariable(String key, String val) {
		
		ByteArrayWriter request = new ByteArrayWriter();

		try {

			request.writeString(key);
			request.writeString(val);

			ChannelRequestFuture future = new ChannelRequestFuture();
			sendChannelRequest("env", true, request.toByteArray(), future);
			return future;
		} catch (IOException ex) {
			throw new IllegalStateException(ex.getMessage(), ex);
		} finally {
			try {
				request.close();
			} catch (IOException e) {
			}
		}
	}

	public RequestFuture startShell() {
		ChannelRequestFuture future = new ChannelRequestFuture();
		sendChannelRequest("shell", true, null, future);
		return future;
	}

	public RequestFuture executeCommand(String cmd) {

		ByteArrayWriter request = new ByteArrayWriter();

		try {
			request.writeString(cmd);
			ChannelRequestFuture future = new ChannelRequestFuture();
			sendChannelRequest("exec", true,  request.toByteArray(), future);
			return future;
		} catch (IOException ex) {
			throw new IllegalStateException(ex.getMessage(), ex);
		} finally {
			try {
				request.close();
			} catch (IOException e) {
			}
		}
	}

	public RequestFuture executeCommand(String cmd, String charset) {

		ByteArrayWriter request = new ByteArrayWriter();

		try {

			request.writeString(cmd, charset);
			ChannelRequestFuture future = new ChannelRequestFuture();
			sendChannelRequest("exec", true,  request.toByteArray(), future);
			return future;

		} catch (IOException ex) {
			throw new IllegalStateException(ex.getMessage(), ex);
		} finally {
			try {
				request.close();
			} catch (IOException e) {
			}
		}
	}

	public RequestFuture startSubsystem(String subsystem) {

		ByteArrayWriter request = new ByteArrayWriter();

		try {

			request.writeString(subsystem, "UTF-8");
			ChannelRequestFuture future = new ChannelRequestFuture();
			sendChannelRequest("subsystem", true,  request.toByteArray(), future);
			return future;

		} catch (IOException ex) {
			throw new IllegalStateException(ex.getMessage(), ex);
		} finally {
			try {
				request.close();
			} catch (IOException e) {
			}
		}

	}

	@Override
	protected void onChannelClosing() {

	}

	@Override
	protected void onChannelRequest(String requesttype, boolean wantreply, byte[] requestdata) {

		try {
			if (requesttype.equals("exit-status")) {
				if (requestdata != null) {
					exitcode = (int) ByteArrayReader.readInt(requestdata, 0);
				}
			}

			if (requesttype.equals("exit-signal")) {

				if (requestdata != null) {
					ByteArrayReader bar = new ByteArrayReader(requestdata, 0, requestdata.length);
					try {
						exitsignalinfo = "Signal=" + bar.readString() + " CoreDump=" + String.valueOf(bar.read() != 0)
								+ " Message=" + bar.readString();
					} finally {
						bar.close();
					}
				}

			}

			if (requesttype.equals("xon-xoff")) {
				flowControlEnabled = (requestdata != null && requestdata[0] != 0);
			}
		} catch (IOException ex) {
			throw new IllegalStateException("Unexpected I/O error reading channel request", ex);
		}
	}

	public int getExitCode() {
		return exitcode;
	}

	public boolean isFlowControlEnabled() {
		return flowControlEnabled;
	}

	public String getExitSignalInfo() {
		return exitsignalinfo;
	}

	@Override
	protected void onRemoteEOF() {

	}

	@Override
	protected void onLocalEOF() {

	}

	public int getMaximumRemotePacketLength() {
		return getRemotePacket();
	}

	public int getMaximumLocalPacketLength() {
		return getLocalPacket();
	}
}

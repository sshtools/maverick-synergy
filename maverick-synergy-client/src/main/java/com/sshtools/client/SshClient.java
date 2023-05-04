package com.sshtools.client;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Set;

import com.sshtools.client.tasks.AbstractCommandTask;
import com.sshtools.client.tasks.DownloadFileTask.DownloadFileTaskBuilder;
import com.sshtools.client.tasks.Task;
import com.sshtools.client.tasks.UploadFileTask.UploadFileTaskBuilder;
import com.sshtools.common.events.Event;
import com.sshtools.common.events.EventCodes;
import com.sshtools.common.events.EventListener;
import com.sshtools.common.forwarding.ForwardingPolicy;
import com.sshtools.common.logger.Log;
import com.sshtools.common.permissions.UnauthorizedException;
import com.sshtools.common.publickey.InvalidPassphraseException;
import com.sshtools.common.publickey.SshKeyUtils;
import com.sshtools.common.ssh.Channel;
import com.sshtools.common.ssh.ChannelEventListener;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.util.Utils;
import com.sshtools.synergy.nio.ConnectRequestFuture;
import com.sshtools.synergy.ssh.Connection;

public class SshClient implements Closeable {

	public static final int EXIT_CODE_NOT_RECEIVED = AbstractCommandTask.EXIT_CODE_NOT_RECEIVED;
	
	Connection<SshClientContext> con;
	SshClientContext sshContext;
	String remotePublicKeys = "";
	String hostname;
	int port;
	boolean closeConnection = true;
	
	public SshClient(String hostname, int port, String username, long connectTimeout, char[] password) throws IOException, SshException {
		this(hostname, port, username, new SshClientContext(), connectTimeout, password);
	}
	
	public SshClient(String hostname, int port, String username, char[] password) throws IOException, SshException {
		this(hostname, port, username, new SshClientContext(), 30000L, password);
	}
	
	public SshClient(String hostname, int port, String username, char[] password, SshClientContext context) throws IOException, SshException {
		this(hostname, port, username, context, 30000L, password);
	}
	
	public SshClient(String hostname, int port, String username, long connectTimeout, File key) throws IOException, SshException, InvalidPassphraseException {
		this(hostname, port, username, connectTimeout, key, null);
	}
	
	public SshClient(String hostname, int port, String username, File key) throws IOException, SshException, InvalidPassphraseException {
		this(hostname, port, username, 30000L, key, null);
	}
	
	public SshClient(String hostname, int port, String username, long connectTimeout, File key, String passphrase) throws IOException, SshException, InvalidPassphraseException {
		this(hostname, port, username, connectTimeout, SshKeyUtils.getPrivateKey(key, passphrase));
	}
	
	public SshClient(String hostname, int port, String username,  File key, String passphrase) throws IOException, SshException, InvalidPassphraseException {
		this(hostname, port, username, 30000L, SshKeyUtils.getPrivateKey(key, passphrase));
	}
	
	public SshClient(String hostname, int port, String username, long connectTimeout, SshKeyPair... identities) throws IOException, SshException, InvalidPassphraseException {
		this(hostname, port, username, new SshClientContext(), connectTimeout, identities);
	}
	
	public SshClient(String hostname, int port, String username, SshKeyPair... identities) throws IOException, SshException, InvalidPassphraseException {
		this(hostname, port, username, new SshClientContext(), 30000L, identities);
	}

	public SshClient(String hostname, int port, String username, SshClientContext sshContext, long connectTimeout, SshKeyPair... identities) throws IOException, SshException {
		this(hostname, port, username, sshContext, connectTimeout, null, identities);
	}
	
	public SshClient(String hostname, int port, String username, SshClientContext sshContext, SshKeyPair... identities) throws IOException, SshException {
		this(hostname, port, username, sshContext, 30000L, null, identities);
	}
	
	public SshClient(String hostname, int port, String username, long connectTimeout, char[] password, SshKeyPair... identities) throws IOException, SshException {
		this(hostname, port, username, new SshClientContext(), connectTimeout, password, identities);
	}
	
	public SshClient(String hostname, int port, String username, char[] password, SshKeyPair... identities) throws IOException, SshException {
		this(hostname, port, username, new SshClientContext(), 30000L, password, identities);
	}

	public SshClient(String hostname, Integer port, String username, long connectTimeout, char[] password, File key, String passphrase) throws IOException, SshException, InvalidPassphraseException {
		this(hostname, port, username, connectTimeout, password, SshKeyUtils.getPrivateKey(key, passphrase));
	}
	
	public SshClient(String hostname, Integer port, String username, char[] password, File key, String passphrase) throws IOException, SshException, InvalidPassphraseException {
		this(hostname, port, username, 30000L, password, SshKeyUtils.getPrivateKey(key, passphrase));
	}
	
	public SshClient(String hostname, Integer port, String username, long connectTimeout) throws IOException, SshException {
		this(hostname, port, username, new SshClientContext(), connectTimeout);
	}
	
	public SshClient(String hostname, Integer port, String username) throws IOException, SshException {
		this(hostname, port, username, new SshClientContext(), 30000L);
	}

	public SshClient(String hostname, Integer port, String username, SshClientContext sshContext, long connectTimeout) throws IOException, SshException {
		this(hostname, port, username, sshContext, connectTimeout, (char[])null);
	}
	
	public SshClient(String hostname, Integer port, String username, SshClientContext sshContext) throws IOException, SshException {
		this(hostname, port, username, sshContext, 30000L, (char[])null);
	}
	
	public SshClient(SshConnection con) {
		this(con, true);
	}
	
	@SuppressWarnings("unchecked")
	public SshClient(SshConnection con, boolean closeConnection) {
		this.con = (Connection<SshClientContext>) con;
		this.closeConnection = closeConnection;
		this.sshContext = (SshClientContext) con.getContext();
		this.hostname = con.getRemoteIPAddress();
		this.port = con.getRemotePort();
		this.remotePublicKeys = Utils.csv(con.getRemotePublicKeys());
	}
	
	public SshClient(String hostname, int port, String username, SshClientContext sshContext, long connectTimeout, char[] password, SshKeyPair... identities) throws IOException, SshException {
		this.sshContext = sshContext;
		this.hostname = hostname;
		this.port = port;
		sshContext.setUsername(username);
		doConnect(hostname, port, username, sshContext, connectTimeout);
		boolean attempted = false;

		if(!isAuthenticated() && identities.length > 0) {
			attempted = true;
			authenticate(new KeyPairAuthenticator(identities), 30000);
		}
		
		if(!isAuthenticated() && Objects.nonNull(password) && password.length > 0) {
			attempted = true;
			authenticate(new PasswordAuthenticator(password), 30000);
		}
		
		if(attempted && !isAuthenticated()) {
			close();
			throw new IOException("Authentication failed");
		}
	}
	
	@SuppressWarnings("unchecked")
	protected void doConnect(String hostname, int port, String username, SshClientContext sshContext, long connectTimeout) throws SshException, IOException {
		configure(sshContext);
		ConnectRequestFuture future = sshContext.getEngine().connect(hostname, port, sshContext);
		future.waitFor(connectTimeout);
		if(!future.isSuccess()) {
			throw new IOException(String.format("Failed to connect to %s:%d", hostname, port));
		}
		con = (Connection<SshClientContext>) future.getConnection();
		con.addEventListener(new EventListener() {
			@Override
			public void processEvent(Event evt) {
				switch(evt.getId()) {
				case EventCodes.EVENT_KEY_EXCHANGE_INIT:
					remotePublicKeys = (String) evt.getAttribute(EventCodes.ATTRIBUTE_REMOTE_PUBLICKEYS);
					break;
				case EventCodes.EVENT_DISCONNECTED:
					disconnect();
					break;
				default:
					break;
				}
			}
		});
		if(!sshContext.getAuthenticators().isEmpty()) {
			con.getAuthenticatedFuture().waitForever();
			if(!con.getAuthenticatedFuture().isSuccess()) {
				close();
				throw new IOException(
						String.format("Failed to authenticate user %s at %s:%d", username, hostname, port));
			}
		}
	}
	
	protected void configure(SshClientContext sshContext) throws SshException, IOException {
		
	}

	public synchronized Task addTask(Task task) throws IOException {
		if(con==null) {
			throw new IOException("Client is no longer connected!");
		}
		con.addTask(task);
		return task;
	}

	@Override
	public void close() throws IOException {
		if(closeConnection) {
			con.disconnect();
		}
	}
	
	public SshClientContext getContext() {
		return con.getContext();
	}

	public Connection<SshClientContext> getConnection() {
		return con;
	}
	
	public ForwardingPolicy getForwardingPolicy() {
		return con.getContext().getForwardingPolicy();
	}
	
	public int startLocalForwarding(String addressToBind, String destinationHost) throws UnauthorizedException, SshException {
		return startLocalForwarding(addressToBind, 0, destinationHost, 0);
	}
	
	public int startLocalForwarding(String addressToBind, int portToBind, String destinationHost, int destinationPort) throws UnauthorizedException, SshException {
		ConnectionProtocolClient client = (ConnectionProtocolClient) con.getConnectionProtocol();
		return client.startLocalForwarding(addressToBind, portToBind, destinationHost, destinationPort);
	}
	
	public void stopLocalForwarding(String addressToBind, int portToBind) {
		ConnectionProtocolClient client = (ConnectionProtocolClient) con.getConnectionProtocol();
		client.stopLocalForwarding(addressToBind, portToBind);
	}
	
	public void stopLocalForwarding() {
		ConnectionProtocolClient client = (ConnectionProtocolClient) con.getConnectionProtocol();
		client.stopLocalForwarding();
	}
	
	public int startRemoteForwarding(String addressToBind, int portToBind, String destinationHost, int destinationPort) throws SshException {
		ConnectionProtocolClient client = (ConnectionProtocolClient) con.getConnectionProtocol();
		return client.startRemoteForwarding(addressToBind, portToBind, destinationHost, destinationPort);
	}

	public int startRemoteForwarding(String addressToBind, String destinationHost) throws SshException {
		return startRemoteForwarding(addressToBind, 0, destinationHost, 0);
	}
	
	public void stopRemoteForwarding(String addressToBind, int portToBind) throws SshException {
		ConnectionProtocolClient client = (ConnectionProtocolClient) con.getConnectionProtocol();
		client.stopRemoteForwarding(addressToBind, portToBind);
	}
	
	public void stopRemoteForwarding() throws SshException {
		ConnectionProtocolClient client = (ConnectionProtocolClient) con.getConnectionProtocol();
		client.stopRemoteForwarding();
	}

	public boolean isConnected() {
		return !con.isDisconnected();
	}

	public void disconnect() {
		if(isConnected() && closeConnection) {
			con.disconnect();
		}
	}
	
	protected <T extends Task> T doTask(T task, long timeout) throws IOException {
		addTask(task);
		if(timeout > 0) {
			task.waitFor(timeout);
		} else {
			task.waitForever();
		}
		if(!task.isDone()) {
			throw new IOException("Task did not complete before the specified timeout");
		}
		if(!task.isSuccess()) {
			if(!Objects.isNull(task.getLastError())) {
				if(task.getLastError() instanceof IOException) {
					throw (IOException)task.getLastError();
				}
				else {
					throw new IOException(task.getLastError().getMessage(), task.getLastError());
				}
			} else {
				throw new IOException("Task did not succeed but did not report an error");
			}
			
		}
		return task;
	}
	
	public File getFile(String path) throws IOException {
		return getFile(path, 0);
	}
	
	public File getFile(String path, long timeout) throws IOException {
		return doTask(DownloadFileTaskBuilder.create().withConnection(getConnection()).withRemotePath(path).build(), timeout).getDownloadedFile();
	}
	
	public void getFile(String path, File destination) throws IOException {
		getFile(path, destination, 0L);
	}
	
	public void getFile(String path, File destination, long timeout) throws IOException {
		doTask(DownloadFileTaskBuilder.create().withConnection(getConnection()).withRemotePath(path).withLocalFile(destination).build(), timeout);
	}

	public void putFile(File file) throws IOException {
		putFile(file, file.getName(), 0);
	}
	
	public void putFile(File file, String path) throws IOException {
		putFile(file, path, 0);
	}
	
	public void putFile(File file, String path, long timeout) throws IOException {
		doTask(UploadFileTaskBuilder.create().withConnection(getConnection()).withLocalFile(file).withRemotePath(path).build(), timeout);
	}

	public String executeCommand(String cmd) throws IOException {
		return executeCommand(cmd, 0, "UTF-8");
	}
	
	public String executeCommand(String cmd, long timeout) throws IOException {
		return executeCommand(cmd, timeout, "UTF-8");
	}
	
	public String executeCommand(String cmd, String charset) throws IOException {
		return executeCommand(cmd, 0, charset);
	}
	
	public String executeCommand(String cmd, long timeout, String charset) throws IOException {

		StringBuffer buffer = new StringBuffer();
		executeCommandWithResult(cmd, buffer, timeout, charset);
		return buffer.toString();
	}

	public int executeCommandWithResult(String cmd, StringBuffer buffer) throws IOException {
		return executeCommandWithResult(cmd, buffer, 0L);
	}
	
	public int executeCommandWithResult(String cmd) throws IOException {
		return executeCommandWithResult(cmd, new StringBuffer(), 0L);
	}
	
	public int executeCommandWithResult(String cmd, StringBuffer buffer, long timeout) throws IOException {
		return executeCommandWithResult(cmd, buffer, timeout, "UTF-8");
	}
	
	public int executeCommandWithResult(String cmd, StringBuffer buffer, String charset) throws IOException {
		return executeCommandWithResult(cmd, buffer, 0L, charset);
	}
	
	public int executeCommandWithResult(String cmd, StringBuffer buffer, long timeout, String charset) throws IOException {
		
		InteractiveOutputListener listener = new InteractiveOutputListener(buffer);
		AbstractCommandTask task = new AbstractCommandTask(getConnection(), cmd) {
			
			protected void beforeExecuteCommand(SessionChannelNG session) {
				session.addEventListener(listener);
			}
			@Override
			protected void onOpenSession(SessionChannelNG session) throws IOException {

				try {
					while(session.getInputStream().read() > -1);
				} catch (IOException e) {
					throw new IllegalStateException(e.getMessage(), e);				
				}
			}
		};
		
		doTask(task, timeout);
		return task.getExitCode();
	}
	
	class InteractiveOutputListener implements ChannelEventListener {

		StringBuffer output;
		
		InteractiveOutputListener(StringBuffer output) {
			this.output = output;
		}
		
		@Override
		public void onChannelDataIn(Channel channel, ByteBuffer buffer) {
			recordOutput(buffer);
		}

		@Override
		public void onChannelExtendedData(Channel channel, ByteBuffer buffer, int type) {
			recordOutput(buffer);
		}
		
		private synchronized void recordOutput(ByteBuffer buffer) {
			byte[] tmp = new byte[buffer.remaining()];
			buffer.get(tmp);
			try {
				output.append(new String(tmp, "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				throw new IllegalStateException(e.getMessage(), e);
			}
		}

	}
	
	public Set<String> getAuthenticationMethods() {
		return sshContext.getAuthenticationClient().getSupportedAuthentications();
	}
	
	public boolean authenticate(ClientAuthenticator authenticator, long timeout) throws IOException, SshException {
		
		if(Log.isDebugEnabled()) {
			Log.debug("Authenticating with {}", authenticator.getName());
		}
		sshContext.getAuthenticationClient().addAuthentication(authenticator);
		authenticator.waitFor(timeout);
		if(Log.isDebugEnabled()) {
			Log.debug("Authentication {}", authenticator.isCancelled() ? "was cancelled" : authenticator.isSuccess() ? "succeeded" : "failed");
		}
		if(authenticator.isCancelled())
			throw new SshException("Authentication cancelled.", SshException.CANCELLED_CONNECTION);
		return authenticator.isDone() && authenticator.isSuccess();
	}

	public boolean isAuthenticated() {
		return con.getAuthenticatedFuture().isDone() && con.getAuthenticatedFuture().isSuccess();
	}

	public <T extends Task> void runTask(T task, long timeout) throws IOException {
		doTask(task, timeout);
	}
	
	public <T extends Task> void runTask(T task) throws IOException {
		doTask(task, 0L);
	}

	public String[] getRemotePublicKeys() {
		return remotePublicKeys.split(",");
	}

	public String getRemoteIdentification() {
		return con.getRemoteIdentification();
	}
	
	public String getLocalIdentification() {
		return con.getLocalIdentification();
	}
	
	public String getHost() {
		return hostname;
	}

	public SshPublicKey getHostKey() {
		return con.getHostKey();
	}

	public SessionChannelNG openSessionChannel() throws SshException {
		return openSessionChannel(60000L, false);
	}
	
	public SessionChannelNG openSessionChannel(long timeout) throws SshException {
		return openSessionChannel(timeout, false);
	}
	
	public SessionChannelNG openSessionChannel(boolean autoConsume) throws SshException {
		return openSessionChannel(60000L, autoConsume);
	}
	
	public SessionChannelNG openSessionChannel(long timeout, boolean autoConsume) throws SshException {
		
		SessionChannelNG session = new SessionChannelNG(con, autoConsume);
		con.openChannel(session);
		session.getOpenFuture().waitFor(timeout);
		if(session.getOpenFuture().isSuccess()) {
			return session;
		}
		throw new SshException(String.format("Session was not opened after %d ms timeout threshold", timeout), SshException.SOCKET_TIMEOUT);
	}
	
	public SshClient openRemoteClient(String hostname, int port, String username) throws SshException, IOException, UnauthorizedException {
		
		int localPort = startLocalForwarding("127.0.0.1", 0, hostname, port);
		try {
			return new SshClient("127.0.0.1", localPort, username);
		} finally {
			stopLocalForwarding("127.0.0.1", localPort);
		}
	}

	public int getPort() {
		return port;
	}
}
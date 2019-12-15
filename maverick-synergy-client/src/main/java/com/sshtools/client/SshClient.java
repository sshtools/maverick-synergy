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
package com.sshtools.client;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Objects;
import java.util.Set;

import com.sshtools.client.tasks.AbstractCommandTask;
import com.sshtools.client.tasks.DownloadFileTask;
import com.sshtools.client.tasks.Task;
import com.sshtools.client.tasks.UploadFileTask;
import com.sshtools.common.events.Event;
import com.sshtools.common.events.EventCodes;
import com.sshtools.common.events.EventListener;
import com.sshtools.common.forwarding.ForwardingPolicy;
import com.sshtools.common.nio.ConnectRequestFuture;
import com.sshtools.common.permissions.UnauthorizedException;
import com.sshtools.common.publickey.InvalidPassphraseException;
import com.sshtools.common.publickey.SshKeyUtils;
import com.sshtools.common.ssh.Connection;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshKeyPair;

public class SshClient implements Closeable {

	public static final int EXIT_CODE_NOT_RECEIVED = AbstractCommandTask.EXIT_CODE_NOT_RECEIVED;
	
	Connection<SshClientContext> con;
	SshClientContext sshContext;
	String remotePublicKeys = "";
	
	public SshClient(String hostname, int port, String username, char[] password) throws IOException, SshException {
		this(hostname, port, username, new SshClientContext(), password);
	}
	
	public SshClient(String hostname, int port, String username, File key) throws IOException, SshException, InvalidPassphraseException {
		this(hostname, port, username, key, null);
	}
	
	public SshClient(String hostname, int port, String username, File key, String passphrase) throws IOException, SshException, InvalidPassphraseException {
		this(hostname, port, username, SshKeyUtils.getPrivateKey(key, passphrase));
	}
	
	public SshClient(String hostname, int port, String username, SshKeyPair... identities) throws IOException, SshException, InvalidPassphraseException {
		this(hostname, port, username, new SshClientContext(), identities);
	}

	public SshClient(String hostname, int port, String username, SshClientContext sshContext, SshKeyPair... identities) throws IOException, SshException {
		this(hostname, port, username, sshContext, null, identities);
	}
	
	public SshClient(String hostname, int port, String username, char[] password, SshKeyPair... identities) throws IOException, SshException {
		this(hostname, port, username, new SshClientContext(), password, identities);
	}

	public SshClient(String hostname, Integer port, String username, char[] password, File key, String passphrase) throws IOException, SshException, InvalidPassphraseException {
		this(hostname, port, username, password, SshKeyUtils.getPrivateKey(key, passphrase));
	}
	
	public SshClient(String hostname, Integer port, String username) throws IOException, SshException {
		this(hostname, port, username, new SshClientContext());
	}

	public SshClient(String hostname, Integer port, String username, SshClientContext sshContext) throws IOException, SshException {
		this(hostname, port, username, sshContext, (char[])null);
	}
	
	public SshClient(String hostname, int port, String username, SshClientContext sshContext, char[] password, SshKeyPair... identities) throws IOException, SshException {
		this.sshContext = sshContext;
		sshContext.setUsername(username);
		if(Objects.nonNull(password) && password.length > 0) {
			sshContext.addAuthenticator(new PasswordAuthenticator(password));
		}
		if(identities.length > 0) {
			sshContext.addAuthenticator(new PublicKeyAuthenticator(identities));
		}
		doConnect(hostname, port, username, sshContext);
	}

	@SuppressWarnings("unchecked")
	protected void doConnect(String hostname, int port, String username, SshClientContext sshContext) throws SshException, IOException {
		configure(sshContext);
		ConnectRequestFuture future = sshContext.getEngine().connect(hostname, port, sshContext);
		future.waitForever();
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
				throw new IOException(
						String.format("Failed to authenticate user %s at %s:%d", username, hostname, port));
			}
		}
	}
	
	protected void configure(SshClientContext sshContext) throws SshException, IOException {
		
	}

	public synchronized void addTask(Task task) throws IOException {
		if(con==null) {
			throw new IOException("Client is no longer connected!");
		}
		con.addTask(task);
	}

	@Override
	public void close() throws IOException {
		con.disconnect();
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
	
	public void stopRemoteForwarding(String addressToBind, int portToBind) throws SshException {
		ConnectionProtocolClient client = (ConnectionProtocolClient) con.getConnectionProtocol();
		client.stopRemoteForwarding(addressToBind, portToBind);
	}
	
	public void stopRemoteForwarding() {
		ConnectionProtocolClient client = (ConnectionProtocolClient) con.getConnectionProtocol();
		client.stopRemoteForwarding();
	}

	public boolean isConnected() {
		return !con.isDisconnected();
	}

	public void disconnect() {
		if(isConnected()) {
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
				throw new IOException("Task did not succeed", task.getLastError());
			} else {
				throw new IOException("Task did not succeed and did not report an error");
			}
			
		}
		return task;
	}
	
	public File getFile(String path) throws IOException {
		return getFile(path, 0);
	}
	
	public File getFile(String path, long timeout) throws IOException {
		return doTask(new DownloadFileTask(getConnection(), path), timeout).getDownloadedFile();
	}
	
	public void getFile(String path, File destination) throws IOException {
		getFile(path, destination, 0L);
	}
	
	public void getFile(String path, File destination, long timeout) throws IOException {
		doTask(new DownloadFileTask(getConnection(), path, destination), timeout);
	}

	public void putFile(File file) throws IOException {
		putFile(file, file.getName(), 0);
	}
	
	public void putFile(File file, String path) throws IOException {
		putFile(file, path, 0);
	}
	
	public void putFile(File file, String path, long timeout) throws IOException {
		doTask(new UploadFileTask(getConnection(), file, path), timeout);
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
	
	public int executeCommandWithResult(String cmd, StringBuffer buffer, long timeout) throws IOException {
		return executeCommandWithResult(cmd, buffer, timeout, "UTF-8");
	}
	
	public int executeCommandWithResult(String cmd, StringBuffer buffer, String charset) throws IOException {
		return executeCommandWithResult(cmd, buffer, 0L, charset);
	}
	
	public int executeCommandWithResult(String cmd, StringBuffer buffer, long timeout, String charset) throws IOException {
		
		AbstractCommandTask task = new AbstractCommandTask(getConnection(), cmd) {
			
			@Override
			protected void onOpenSession(SessionChannelNG session) throws IOException {
				
				BufferedReader reader = new BufferedReader(new InputStreamReader(session.getInputStream(), charset));
				String line;
				while((line = reader.readLine()) != null) {
					if(buffer.length() > 0) {
						buffer.append(System.lineSeparator());
					}
					buffer.append(line);
				}
			}
		};
		
		doTask(task, timeout);
		return task.getExitCode();
	}
	
	public Set<String> getAuthenticationMethods() {
		return sshContext.getAuthenticationClient().getSupportedAuthentications();
	}
	
	public boolean authenticate(ClientAuthenticator authenticator, long timeout) throws IOException, SshException {
		
		sshContext.getAuthenticationClient().doAuthentication(authenticator);
		authenticator.waitFor(timeout);
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

}

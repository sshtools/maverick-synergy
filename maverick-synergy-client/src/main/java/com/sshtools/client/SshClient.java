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
 * along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.client;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.sshtools.client.tasks.AbstractCommandTask;
import com.sshtools.client.tasks.DownloadFileTask;
import com.sshtools.client.tasks.Task;
import com.sshtools.client.tasks.UploadFileTask;
import com.sshtools.common.forwarding.ForwardingPolicy;
import com.sshtools.common.nio.ConnectRequestFuture;
import com.sshtools.common.nio.SshEngine;
import com.sshtools.common.permissions.UnauthorizedException;
import com.sshtools.common.publickey.InvalidPassphraseException;
import com.sshtools.common.publickey.SshKeyUtils;
import com.sshtools.common.ssh.Connection;
import com.sshtools.common.ssh.ConnectionTaskWrapper;
import com.sshtools.common.ssh.RequestFuture;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshKeyPair;

public class SshClient implements Closeable {

	Connection<SshClientContext> con;
	SshClientContext sshContext;
	SshEngine engine;
	
	public SshClient(String hostname, int port, String username, char[] password) throws IOException, SshException {
		engine = SshEngine.getDefaultInstance();
		sshContext = createContext(username);
		sshContext.addAuthenticator(new PasswordAuthenticator(password));
		doConnect(hostname, port, username, sshContext);
	}
	
	public SshClient(String hostname, int port, String username, File key) throws IOException, SshException, InvalidPassphraseException {
		this(hostname, port, username, key, null);
	}
	
	public SshClient(String hostname, int port, String username, File key, String passphrase) throws IOException, SshException, InvalidPassphraseException {
		this(hostname, port, username, SshKeyUtils.getPrivateKey(key, passphrase));
	}
	
	public SshClient(String hostname, int port, String username, SshKeyPair... identities) throws IOException, SshException, InvalidPassphraseException {
		engine = SshEngine.getDefaultInstance();
		sshContext = createContext(username);
		sshContext.addAuthenticator(new PublicKeyAuthenticator(identities));
		doConnect(hostname, port, username, sshContext);
	}
	
	public SshClient(String hostname, int port, String username, char[] password, SshKeyPair... identities) throws IOException, SshException, InvalidPassphraseException {
		engine = SshEngine.getDefaultInstance();
		sshContext = createContext(username);
		if(Objects.isNull(password) || password.length > 0) {
			sshContext.addAuthenticator(new PasswordAuthenticator(password));
		}
		if(identities.length > 0) {
			sshContext.addAuthenticator(new PublicKeyAuthenticator(identities));
		}
		doConnect(hostname, port, username, sshContext);
	}
	
	public SshClient(String hostname, Integer port, String username, char[] password, File key, String passphrase) throws IOException, SshException, InvalidPassphraseException {
		this(hostname, port, username, password, SshKeyUtils.getPrivateKey(key, passphrase));
	}
	
	@SuppressWarnings("unchecked")
	public SshClient(String hostname, Integer port, String username) throws IOException, SshException {
		engine = SshEngine.getDefaultInstance();
		sshContext = createContext(username);
		ConnectRequestFuture future = engine.connect(hostname, port, sshContext);
		future.waitForever();
		if(!future.isSuccess()) {
			throw new IOException(String.format("Failed to connect to %s:%d", hostname, port));
		}
		con = (Connection<SshClientContext>) future.getConnection();
	}

	@SuppressWarnings("unchecked")
	protected void doConnect(String hostname, int port, String username, SshClientContext sshContext) throws SshException, IOException {
		ConnectRequestFuture future = engine.connect(hostname, port, sshContext);
		future.waitForever();
		if(!future.isSuccess()) {
			throw new IOException(String.format("Failed to connect to %s:%d", hostname, port));
		}
		con = (Connection<SshClientContext>) future.getConnection();
		con.getAuthenticatedFuture().waitForever();
		if(!con.getAuthenticatedFuture().isSuccess()) {
			throw new IOException(
					String.format("Failed to authenticate user %s at %s:%d", username, hostname, port));
		}
	}
	
	public synchronized void addTask(Runnable task) throws IOException {
		if(con==null) {
			throw new IOException("Client is no longer connected!");
		}
		con.addTask(new ConnectionTaskWrapper(getConnection(), task));
	}

	protected SshClientContext createContext(String username) throws IOException {
		SshClientContext sshContext = new SshClientContext(engine);
		sshContext.setUsername(username);
		sshContext.addStateListener(new ClientStateListener() {

			@Override
			public void connected(Connection<SshClientContext> con) {
				
			}

			@Override
			public void disconnected(Connection<SshClientContext> con) {

			}

			@Override
			public void authenticate(AuthenticationProtocolClient auth, Connection<SshClientContext> con, Set<String> supportedAuths,
					boolean moreRequired, List<ClientAuthenticator> authsToTry) {

			}
		});
		return sshContext;
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
	
	public int startRemoteForwarding(String addressToBind, int portToBind, String destinationHost, int destinationPort) throws SshException {
		ConnectionProtocolClient client = (ConnectionProtocolClient) con.getConnectionProtocol();
		return client.startRemoteForwarding(addressToBind, portToBind, destinationHost, destinationPort);
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

	public void putFile(File file) throws IOException {
		putFile(file, file.getName(), 0);
	}
	
	public void putFile(File file, String path) throws IOException {
		putFile(file, path, 0);
	}
	
	public void putFile(File file, String path, long timeout) throws IOException {
		doTask(new UploadFileTask(getConnection(), file, path), timeout);
	}

	public String executeCommand(String cmd, long timeout) throws IOException {

		StringBuffer buffer = new StringBuffer();
		doTask(new AbstractCommandTask(getConnection(), cmd) {
			
			Throwable e = null;
			
			@Override
			protected void onOpenSession(SessionChannelNG session) {
				
				try {
					BufferedReader reader = new BufferedReader(new InputStreamReader(session.getInputStream()));
					String line;
					while((line = reader.readLine()) != null) {
						if(buffer.length() > 0) {
							buffer.append(System.lineSeparator());
						}
						buffer.append(line);
					}
					done(true);
				} catch (IOException e) {
					this.e = e;
					done(false);
				}
				session.close();
			}
			
			@Override
			protected void onCloseSession(SessionChannelNG session) {
				
			}

			@Override
			public Throwable getLastError() {
				return e;
			}
		}, timeout);
		
		return buffer.toString();
	}

	public Set<String> getAuthenticationMethods() {
		return sshContext.getAuthenticationClient().getSupportedAuthentications();
	}
	
	public boolean authenticate(ClientAuthenticator authenticator, long timeout) throws IOException {
		
		sshContext.getAuthenticationClient().doAuthentication(authenticator);
		authenticator.waitFor(timeout);
		
		return authenticator.isDone() && authenticator.isSuccess();
	}

	public boolean isAuthenticated() {
		return con.getAuthenticatedFuture().isDone() && con.getAuthenticatedFuture().isSuccess();
	}


}

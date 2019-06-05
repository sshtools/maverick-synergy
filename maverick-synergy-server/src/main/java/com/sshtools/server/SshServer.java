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
package com.sshtools.server;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import com.sshtools.common.auth.AuthenticationMechanismFactory;
import com.sshtools.common.auth.Authenticator;
import com.sshtools.common.files.AbstractFileFactory;
import com.sshtools.common.logger.Log;
import com.sshtools.common.nio.ProtocolContextFactory;
import com.sshtools.common.nio.SshEngine;
import com.sshtools.common.nio.SshEngineContext;
import com.sshtools.common.nio.SshEngineListenerAdapter;
import com.sshtools.common.publickey.InvalidPassphraseException;
import com.sshtools.common.publickey.SshKeyPairGenerator;
import com.sshtools.common.ssh.AbstractRequestFuture;
import com.sshtools.common.ssh.ChannelFactory;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.jce.JCEComponentManager;

public class SshServer implements ProtocolContextFactory<SshServerContext>, Closeable {

	SshEngine engine = new SshEngine();
	InetAddress addressToBind;
	int port;
	
	ServerShutdownFuture shutdownFuture = new ServerShutdownFuture();
	
	Collection<SshKeyPair> hostKeys = new ArrayList<>();
	Collection<Authenticator> providers = new ArrayList<>();
	AbstractFileFactory<?> fileFactory;
	ChannelFactory<SshServerContext> channelFactory = new DefaultServerChannelFactory(); 
	
	public SshServer() throws UnknownHostException {
		this("::", 0);
	}
	
	public SshServer(int port) throws UnknownHostException {
		this("::", port);
	}
	
	public SshServer(String addressToBind, int port) throws UnknownHostException {
		this(InetAddress.getByName(addressToBind), port);
	}
	
	public SshServer(InetAddress addressToBind, int port) {
		this.addressToBind = addressToBind;
		this.port = port;
		JCEComponentManager.getDefaultInstance();
	}
	
	public void start() throws IOException {
		
		beforeStart();
		if(!engine.startup()) {
			throw new IOException("Server failed to start");
		}
		this.port = engine.getContext().addListeningInterface(addressToBind, port, this, true).getActualPort();
		if(Log.isInfoEnabled()) {
			Log.info("Listening on port %d", this.port);
		}
		engine.addListener(new SshEngineListenerAdapter() {

			@Override
			public void shutdown(SshEngine engine) {
				shutdownFuture.stop();
			}
			
		});
		afterStart();
	}
	
	public void stop() {
		engine.shutdownNow(false, 0L);
	}
	
	public void addHostKeys(Collection<SshKeyPair> hostKeys) {
		this.hostKeys.addAll(hostKeys);
	}
	
	public void addHostKeys(SshKeyPair... hostKeys) {
		addHostKeys(Arrays.asList(hostKeys));
	}
	
	public void addHostKey(SshKeyPair key) {
		this.hostKeys.add(key);
	}
	
	public void addAuthenticator(Authenticator provider) {
		providers.add(provider);
	}
	
	public void setFileFactory(AbstractFileFactory<?> fileFactory) {
		this.fileFactory = fileFactory;
	}
	
	public void setChannelFactory(ChannelFactory<SshServerContext> channelFactory) {
		this.channelFactory = channelFactory;
	}
	
	public int getPort() {
		return port;
	}
	
	protected void beforeStart() {
		
	}
	
	protected void afterStart() {
		
	}
	
	public AbstractRequestFuture getShutdownFuture() {
		return shutdownFuture;
	}
	
	protected void configureHostKeys(SshServerContext sshContext) throws IOException, SshException {
		
		if(!hostKeys.isEmpty()) {
			sshContext.addHostKeys(hostKeys);
		} else {
			try {
				sshContext.loadOrGenerateHostKey(new File("ssh_host_rsa"), SshKeyPairGenerator.SSH2_RSA, 2048);
				sshContext.loadOrGenerateHostKey(new File("ssh_host_ecdsa_256"), SshKeyPairGenerator.ECDSA, 256);
				sshContext.loadOrGenerateHostKey(new File("ssh_host_ecdsa_384"), SshKeyPairGenerator.ECDSA, 384);
				sshContext.loadOrGenerateHostKey(new File("ssh_host_ecdsa_521"), SshKeyPairGenerator.ECDSA, 521);
				
				/**
				 * Because this requires a dependency and thus not installed by default
				 */
				if(JCEComponentManager.getDefaultInstance().supportedPublicKeys().contains(SshKeyPairGenerator.ED25519)) {
					sshContext.loadOrGenerateHostKey(new File("ssh_host_ed25519"), SshKeyPairGenerator.ED25519, 0);
				}
			} catch (InvalidPassphraseException e) {
				/**
				 * Should not happen because non of the above passes a passphrase.
				 */
				throw new SshException(e);
			}
		}
	}
	
	protected void configureFilesystem(SshServerContext sshContext) throws IOException, SshException {
		sshContext.setFileFactory(fileFactory);
	}
	
	@SuppressWarnings("unchecked")
	protected void configureAuthentication(SshServerContext sshContext) throws IOException, SshException {
		sshContext.getPolicy(AuthenticationMechanismFactory.class).addProviders(providers);
	}
	
	protected void configureChannels(SshServerContext sshContext) throws IOException, SshException {
		sshContext.setChannelFactory(channelFactory);
	}
	
	protected void configure(SshServerContext sshContext) throws IOException, SshException {
		
	}
	
	@Override
	public SshServerContext createContext(SshEngineContext daemonContext, SocketChannel sc)
			throws IOException, SshException {
		
		SshServerContext sshContext = new SshServerContext(engine);
		
		configureHostKeys(sshContext);

		configureAuthentication(sshContext);
		
		configureChannels(sshContext);
		
		configureFilesystem(sshContext);

		configure(sshContext);
		return sshContext;
	}

	public SshEngine getEngine() {
		return engine;
	}
	
	@Override
	public void close() {
		engine.shutdownNow(false, 0);
	}

	class ServerShutdownFuture extends AbstractRequestFuture {
		public void stop() {
			done(true);
		}
	}
}

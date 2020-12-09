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
import java.util.Collections;
import java.util.Objects;

import com.sshtools.common.auth.AuthenticationMechanismFactory;
import com.sshtools.common.auth.Authenticator;
import com.sshtools.common.forwarding.ForwardingPolicy;
import com.sshtools.common.logger.Log;
import com.sshtools.common.nio.ProtocolContextFactory;
import com.sshtools.common.nio.SshEngine;
import com.sshtools.common.nio.SshEngineContext;
import com.sshtools.common.nio.SshEngineListenerAdapter;
import com.sshtools.common.permissions.IPPolicy;
import com.sshtools.common.policy.FileFactory;
import com.sshtools.common.policy.FileSystemPolicy;
import com.sshtools.common.publickey.InvalidPassphraseException;
import com.sshtools.common.publickey.SshKeyPairGenerator;
import com.sshtools.common.publickey.SshKeyUtils;
import com.sshtools.common.scp.ScpCommand;
import com.sshtools.common.ssh.AbstractRequestFuture;
import com.sshtools.common.ssh.ChannelFactory;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.jce.JCEComponentManager;

public abstract class AbstractSshServer implements Closeable {

	SshEngine engine = new SshEngine();
	InetAddress addressToBind;
	int port;
	boolean enableScp;
	ServerShutdownFuture shutdownFuture = new ServerShutdownFuture();

	Collection<SshKeyPair> hostKeys = new ArrayList<>();
	Collection<Authenticator> providers = new ArrayList<>();
	Collection<Authenticator> defaultProviders = 
			Collections.unmodifiableCollection(
					Arrays.asList(new NoOpPasswordAuthenticator(),
							new NoOpPublicKeyAuthenticator()));
	FileFactory fileFactory;
	ForwardingPolicy forwardingPolicy = new ForwardingPolicy();
	
	ChannelFactory<SshServerContext> channelFactory; 
	File confFolder = new File(".");
	IPPolicy ipPolicy = new IPPolicy();
	
	protected AbstractSshServer() {
	}
	
	public AbstractSshServer(int port) throws UnknownHostException {
		this("::", port);
	}
	
	public AbstractSshServer(String addressToBind, int port) throws UnknownHostException {
		this(InetAddress.getByName(addressToBind), port);
	}
	
	public AbstractSshServer(InetAddress addressToBind, int port) {
		this.addressToBind = addressToBind;
		this.port = port;
		JCEComponentManager.getDefaultInstance();
	}
	
	protected abstract ProtocolContextFactory<?> getDefaultContextFactory();
	
	public void setConfigFolder(File confFolder) {
		this.confFolder = confFolder;
	}
	
	public void start() throws IOException {
		start(false);
	}
	
	public void addInterface(String addressToBind, int portToBind) throws IOException {
		engine.getContext().addListeningInterface(addressToBind, portToBind, getDefaultContextFactory(), true);
	}
	
	public void addInterface(String addressToBind, int portToBind, ProtocolContextFactory<?> contextFactory) throws IOException {
		engine.getContext().addListeningInterface(addressToBind, portToBind, contextFactory, true);
	}
	
	public void start(boolean requireListeningInterface) throws IOException {
		
		beforeStart();
		
		engine.setStartupRequiresListeningInterfaces(requireListeningInterface);
		
		if(!engine.startup()) {
			throw new IOException("Server failed to start");
		}
		if(!Objects.isNull(addressToBind)) {
			this.port = engine.getContext().addListeningInterface(addressToBind, port, getDefaultContextFactory(), true).getActualPort();
		} else if(engine.getContext().getListeningInterfaces().length > 0){
			this.port = engine.getContext().getListeningInterfaces()[0].getActualPort();
		}
		
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
	
	public void setFileFactory(FileFactory fileFactory) {
		this.fileFactory = fileFactory;
	}
	
	public void setChannelFactory(ChannelFactory<SshServerContext> channelFactory) {
		this.channelFactory = channelFactory;
	}
	
	public IPPolicy getIPPolicy() {
		return ipPolicy;
	}
	
	public void setIPPolicy(IPPolicy ipPolicy) {
		this.ipPolicy = ipPolicy;
	}
	
	public void enableSCP() {
		enableScp = true;
	}
	
	public void disableSCP() {
		enableScp = false;
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
		
	protected void configureHostKeys(SshServerContext sshContext, SocketChannel sc) throws IOException, SshException {
		
		if(!hostKeys.isEmpty()) {
			sshContext.addHostKeys(hostKeys);
		} else {
			loadOrGenerateHostKey(sshContext, new File(confFolder, "ssh_host_rsa"), SshKeyPairGenerator.SSH2_RSA, 2048);
			
			try {
				loadOrGenerateHostKey(sshContext, SshKeyUtils.getRSAPrivateKeyWithSHA256Signature(new File(confFolder, "ssh_host_rsa"), null));
			} catch (InvalidPassphraseException e) {
			}
			
			try {
				loadOrGenerateHostKey(sshContext, SshKeyUtils.getRSAPrivateKeyWithSHA512Signature(new File(confFolder, "ssh_host_rsa"), null));
			} catch (InvalidPassphraseException e) {
			}
			
			loadOrGenerateHostKey(sshContext, new File(confFolder, "ssh_host_ecdsa_256"), SshKeyPairGenerator.ECDSA, 256);
			loadOrGenerateHostKey(sshContext, new File(confFolder, "ssh_host_ecdsa_384"), SshKeyPairGenerator.ECDSA, 384);
			loadOrGenerateHostKey(sshContext, new File(confFolder, "ssh_host_ecdsa_521"), SshKeyPairGenerator.ECDSA, 521);
			loadOrGenerateHostKey(sshContext, new File(confFolder, "ssh_host_ed25519"), SshKeyPairGenerator.ED25519, 0);
			
			if(hostKeys.isEmpty()) {
				throw new IOException("There are no host keys available");
			}
		}
	}
	
	private void loadOrGenerateHostKey(SshServerContext context, File file, String type, int bitlength) {
		try {
			hostKeys.add(context.loadOrGenerateHostKey(file, type, bitlength));
		} catch (IOException | InvalidPassphraseException | SshException e) {
			Log.warn("Could not generate or load host key for algorithm {}: {}", type, e.getMessage());
		}
	}
	
	private void loadOrGenerateHostKey(SshServerContext context, SshKeyPair pair) throws IOException {
		
		context.addHostKey(pair);
		hostKeys.add(pair);
		
	}
	
	protected void configureFilesystem(SshServerContext sshContext, SocketChannel sc) throws IOException, SshException {
		sshContext.getPolicy(FileSystemPolicy.class).setFileFactory(fileFactory);
		if(enableScp) {
			sshContext.addCommand("scp", ScpCommand.class);
		}
	}
	
	@SuppressWarnings("unchecked")
	protected void configureAuthentication(SshServerContext sshContext, SocketChannel sc) throws IOException, SshException {
		if(providers.isEmpty()) {
			sshContext.getPolicy(AuthenticationMechanismFactory.class).addProviders(defaultProviders);
		} else {
			sshContext.getPolicy(AuthenticationMechanismFactory.class).addProviders(providers);
		}
	}
	
	protected void configureChannels(SshServerContext sshContext, SocketChannel sc) throws IOException, SshException {
		
		if(Objects.nonNull(channelFactory)) {
			sshContext.setChannelFactory(channelFactory);
		}
	}
	
	protected void configureForwarding(SshServerContext sshContext, SocketChannel sc) throws IOException, SshException {
		sshContext.setPolicy(ForwardingPolicy.class, forwardingPolicy);
	}
	
	protected void configure(SshServerContext sshContext, SocketChannel sc) throws IOException, SshException {
		sshContext.setPolicy(IPPolicy.class, ipPolicy);
	}
	
	public SshServerContext createServerContext(SshEngineContext daemonContext, SocketChannel sc)
			throws IOException, SshException {
		
		SshServerContext sshContext = new SshServerContext(daemonContext.getEngine());
		
		configureHostKeys(sshContext, sc);

		configureAuthentication(sshContext, sc);
		
		configureChannels(sshContext, sc);
		
		configureFilesystem(sshContext, sc);

		configureForwarding(sshContext, sc);
		
		configure(sshContext, sc);
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

	public ForwardingPolicy getForwardingPolicy() {
		return forwardingPolicy;
	}
}

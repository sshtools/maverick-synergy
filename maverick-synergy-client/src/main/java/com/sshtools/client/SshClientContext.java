/*
 *    _           _             _   _
 *   (_) __ _  __| | __ _ _ __ | |_(_)_   _____
 *   | |/ _` |/ _` |/ _` | '_ \| __| \ \ / / _ \
 *   | | (_| | (_| | (_| | |_) | |_| |\ V /  __/
 *  _/ |\__,_|\__,_|\__,_| .__/ \__|_| \_/ \___|
 * |__/                  |_|
 *
 * This file is part of the Maverick Synergy Hotfixes Java SSH API
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Copyright (C) 2002-2021 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
 */

package com.sshtools.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.sshtools.client.components.Curve25519SHA256Client;
import com.sshtools.client.components.Curve25519SHA256LibSshClient;
import com.sshtools.client.components.DiffieHellmanEcdhNistp256;
import com.sshtools.client.components.DiffieHellmanEcdhNistp384;
import com.sshtools.client.components.DiffieHellmanEcdhNistp521;
import com.sshtools.client.components.DiffieHellmanGroup14Sha1JCE;
import com.sshtools.client.components.DiffieHellmanGroup14Sha256JCE;
import com.sshtools.client.components.DiffieHellmanGroup15Sha512JCE;
import com.sshtools.client.components.DiffieHellmanGroup16Sha512JCE;
import com.sshtools.client.components.DiffieHellmanGroup17Sha512JCE;
import com.sshtools.client.components.DiffieHellmanGroup18Sha512JCE;
import com.sshtools.client.components.DiffieHellmanGroupExchangeSha256JCE;
import com.sshtools.client.components.Rsa2048Sha256;
import com.sshtools.common.knownhosts.HostKeyVerification;
import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.SecurityLevel;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.ComponentFactory;
import com.sshtools.common.ssh.components.ComponentManager;
import com.sshtools.common.ssh.components.jce.JCEComponentManager;
import com.sshtools.synergy.nio.ConnectRequestFuture;
import com.sshtools.synergy.nio.DefaultSocketConnectionFactory;
import com.sshtools.synergy.nio.ProtocolEngine;
import com.sshtools.synergy.nio.SocketConnectionFactory;
import com.sshtools.synergy.nio.SshEngine;
import com.sshtools.synergy.nio.SshEngineContext;
import com.sshtools.synergy.ssh.ChannelFactory;
import com.sshtools.synergy.ssh.ConnectionManager;
import com.sshtools.synergy.ssh.ForwardingManager;
import com.sshtools.synergy.ssh.GlobalRequestHandler;
import com.sshtools.synergy.ssh.SshContext;
import com.sshtools.synergy.ssh.components.SshKeyExchange;

/**
 * Holds the configuration for an SSH connection.
 */
public class SshClientContext extends SshContext {

	List<ClientAuthenticator> authenticators = new ArrayList<ClientAuthenticator>();

	BannerDisplay bannerDisplay;

	String username;

	protected TransportProtocolClient transport;
	
	Collection<ClientStateListener> stateListeners = new ArrayList<ClientStateListener>();
	
	int subsystemCacheSize = 65535 * 10;
	
	ChannelFactory<SshClientContext> channelFactory = new DefaultClientChannelFactory();
	
	Map<String, GlobalRequestHandler<SshClientContext>> globalRequestHandlers = Collections
			.synchronizedMap(new HashMap<String, GlobalRequestHandler<SshClientContext>>());

	SocketConnectionFactory socketConnectionFactory = new DefaultSocketConnectionFactory();
	AuthenticationProtocolClient authenticationClient; 
	
	private HostKeyVerification hkv = null;
	
//	private String remoteHostname;
//	private int remotePort;
	
	ForwardingManager<SshClientContext> forwardingManager;
	ConnectionManager<SshClientContext> connectionManager;
	
	static ForwardingManager<SshClientContext> defaultForwardingManager 
				= new ForwardingManager<SshClientContext>();
	static ConnectionManager<SshClientContext> defaultConnectionManager 
				= new ConnectionManager<SshClientContext>("client");
	
	static {
		defaultForwardingManager.setForwardingFactory((h, p) -> new LocalForwardingChannelFactoryImpl(h, p));
		defaultForwardingManager.addRemoteForwardRequestHandler(new DefaultRemoteForwardRequestHandler());
	}

	private boolean preferKeyboardInteractiveOverPassword = true;
	
	private static ComponentFactory<SshKeyExchange<SshClientContext>> verifiedKeyExchanges;
	
	public SshClientContext() throws IOException, SshException {
		this(SecurityLevel.WEAK);
	}
	
	public SshClientContext(SshEngine daemon, ComponentManager componentManager, SecurityLevel securityLevel) throws IOException, SshException {
		super(componentManager, securityLevel);
		this.daemon = daemon;
	}
	
	public SshClientContext(SshEngine daemon) throws IOException, SshException {
		this(daemon, ComponentManager.getDefaultInstance(), SecurityLevel.WEAK);
	}
	
	public SshClientContext(SshEngine daemon, SecurityLevel securityLevel) throws IOException, SshException {
		this(daemon, ComponentManager.getDefaultInstance(), securityLevel);
	}
	
	public SshClientContext(SecurityLevel securityLevel) throws IOException, SshException {
		this(SshEngine.getDefaultInstance(), securityLevel);
	}

	public ProtocolEngine createEngine(ConnectRequestFuture connectFuture) throws IOException {
		return transport = new TransportProtocolClient(this, connectFuture);
	}
	
	public final SshEngine getEngine() {
		return daemon;
	}
	
	/**
	 * Set the username for this connection.
	 * @param username
	 */
	public SshClientContext setUsername(String username) {
		this.username = username;
		return this;
	}
	
	/**
	 * Get the username of this connection.
	 * @return
	 */
	public String getUsername() {
		return username;
	}

	public SshClientContext addStateListener(ClientStateListener stateListener) {
		this.stateListeners.add(stateListener);
		return this;
	}

	public Collection<ClientStateListener> getStateListeners() {
		return stateListeners;
	}
	
	@Override
	public ForwardingManager<SshClientContext> getForwardingManager() {
		return forwardingManager == null ? defaultForwardingManager : forwardingManager;
	}
	
	public SshClientContext setForwardingManager(ForwardingManager<SshClientContext> forwardingManager) {
		this.forwardingManager = forwardingManager;
		return this;
	}
	
	public void keysExchanged(boolean first) {
		if(first) {
			transport.startService(authenticationClient = new AuthenticationProtocolClient(
					transport,
					SshClientContext.this,
					username));
		}
	}

	public AuthenticationProtocolClient getAuthenticationClient() {
		return authenticationClient;
	}
	
	@SuppressWarnings("unchecked")
	protected synchronized void configureKeyExchanges() {
		
		if(Objects.nonNull(verifiedKeyExchanges)) {
			keyExchanges = (ComponentFactory<SshKeyExchange<? extends SshContext>>)verifiedKeyExchanges.clone();
			return;
		}
		
		if(Log.isInfoEnabled()) {
			Log.info("Initializing client key exchanges");
		}
		
		verifiedKeyExchanges = new ComponentFactory<SshKeyExchange<SshClientContext>>(componentManager);
		
		JCEComponentManager.getDefaultInstance().loadExternalComponents("kex-client.properties", verifiedKeyExchanges);
		
		if(testClientKeyExchangeAlgorithm(
				Curve25519SHA256Client.CURVE25519_SHA2, Curve25519SHA256Client.class)) {
			verifiedKeyExchanges.add(Curve25519SHA256Client.CURVE25519_SHA2, Curve25519SHA256Client.class);
		}
		
		if(testClientKeyExchangeAlgorithm(
				Curve25519SHA256LibSshClient.CURVE25519_SHA2_AT_LIBSSH_ORG, Curve25519SHA256LibSshClient.class)) {
			verifiedKeyExchanges.add(Curve25519SHA256LibSshClient.CURVE25519_SHA2_AT_LIBSSH_ORG, Curve25519SHA256LibSshClient.class);
		}
		
		if (testClientKeyExchangeAlgorithm("diffie-hellman-group-exchange-sha256",
				DiffieHellmanGroupExchangeSha256JCE.class)) {
			verifiedKeyExchanges.add("diffie-hellman-group-exchange-sha256", DiffieHellmanGroupExchangeSha256JCE.class);
		}
		
		if (testClientKeyExchangeAlgorithm("diffie-hellman-group14-sha256", DiffieHellmanGroup14Sha256JCE.class)) {
			verifiedKeyExchanges.add("diffie-hellman-group14-sha256", DiffieHellmanGroup14Sha256JCE.class);
		}
		
		if (testClientKeyExchangeAlgorithm("diffie-hellman-group15-sha512", DiffieHellmanGroup15Sha512JCE.class)) {
			verifiedKeyExchanges.add("diffie-hellman-group15-sha512", DiffieHellmanGroup15Sha512JCE.class);
		}
		
		if (testClientKeyExchangeAlgorithm("diffie-hellman-group16-sha512", DiffieHellmanGroup16Sha512JCE.class)) {
			verifiedKeyExchanges.add("diffie-hellman-group16-sha512", DiffieHellmanGroup16Sha512JCE.class);
		}
		
		if (testClientKeyExchangeAlgorithm("diffie-hellman-group17-sha512", DiffieHellmanGroup17Sha512JCE.class)) {
			verifiedKeyExchanges.add("diffie-hellman-group17-sha512", DiffieHellmanGroup17Sha512JCE.class);
		}
		
		if (testClientKeyExchangeAlgorithm("diffie-hellman-group18-sha512", DiffieHellmanGroup18Sha512JCE.class)) {
			verifiedKeyExchanges.add("diffie-hellman-group18-sha512", DiffieHellmanGroup18Sha512JCE.class);
		}
		
		if (testClientKeyExchangeAlgorithm("diffie-hellman-group14-sha1", DiffieHellmanGroup14Sha1JCE.class)) {
			verifiedKeyExchanges.add("diffie-hellman-group14-sha1", DiffieHellmanGroup14Sha1JCE.class);
		}
		
		if (testClientKeyExchangeAlgorithm("ecdh-sha2-nistp256", DiffieHellmanEcdhNistp256.class)) {
			verifiedKeyExchanges.add("ecdh-sha2-nistp256", DiffieHellmanEcdhNistp256.class);
		}
		
		if (testClientKeyExchangeAlgorithm("ecdh-sha2-nistp384", DiffieHellmanEcdhNistp384.class)) {
			verifiedKeyExchanges.add("ecdh-sha2-nistp384", DiffieHellmanEcdhNistp384.class);
		}

		if (testClientKeyExchangeAlgorithm("ecdh-sha2-nistp521", DiffieHellmanEcdhNistp521.class)) {
			verifiedKeyExchanges.add("ecdh-sha2-nistp521", DiffieHellmanEcdhNistp521.class);
		}

		if (testClientKeyExchangeAlgorithm(Rsa2048Sha256.RSA_2048_SHA256, Rsa2048Sha256.class)) {
			verifiedKeyExchanges.add(Rsa2048Sha256.RSA_2048_SHA256, Rsa2048Sha256.class);
		}
		
		
		keyExchanges = (ComponentFactory<SshKeyExchange<? extends SshContext>>)verifiedKeyExchanges.clone();
		

	}

	public boolean testClientKeyExchangeAlgorithm(String name, Class<? extends SshKeyExchange<? extends SshContext>> cls) {
		
		SshKeyExchange<? extends SshContext> c = null;
		try {

			c = cls.getConstructor().newInstance();

			if (!JCEComponentManager.getDefaultInstance().supportedDigests().contains(c.getHashAlgorithm()))
				throw new Exception("Hash algorithm " + c.getHashAlgorithm() + " is not supported");

			c.test();
			
		} catch (Exception e) {
			if(Log.isDebugEnabled())
				Log.debug("   " + name + " (client) will not be supported: " + e.getMessage());
			return false;
		} catch (Throwable e) {
			// a null pointer exception will be caught at the end of the keyex
			// call when transport.sendmessage is called, at this point the
			// algorithm has not thrown an exception so we ignore this excpected
			// exception.
		}

		if(Log.isDebugEnabled())
			Log.debug("   " + name + " (client) will be supported using JCE Provider "
					+ c.getProvider());

		return true;
	}

	public String getSupportedPublicKeys() {
		return listPublicKeys(supportedPublicKeys().toArray());
	}

	@Override
	public ComponentFactory<SshKeyExchange<?>> supportedKeyExchanges() {
		return keyExchanges;
	}

	@Override
	public String getPreferredPublicKey() {
		return prefPublicKey;
	}
	

	public void setPreferredPublicKey(String prefPublicKey) {
		this.prefPublicKey = prefPublicKey;
	}

	@Override
	public ConnectionManager<SshClientContext> getConnectionManager() {
		return connectionManager == null ? defaultConnectionManager : connectionManager;
	}

	public SshClientContext setConnectionManager(
			ConnectionManager<SshClientContext> connectionManager) {
		this.connectionManager = connectionManager;
		return this;
	}

	public SshClientContext addAuthenticator(ClientAuthenticator auth) {
		authenticators.add(auth);
		return this;
	}

	public List<ClientAuthenticator> getAuthenticators() {
		return authenticators;
	}

	public BannerDisplay getBannerDisplay() {
		return bannerDisplay;
	}

	public SshClientContext setBannerDisplay(BannerDisplay bannerDisplay) {
		this.bannerDisplay = bannerDisplay;
		return this;
	}

	public int getSubsystemCacheSize() {
		return subsystemCacheSize;
	}
	
	public SshClientContext setSubsystemCacheSize(int subsystemCacheSize) {
		this.subsystemCacheSize = subsystemCacheSize;
		return this;
	}

	@Override
	public ChannelFactory<SshClientContext> getChannelFactory() {
		return channelFactory;
	}

	public void setChannelFactory(ChannelFactory<SshClientContext> channelFactory) {
		this.channelFactory = channelFactory;
	}

	@Override
	public SshEngineContext getDaemonContext() {
		return daemon.getContext();
	}

	public SshClientContext addGlobalRequestHandler(GlobalRequestHandler<SshClientContext> handler) {
		for (int i = 0; i < handler.supportedRequests().length; i++) {
			globalRequestHandlers.put(handler.supportedRequests()[i], handler);
		}
		return this;
	}

	@Override
	public GlobalRequestHandler<SshClientContext> getGlobalRequestHandler(String name) {
		return globalRequestHandlers.get(name);
	}

	@Override
	public SocketConnectionFactory getSocketConnectionFactory() {
		return socketConnectionFactory;
	}

	public HostKeyVerification getHostKeyVerification() {
		return hkv;
	}

	public SshClientContext setHostKeyVerification(HostKeyVerification  hkv) {
		this.hkv = hkv;
		return this;
	}

//	public AbstractRequestFuture authenticate(Connection<?> con, PasswordAuthenticator authenticator) throws IOException, SshException {
//
//		if(transport==null) {
//			throw new IllegalStateException("You cannot call authenticate until the connection has been established");
//		}
//		
//		if(!(transport.getActiveService() instanceof AuthenticationProtocolClient)) {
//			throw new IllegalStateException("You cannot call authenticate until the connection has been established");
//		}
//		
//		((AuthenticationProtocolClient) transport.getActiveService()).addAuthentication(authenticator);
//		return authenticator;
//	}

	public boolean getPreferKeyboardInteractiveOverPassword() {
		return preferKeyboardInteractiveOverPassword;
	}
	
	public void setPreferKeyboardInteractiveOverPassword(boolean preferKeyboardInteractiveOverPassword) {
		this.preferKeyboardInteractiveOverPassword = preferKeyboardInteractiveOverPassword;
	}
}

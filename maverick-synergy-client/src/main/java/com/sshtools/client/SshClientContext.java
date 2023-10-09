package com.sshtools.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;

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
		for(var kex : ServiceLoader.load(SshKeyExchangeClientFactory.class, 
				JCEComponentManager.getDefaultInstance().getClassLoader())) {
			if(testClientKeyExchangeAlgorithm(kex))
				verifiedKeyExchanges.add(kex);
		}
		
		keyExchanges = (ComponentFactory<SshKeyExchange<? extends SshContext>>)verifiedKeyExchanges.clone();
		
	}

	public boolean testClientKeyExchangeAlgorithm(SshKeyExchangeClientFactory<? extends SshKeyExchangeClient> cls) {
		var name  = cls.getKeys() [0];
		
		SshKeyExchange<? extends SshContext> c = null;
		try {

			c = cls.create();

			if (!JCEComponentManager.getDefaultInstance().supportedDigests().contains(c.getHashAlgorithm()))
				throw new Exception("Hash algorithm " + c.getHashAlgorithm() + " is not supported");

			c.test();
			
		} catch (Exception e) {
			if(Log.isInfoEnabled())
				Log.info("   " + name + " (client) will not be supported: " + e.getMessage());
			return false;
		} catch (Throwable e) {
			// a null pointer exception will be caught at the end of the keyex
			// call when transport.sendmessage is called, at this point the
			// algorithm has not thrown an exception so we ignore this excpected
			// exception.
		}

		if(Log.isInfoEnabled())
			Log.info("   " + name + " (client) will be supported using JCE Provider "
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

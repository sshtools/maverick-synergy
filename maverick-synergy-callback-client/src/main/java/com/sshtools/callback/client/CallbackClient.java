package com.sshtools.callback.client;

import java.io.Closeable;

/*-
 * #%L
 * Callback Client API
 * %%
 * Copyright (C) 2002 - 2024 JADAPTIVE Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.sshtools.common.auth.InMemoryMutualKeyAuthenticationStore;
import com.sshtools.common.events.Event;
import com.sshtools.common.events.EventCodes;
import com.sshtools.common.events.EventListener;
import com.sshtools.common.events.EventServiceImplementation;
import com.sshtools.common.forwarding.ForwardingPolicy.ForwardingPolicyBuilder;
import com.sshtools.common.logger.Log;
import com.sshtools.common.permissions.Policy;
import com.sshtools.common.policy.AuthenticationPolicy.AuthenticationPolicyBuilder;
import com.sshtools.common.policy.FileFactory;
import com.sshtools.common.policy.FileSystemPolicy;
import com.sshtools.common.publickey.SshKeyUtils;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.jce.JCEComponentManager;
import com.sshtools.server.DefaultServerChannelFactory;
import com.sshtools.server.SshServerContext;
import com.sshtools.synergy.nio.DisconnectRequestFuture;
import com.sshtools.synergy.nio.SshEngine;
import com.sshtools.synergy.nio.SshEngineContext;
import com.sshtools.synergy.ssh.ChannelFactory;
import com.sshtools.synergy.ssh.ChannelFactoryListener;
import com.sshtools.synergy.ssh.Connection;

public final class CallbackClient implements ChannelFactoryListener<SshServerContext>, ICallbackClient<CallbackSession>, Closeable {
	
	public static final String CALLBACK_CLIENT = "callbackClient";

	private SshEngine ssh = new SshEngine();
	private ChannelFactory<SshServerContext> channelFactory;
	private FileFactory fileFactory;
	private final Set<CallbackSession> clients = new HashSet<>();
	private final List<SshKeyPair> hostKeys = new ArrayList<>();
	private final List<Object> defaultPolicies = new ArrayList<>();
	private final List<CallbackClientListener> listeners = new ArrayList<>();
	private final ExecutorService executor;

	public CallbackClient() {
		this(1);
	}

	public CallbackClient(int maxClients) {
		executor = Executors.newFixedThreadPool(maxClients);
		EventServiceImplementation.getInstance().addListener(new DisconnectionListener());
		channelFactory = new DefaultServerChannelFactory();
		try {
			ssh.startup();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
	
	@Override
	public int getConnections() {
		return clients.size();
	}

	@Override
	public void addListener(CallbackClientListener listener) {
		this.listeners.add(listener);
	}

	@Override
	public void removeListener(CallbackClientListener listener) {
		this.listeners.add(listener);
	}

	public SshEngine getSshEngine() {
		return ssh;
	}

	/**
	 * @param policies
	 * @see #setPolicyDefaults(Policy...)
	 */
	@Deprecated(since ="3.2.0", forRemoval = true)
	public void setDefaultPolicies(Object... policies) {
		defaultPolicies.addAll(Arrays.asList(policies));
	}

	@Override
	public void setPolicyDefaults(Policy... policies) {
		defaultPolicies.addAll(Arrays.asList(policies));
	}

	@Override
	public synchronized CallbackSession start(ICallbackConfiguration config) throws IOException {
		return start(config, config.getServerHost(), config.getServerPort());
	}

	public synchronized CallbackSession start(ICallbackConfiguration config, String hostname, int port) throws IOException {
		CallbackSession session = new CallbackSession(config, this, hostname, port);
		for(var i = listeners.size() - 1 ; i >= 0; i--) {
			listeners.get(i).onClientStarting(session);
		}
		onClientStarting(session);
		start(session);
		return session;
	}

	public void updateMemo(String memo) throws IOException {
		synchronized(clients) {
			IOException exception = null;
			for(var clnt : clients) {
				try {
					clnt.updateMemo(memo);
				} catch (IOException e) {
					if(exception == null) {
						exception = e;
					}
				}
			}
			if(exception != null) {
				throw exception;
			}
		}
	}

	public synchronized void start(CallbackSession client) {

		if(Log.isInfoEnabled()) {
			try {
				Log.info("Starting client " + client.getConfig().getAgentName() + " to connect to " + SshKeyUtils.getFormattedKey(client.getConfig().getPublicKey(), ""));
			} catch (IOException e) {
			}
		}
		executor.execute(client);
	}

	void onClientConnected(CallbackSession client, SshConnection connection) {
		clients.add(client);
		for(var i = listeners.size() - 1 ; i >= 0; i--) {
			listeners.get(i).onClientStart(client, connection);
		}
		onClientStart(client, connection);
	}

	@Override
	public boolean isConnected() {
		return ssh.isStarted() && !clients.isEmpty();
	}

	public Collection<CallbackSession> getClients() {
		return clients;
	}


	@Deprecated(since ="3.2.0", forRemoval = true)
	protected void onClientStarting(ICallbackSession client) {

	}

	@Deprecated(since ="3.2.0", forRemoval = true)
	protected void onClientStopping(ICallbackSession client) {

	}

	@Deprecated(since ="3.2.0", forRemoval = true)
	protected void onClientStart(ICallbackSession client, SshConnection connection) {

	}

	@Deprecated(since ="3.2.0", forRemoval = true)
	protected void onClientStop(ICallbackSession client, SshConnection connection) {

	}

	@Override
	public void waitForShutdown() {
		getSshEngine().getShutdownFuture().waitForever();
		
	}

	@Override
	public Throwable getLastError() {
		return getSshEngine().getLastError();
	}

	public synchronized void stop(CallbackSession client) {

		onClientStopping(client);
		for(var i = listeners.size() - 1 ; i >= 0; i--) {
			listeners.get(i).onClientStopping(client);
		}

		if(Log.isInfoEnabled()) {
			Log.info("Stopping callback client");
		}

		DisconnectRequestFuture future = client.stop();

		if(Log.isInfoEnabled()) {
			Log.info("Callback client has disconnected [{}]", String.valueOf(future.isDone()));
		}
	}

	@Override
	public void stop() {
		for(CallbackSession client : new ArrayList<>(clients)) {
			stop(client);
		}
	}

	@Override
	public void close() {
		stop();
		ssh.shutdownAndExit();
		executor.shutdownNow();
	}

	class DisconnectionListener implements EventListener {

		@Override
		public void processEvent(Event evt) {
			switch(evt.getId()) {
			case EventCodes.EVENT_DISCONNECTED:
				final SshConnection con = (SshConnection)evt.getAttribute(EventCodes.ATTRIBUTE_CONNECTION);
				CallbackSession client = (CallbackSession) con.getProperty(CALLBACK_CLIENT);
				if(client != null) {
					if(!executor.isShutdown()) {
						executor.execute(new Runnable() {
							@Override
							public void run() {
								if(Log.isInfoEnabled()) {
									Log.info("Disconnected from {}:{}" ,
										client.getConfig().getServerHost(),
										client.getConfig().getServerPort());
								}
								con.removeProperty(CALLBACK_CLIENT);
								clients.remove(client);
							}
						});
					}	
				}
				

				break;
			default:
				break;
			}
		}

	}

	@SuppressWarnings("deprecation")
	public SshServerContext createContext(SshEngineContext daemonContext, ICallbackConfiguration config) throws IOException, SshException {

		SshServerContext sshContext = new SshServerContext(getSshEngine(), JCEComponentManager.getDefaultInstance());

		sshContext.setIdleConnectionTimeoutSeconds(0);
		sshContext.setExtendedIdentificationSanitization(false);

		for(SshKeyPair key : hostKeys) {
			sshContext.addHostKey(key);
		}

		for(Object policy : defaultPolicies) {
			if(policy instanceof Policy p) {
				sshContext.setPolicy(p);
			} else {
				sshContext.setPolicy(policy.getClass(), policy);
			}
		}

		sshContext.setSoftwareVersionComments(String.format("%s_%s", config.getCallbackIdentifier(), config.getAgentName()));

		InMemoryMutualKeyAuthenticationStore authenticationStore = new InMemoryMutualKeyAuthenticationStore();
		authenticationStore.addKey(config.getAgentName(), config.getPrivateKey(), config.getPublicKey());
		MutualCallbackAuthenticationProvider provider = new MutualCallbackAuthenticationProvider(authenticationStore);
		sshContext.setAuthenicationMechanismFactory(new CallbackAuthenticationMechanismFactory<>(provider));

		sshContext.setPolicy(AuthenticationPolicyBuilder.create().
				addRequiredMechanisms(MutualCallbackAuthenticationProvider.MUTUAL_KEY_AUTHENTICATION).
				build());

		sshContext.setSendIgnorePacketOnIdle(true);

		configureForwarding(sshContext, config);
		configureChannels(sshContext, config);
		configureFilesystem(sshContext, config);

		configureContext(sshContext, config);
		for(var i = listeners.size() - 1 ; i >= 0; i--) {
			listeners.get(i).onConfigureContext(sshContext, config);
		}

		return sshContext;
	}

	@Deprecated(since ="3.2.0", forRemoval = true)
	protected void configureContext(SshServerContext sshContext, ICallbackConfiguration config) {
	}

	protected void configureFilesystem(SshServerContext sshContext, ICallbackConfiguration config) {
		sshContext.getPolicy(FileSystemPolicy.class).setFileFactory(fileFactory);
	}

	protected void configureChannels(SshServerContext sshContext, ICallbackConfiguration config) {
		sshContext.setChannelFactory(channelFactory);
	}

	protected void configureForwarding(SshServerContext sshContext, ICallbackConfiguration config) {
		sshContext.setPolicy(ForwardingPolicyBuilder.create().
				allowAll().
				build());
	}

	@Override
	public void addHostKey(SshKeyPair pair) {
		this.hostKeys.add(pair);
	}

	@Override
	public void setChannelFactory(ChannelFactory<SshServerContext> channelFactory) {
		this.channelFactory = channelFactory;
	}

	@Override
	public void setFileFactory(FileFactory fileFactory) {
		this.fileFactory = fileFactory;
	}

	void doClientStop(CallbackSession callbackSession, Connection<?> con) {
		onClientStop(callbackSession, con);
		for(var i = listeners.size() - 1 ; i >= 0; i--) {
			listeners.get(i).onClientStop(callbackSession, con);
		}
	}

}

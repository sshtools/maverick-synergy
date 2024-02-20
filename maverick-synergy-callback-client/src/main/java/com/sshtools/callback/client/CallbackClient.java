package com.sshtools.callback.client;

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
import com.sshtools.common.logger.Log;
import com.sshtools.common.policy.AuthenticationPolicy;
import com.sshtools.common.policy.FileFactory;
import com.sshtools.common.policy.FileSystemPolicy;
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

public class CallbackClient implements ChannelFactoryListener<SshServerContext> {

	public static final String CALLBACK_CLIENT = "callbackClient";
	
	SshEngine ssh = new SshEngine();
	Set<CallbackSession> clients = new HashSet<CallbackSession>();
	ExecutorService executor;
	List<SshKeyPair> hostKeys = new ArrayList<>();
	ChannelFactory<SshServerContext> channelFactory;
	List<Object> defaultPolicies = new ArrayList<>();
	FileFactory fileFactory;
	
	public CallbackClient() {
		executor = getExecutorService();
		EventServiceImplementation.getInstance().addListener(new DisconnectionListener());
		channelFactory = new DefaultServerChannelFactory();
	}
	
	public SshEngine getSshEngine() {
		return ssh;
	}
	
	protected ExecutorService getExecutorService() {
		 return Executors.newCachedThreadPool();
	}

	public void setDefaultPolicies(Object... policies) {
		defaultPolicies.addAll(Arrays.asList(policies));
	}
	
	public synchronized CallbackSession start(CallbackConfiguration config) throws IOException {
		return start(config, config.getServerHost(), config.getServerPort());
	}
	
	public synchronized CallbackSession start(CallbackConfiguration config, String hostname, int port) throws IOException {
		CallbackSession session = new CallbackSession(config, this, hostname, port);
		onClientStarting(session);
		start(session);
		return session;
	}
	
	public synchronized void start(CallbackSession client) {
		
		if(Log.isInfoEnabled()) {
			Log.info("Starting client " + client.getConfig().getAgentName());
		}
		executor.execute(client);
	}
	
	void onClientConnected(CallbackSession client, SshConnection connection) {
		clients.add(client);
		onClientStart(client, connection);
	}
	
	public boolean isConnected() {
		return ssh.isStarted() && !clients.isEmpty();
	}
	
	public Collection<CallbackSession> getClients() {
		return clients;
	}
	
	protected void onClientStarting(CallbackSession client) {
		
	}
	
	protected void onClientStopping(CallbackSession client) {
		
	}
	
	protected void onClientStart(CallbackSession client, SshConnection connection) {
		
	}
	
	protected void onClientStop(CallbackSession client, SshConnection connection) {
		
	}
	
	public synchronized void stop(CallbackSession client) {
		
		onClientStopping(client);
		
		if(Log.isInfoEnabled()) {
			Log.info("Stopping callback client");
		}
		
		DisconnectRequestFuture future = client.stop();
		
		if(Log.isInfoEnabled()) {
			Log.info("Callback client has disconnected [{}]", String.valueOf(future.isDone()));
		}
	}
	
	public void stop() {	
		for(CallbackSession client : new ArrayList<CallbackSession>(clients)) {
			stop(client);
		}
	}
	
	public void shutdown() {
		
		for(CallbackSession client : new ArrayList<CallbackSession>(clients)) {
			stop(client);
		}
		
		ssh.shutdownAndExit();
		executor.shutdownNow();
	}
	
	class DisconnectionListener implements EventListener {

		@Override
		public void processEvent(Event evt) {
			
			switch(evt.getId()) {
			case EventCodes.EVENT_DISCONNECTED:
				
				final SshConnection con = (SshConnection)evt.getAttribute(EventCodes.ATTRIBUTE_CONNECTION);
				
				if(!executor.isShutdown()) {
					executor.execute(new Runnable() {
						public void run() {
							if(con.containsProperty(CALLBACK_CLIENT)) {
								CallbackSession client = (CallbackSession) con.getProperty(CALLBACK_CLIENT);
								
								if(Log.isInfoEnabled()) {
									Log.info("Disconnected from {}:{}" , 
											client.getConfig().getServerHost(), 
											client.getConfig().getServerPort());
								}
								
								onClientStop(client, con);
								con.removeProperty(CALLBACK_CLIENT);
								clients.remove(client);
								
								if(!client.isStopped() && client.getConfig().isReconnect()) {
									while(getSshEngine().isStarted()) {
										
										if(Log.isInfoEnabled()) {
											Log.info("Will connect again to {}:{} in {} seconds" , 
													client.getConfig().getServerHost(), 
													client.getConfig().getServerPort(), 
													client.getConfig().getReconnectIntervalMs() / 1000);
										}
										try {
											try {
												Thread.sleep(client.getConfig().getReconnectIntervalMs());
											} catch (InterruptedException e1) {
											}
											client.connect();
											break;
										} catch (IOException e) {
										}
									}
								} else {
									stop();
								}
							} 
						}
					});
				}
				
				break;
			default:
				break;
			}
		}
		
	}

	public SshServerContext createContext(SshEngineContext daemonContext, CallbackConfiguration config) throws IOException, SshException {
		
		SshServerContext sshContext = new SshServerContext(getSshEngine(), JCEComponentManager.getDefaultInstance());
		
		sshContext.setIdleConnectionTimeoutSeconds(0);
		sshContext.setExtendedIdentificationSanitization(false);
		for(SshKeyPair key : hostKeys) {
			sshContext.addHostKey(key);
		}
				
		for(Object policy : defaultPolicies) {
			sshContext.setPolicy(policy.getClass(), policy);
		}
		
		sshContext.setSoftwareVersionComments(String.format("%s_%s", config.getCallbackIdentifier(), config.getAgentName()));
		
		InMemoryMutualKeyAuthenticationStore authenticationStore = new InMemoryMutualKeyAuthenticationStore();
		authenticationStore.addKey(config.getAgentName(), config.getPrivateKey(), config.getPublicKey());
		MutualCallbackAuthenticationProvider provider = new MutualCallbackAuthenticationProvider(authenticationStore);
		sshContext.setAuthenicationMechanismFactory(new CallbackAuthenticationMechanismFactory<>(provider));
		sshContext.getPolicy(AuthenticationPolicy.class).addRequiredMechanism(
				MutualCallbackAuthenticationProvider.MUTUAL_KEY_AUTHENTICATION);
		
		sshContext.setSendIgnorePacketOnIdle(true);
		
		configureForwarding(sshContext, config);
		configureChannels(sshContext, config);
		configureFilesystem(sshContext, config);
		
		configureContext(sshContext, config);
				
		return sshContext;
	}

	protected void configureContext(SshServerContext sshContext, CallbackConfiguration config) {
	}

	protected void configureFilesystem(SshServerContext sshContext, CallbackConfiguration config) {
		sshContext.getPolicy(FileSystemPolicy.class).setFileFactory(fileFactory);
	}

	protected void configureChannels(SshServerContext sshContext, CallbackConfiguration config) {
		sshContext.setChannelFactory(channelFactory);
	}

	protected void configureForwarding(SshServerContext sshContext, CallbackConfiguration config) {
		sshContext.getForwardingPolicy().allowForwarding();
	}

	public void addHostKey(SshKeyPair pair) {
		this.hostKeys.add(pair);
	}

	public void setChannelFactory(ChannelFactory<SshServerContext> channelFactory) {
		this.channelFactory = channelFactory;
	}

	public void setFileFactory(FileFactory fileFactory) {
		this.fileFactory = fileFactory;
	}
	
}

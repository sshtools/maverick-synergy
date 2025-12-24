package com.sshtools.callback.client;

/*-
 * #%L
 * Callback Client API
 * %%
 * Copyright (C) 2002 - 2025 JADAPTIVE Limited
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
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import com.sshtools.common.permissions.Policy;
import com.sshtools.common.policy.FileFactory;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.server.SshServerContext;
import com.sshtools.synergy.ssh.ChannelFactory;
import com.sshtools.synergy.ssh.Connection;

public class ClusteredCallbackClient implements ICallbackClient<ClusteredCallbackClient.ClusteredCallbackSession, IClusteredCallbackConfiguration> {
	
	public static final class ClusteredCallbackSession implements ICallbackSession {
		private final ICallbackConfiguration config;
		private final ArrayList<ICallbackSession> sessions;
		private final ICallbackClient<?, ?> client;

		private ClusteredCallbackSession(ICallbackClient<?, ?> client, ICallbackConfiguration config, ArrayList<ICallbackSession> sessions) {
			this.config = config;
			this.sessions = sessions;
			this.client = client;
		}

		@Override
		public String getName() {
			return String.join(", ", sessions.stream().map(ICallbackSession::getName).toList());
		}

		@Override
		public ICallbackClient<?, ?> getClient() {
			return client;
		}

		@Override
		public ICallbackConfiguration getConfig() {
			return config;
		}

		@Override
		public boolean stop(Duration waitTime) throws InterruptedException {
			var done = true;
			for(var sesh : sessions) {
				done &= sesh.stop(waitTime);
			}
			return done;
		}
	}

	public interface ClusterProvider {
		List<CallbackConfiguration> expand(ICallbackConfiguration config) throws IOException;
	}

	private final List<CallbackClient> activeClients = new CopyOnWriteArrayList<>();
	private final List<SshKeyPair> keyPairs = new ArrayList<>();
	private final List<CallbackClientListener> listeners = new ArrayList<>();

	private ChannelFactory<SshServerContext> channelFactory;
	private Policy[] policies;
	private FileFactory fileFactory;
	
	public ClusteredCallbackClient() {
	}

	@Override
	public void addListener(CallbackClientListener listener) {
		listeners.add(listener);
		activeClients.forEach(clnt -> clnt.addListener(listener));
	}

	@Override
	public int getConnections() {
		return activeClients.stream().collect(Collectors.summingInt(ICallbackClient::getConnections));
	}

	@Override
	public void removeListener(CallbackClientListener listener) {
		listeners.remove(listener);
		activeClients.forEach(clnt -> clnt.removeListener(listener));
	}

	@Override
	public ClusteredCallbackSession start(IClusteredCallbackConfiguration config) throws IOException {
		var sessions = new ArrayList<ICallbackSession>();
		startClient(config, sessions);
		return new ClusteredCallbackSession(this, config, sessions);
	}

	private void startClient(IClusteredCallbackConfiguration config, ArrayList<ICallbackSession> sessions) throws IOException {
		for(CallbackConfiguration addr : config.getProvider().expand(config)) {
			var nodeCb = new CallbackClient();
			
			if(channelFactory != null) {
				nodeCb.setChannelFactory(channelFactory);
			}
			
			if(fileFactory != null) {
				nodeCb.setFileFactory(fileFactory);
			}
			
			if(policies != null) {
				nodeCb.setPolicyDefaults(policies);
			}
			
			keyPairs.forEach(nodeCb::addHostKey);
			nodeCb.addListener(new CallbackClientListener() {
				@Override
				public void onClientStop(ICallbackSession client, Connection<?> con) {
					activeClients.remove(client.getClient());
				}
			});
			listeners.forEach(lnstr -> nodeCb.addListener(lnstr));
			
			activeClients.add(nodeCb);
			sessions.add(nodeCb.start(addr));
		}
	}

	@Override
	public boolean isConnected() {
		for(var clnt : activeClients) {
			if(clnt.isConnected()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void stop() {
		activeClients.forEach(ICallbackClient::stop);
	}

	@Override
	public void addHostKey(SshKeyPair pair) {
		keyPairs.add(pair);
		activeClients.forEach(clnt -> clnt.addHostKey(pair));
	}

	@Override
	public void setFileFactory(FileFactory fileFactory) {
		if(!Objects.equals(fileFactory, this.fileFactory)) {
			this.fileFactory = fileFactory;
			activeClients.forEach(clnt -> clnt.setFileFactory(fileFactory));
		}
	}

	@Override
	public void waitForShutdown() {
		activeClients.forEach(ICallbackClient::waitForShutdown);
	}

	@Override
	public Throwable getLastError() {
		for(var clnt : activeClients) {
			if(clnt.getLastError() != null) {
				return clnt.getLastError();
			}
		}
		return null;
	}

	@Override
	public void setPolicyDefaults(Policy... policies) {
		if(!Objects.equals(policies, this.policies)) {
			this.policies = policies;
			activeClients.forEach(clnt -> clnt.setPolicyDefaults(policies));
		}
	}

	@Override
	public void setChannelFactory(ChannelFactory<SshServerContext> channelFactory) {
		if(!Objects.equals(channelFactory, this.channelFactory)) {
			this.channelFactory = channelFactory;
			activeClients.forEach(clnt -> clnt.setChannelFactory(channelFactory));
		}
	}

	@Override
	public void close() {
		activeClients.forEach(ICallbackClient::close);
	}

}

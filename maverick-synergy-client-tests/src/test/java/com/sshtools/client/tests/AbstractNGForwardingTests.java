package com.sshtools.client.tests;

/*-
 * #%L
 * Client API Tests
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

import com.sshtools.client.SshClient;
import com.sshtools.client.SshClient.SshClientBuilder;
import com.sshtools.common.forwarding.ForwardingPolicy.ForwardingPolicyBuilder;
import com.sshtools.common.forwarding.ForwardingRequest;
import com.sshtools.common.logger.Log;
import com.sshtools.common.logger.Log.Level;
import com.sshtools.common.permissions.UnauthorizedException;
import com.sshtools.common.publickey.InvalidPassphraseException;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.tests.AbstractForwardingTests;
import com.sshtools.common.tests.ForwardingConfiguration;
import com.sshtools.common.tests.ForwardingTestTemplate;
import com.sshtools.common.tests.TestConfiguration;
import com.sshtools.synergy.ssh.UnixDomainSockets;

public abstract class AbstractNGForwardingTests extends AbstractForwardingTests<SshClient> {

	@Override
	protected void enableLogging(ForwardingConfiguration config) {
		Log.getDefaultContext().enableConsole(Level.valueOf(config.getLoggingLevel()));
	}

	@Override
	protected void log(String msg) {
		Log.info(msg);
	}

	@Override
	protected ForwardingTestTemplate<SshClient, String> createLocalDomainSocketForwardingTemplate() {
		return new ForwardingTestTemplate<SshClient, String>() {
			
			@Override
			public String startForwarding(SshClient client, String targetPath) throws UnauthorizedException, SshException {
		        var tmp = UnixDomainSockets.createTemporayAddress().getPath().toString();
				return client.bindLocal(ForwardingRequest.ofDomainSocket(tmp, targetPath)).boundPath().get();
			}

			@Override
			public SshClient createClient(TestConfiguration config) throws IOException, SshException, InvalidPassphraseException {
				return createBuilder(config).
						onConfigure(ctx -> {
							ctx.setKeyExchangeTransferLimit(config.getKeyExchangeLimit());
							ctx.setPolicy(ForwardingPolicyBuilder.create().
									allowUnixDomainSocketForwarding().
									build());			
						}).
						build();
			}

			@Override
			public void disconnect(SshClient client) {
				client.disconnect();
			}
		};
	}

	@Override
	protected ForwardingTestTemplate<SshClient, Integer> createLocalForwardingTemplate() {
		return new ForwardingTestTemplate<SshClient, Integer>() {
			
			@Override
			public Integer startForwarding(SshClient client, Integer targetPort) throws UnauthorizedException, SshException {
				return client.bindLocal(ForwardingRequest.ofTcp("127.0.0.1", 0, "127.0.0.1", targetPort)).boundPort().orElse(0);
			}

			@Override
			public SshClient createClient(TestConfiguration config) throws IOException, SshException, InvalidPassphraseException {
				return createBuilder(config).
						onConfigure(ctx -> {
							ctx.setKeyExchangeTransferLimit(config.getKeyExchangeLimit());
							ctx.setPolicy(ForwardingPolicyBuilder.create().
									allowTCPForwarding().
									build());			
						}).
						build();
			}

			@Override
			public void disconnect(SshClient client) {
				client.disconnect();
			}
		};
	}
	
	@Override
	protected ForwardingTestTemplate<SshClient, Integer> createRemoteForwardingTemplate() {
		return new ForwardingTestTemplate<SshClient, Integer>() {
			
			@Override
			public Integer startForwarding(SshClient client, Integer targetPort) throws UnauthorizedException, SshException {
				return client.bindRemote(ForwardingRequest.ofTcp("127.0.0.1", 0, "127.0.0.1", targetPort)).boundPort().orElse(0);
			}
			
			@Override
			public SshClient createClient(TestConfiguration config) throws IOException, SshException, InvalidPassphraseException {
				return createBuilder(config).
						onConfigure(ctx -> {
							ctx.setKeyExchangeTransferLimit(config.getKeyExchangeLimit());
							ctx.setPolicy(ForwardingPolicyBuilder.create().
									allowTCPForwarding().
									build());						
						}).
						build();
			}
			
			@Override
			public void disconnect(SshClient client) {
				client.disconnect();
			}
		};
	}
	
	@Override
	protected ForwardingTestTemplate<SshClient, String> createRemoteDomainSocketForwardingTemplate() {
		return new ForwardingTestTemplate<SshClient, String>() {
			
			@Override
			public String startForwarding(SshClient client, String targetPath) throws UnauthorizedException, SshException {
		        var tmp = UnixDomainSockets.createTemporayAddress().getPath().toString();
				return client.bindRemote(ForwardingRequest.ofDomainSocket(tmp, targetPath)).request().bindPath();
			}
			
			@Override
			public SshClient createClient(TestConfiguration config) throws IOException, SshException, InvalidPassphraseException {
				return createBuilder(config).
						onConfigure(ctx -> {
							ctx.setKeyExchangeTransferLimit(config.getKeyExchangeLimit());
							ctx.setPolicy(ForwardingPolicyBuilder.create().
									allowUnixDomainSocketForwarding().
									build());						
						}).
						build();
			}
			
			@Override
			public void disconnect(SshClient client) {
				client.disconnect();
			}
		};
	}

	private SshClientBuilder createBuilder(TestConfiguration config)
			throws IOException, InvalidPassphraseException {
		return SshClientBuilder.create().
				withTarget(config.getHostname(), config.getPort()).
				withUsername(config.getUsername()).
				withConnectTimeout(5000L).
				withPassword(config.getPassword()).
				withIdentities(config.getIdentities());
	}

}

package com.sshtools.client.tests;

import java.io.IOException;

import com.sshtools.client.SshClient;
import com.sshtools.common.logger.Log;
import com.sshtools.common.logger.Log.Level;
import com.sshtools.common.permissions.UnauthorizedException;
import com.sshtools.common.publickey.InvalidPassphraseException;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.tests.AbstractForwardingTests;
import com.sshtools.common.tests.ForwardingConfiguration;
import com.sshtools.common.tests.ForwardingTestTemplate;
import com.sshtools.common.tests.TestConfiguration;

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
	protected ForwardingTestTemplate<SshClient> createLocalForwardingTemplate() {
		return new ForwardingTestTemplate<SshClient>() {
			
			@Override
			public int startForwarding(SshClient client, int targetPort) throws UnauthorizedException, SshException {
				return client.startLocalForwarding("127.0.0.1", 0, "127.0.0.1", targetPort);
			}

			@Override
			public SshClient createClient(TestConfiguration config) throws IOException, SshException, InvalidPassphraseException {
				SshClient client = new SshClient(config.getHostname(), 
				config.getPort(), 
				config.getUsername(), 
				5000L,
				config.getPassword(), 
				config.getIdentities());
				
				client.getContext().setKeyExchangeTransferLimit(config.getKeyExchangeLimit());
				client.getContext().getForwardingPolicy().allowForwarding();
				
				return client;
			}

			@Override
			public void disconnect(SshClient client) {
				client.disconnect();
			}
		};
	}
	
	@Override
	protected ForwardingTestTemplate<SshClient> createRemoteForwardingTemplate() {
		return new ForwardingTestTemplate<SshClient>() {
			
			@Override
			public int startForwarding(SshClient client, int targetPort) throws UnauthorizedException, SshException {
				return client.startRemoteForwarding("127.0.0.1", 0, "127.0.0.1", targetPort);
			}
			
			@Override
			public SshClient createClient(TestConfiguration config) throws IOException, SshException, InvalidPassphraseException {
				SshClient client = new SshClient(config.getHostname(), 
				config.getPort(), 
				config.getUsername(), 
				5000L,
				config.getPassword(), 
				config.getIdentities());
				
				client.getContext().setKeyExchangeTransferLimit(config.getKeyExchangeLimit());
				client.getContext().getForwardingPolicy().allowForwarding();
				
				return client;
			}
			
			@Override
			public void disconnect(SshClient client) {
				client.disconnect();
			}
		};
	}

}

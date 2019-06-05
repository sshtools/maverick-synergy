package com.sshtools.common.tests;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import com.sshtools.common.knownhosts.KnownHostsKeyVerification;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshPublicKey;

public class LegacyKnownHostsTests extends AbstractKnownHostsTests {

	@Override
	public KnownHostsKeyVerification loadKnownHosts(InputStream in) throws SshException, IOException {
		return new LegacyKnownHostsKeyVerification(in);
	}

	
	class LegacyKnownHostsKeyVerification extends KnownHostsKeyVerification {

		public LegacyKnownHostsKeyVerification(InputStream knownHosts) throws SshException, IOException {
			super(knownHosts);
		}

		@Override
		public void onHostKeyMismatch(String host, List<SshPublicKey> allowedHostKey, SshPublicKey actualHostKey)
				throws SshException {
			
		}

		@Override
		public void onUnknownHost(String host, SshPublicKey key) throws SshException {
			
		}
		
	}
}

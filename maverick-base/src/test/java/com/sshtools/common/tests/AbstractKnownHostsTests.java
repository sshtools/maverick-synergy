
package com.sshtools.common.tests;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Ignore;

import com.sshtools.common.knownhosts.HostKeyVerification;
import com.sshtools.common.knownhosts.KnownHostsKeyVerification;
import com.sshtools.common.publickey.SshKeyUtils;
import com.sshtools.common.ssh.SshException;

import junit.framework.TestCase;

@Ignore
public abstract class AbstractKnownHostsTests extends TestCase {

	public abstract KnownHostsKeyVerification loadKnownHosts(InputStream in) throws SshException, IOException;

	
	public void testLoading() throws SshException, IOException {
		loadKnownHosts(getClass().getResourceAsStream("/known_hosts"));
	}
	
	public void testOutputMatches() throws SshException, IOException {
		
		
		HostKeyVerification k = loadKnownHosts(getClass().getResourceAsStream("/known_hosts"));
		
		String output = k.toString();
		
		System.out.println("----GENERATED-----");
		System.out.println(output);
		System.out.println("----END-----");
		
		String originalContent = IOUtils.toString(getClass().getResourceAsStream("/known_hosts"), "UTF-8");
		
		System.out.println("----ORIGINAL-----");
		System.out.println(originalContent);
		System.out.println("----END-----");
		/**
		 * Assert the original and output are the same, disregard newline convention which may differ
		 * between operating systems.
		 */
		assertEquals("The known_hosts file should be written exactly as it was read", 
				output.trim().replace("\r", "").replace("\n", ""), 
				originalContent.trim().replace("\r", "").replace("\n", ""));
	}
	
	public void testValidEntries() throws SshException, IOException {
		
		HostKeyVerification k = loadKnownHosts(getClass().getResourceAsStream("/known_hosts"));
		
		assertTrue(k.verifyHost("localhost", SshKeyUtils.getPublicKey(getClass().getResourceAsStream("/openssh/rsa1024.pub"))));
		assertTrue(k.verifyHost("127.0.0.1", SshKeyUtils.getPublicKey(getClass().getResourceAsStream("/openssh/rsa1024.pub"))));
		
	}
	
	public void testNonStandardPortEntries() throws SshException, IOException {
		
		HostKeyVerification k = loadKnownHosts(getClass().getResourceAsStream("/known_hosts"));
		
		assertTrue(k.verifyHost("[localhost]:4022", SshKeyUtils.getPublicKey(getClass().getResourceAsStream("/openssh/rsa2048.pub"))));
		assertTrue(k.verifyHost("[127.0.0.1]:4022", SshKeyUtils.getPublicKey(getClass().getResourceAsStream("/openssh/rsa2048.pub"))));
		
	}
	
	public void testHashedEntries() throws SshException, IOException {
		
		HostKeyVerification k = loadKnownHosts(getClass().getResourceAsStream("/known_hosts"));
		
		assertTrue(k.verifyHost("[localhost]:4022", SshKeyUtils.getPublicKey(getClass().getResourceAsStream("/openssh/rsa1024.pub"))));
		assertTrue(k.verifyHost("[127.0.0.1]:4022", SshKeyUtils.getPublicKey(getClass().getResourceAsStream("/openssh/rsa1024.pub"))));
		
	}

	public void testWildcardEntries() throws SshException, IOException {
		
		HostKeyVerification k = loadKnownHosts(getClass().getResourceAsStream("/known_hosts"));
		
		assertTrue(k.verifyHost("node1.sshtools.com", SshKeyUtils.getPublicKey(getClass().getResourceAsStream("/openssh/rsa1024.pub"))));
		assertTrue(k.verifyHost("node2.sshtools.com", SshKeyUtils.getPublicKey(getClass().getResourceAsStream("/openssh/rsa1024.pub"))));
		assertFalse(k.verifyHost("node2xsshtoolsxcom", SshKeyUtils.getPublicKey(getClass().getResourceAsStream("/openssh/rsa1024.pub"))));
		
		assertTrue(k.verifyHost("server1.sshtools.net", SshKeyUtils.getPublicKey(getClass().getResourceAsStream("/openssh/rsa1024.pub"))));
		assertTrue(k.verifyHost("server2.sshtools.net", SshKeyUtils.getPublicKey(getClass().getResourceAsStream("/openssh/rsa1024.pub"))));
		assertFalse(k.verifyHost("server22.sshtools.net", SshKeyUtils.getPublicKey(getClass().getResourceAsStream("/openssh/rsa1024.pub"))));
		assertFalse(k.verifyHost("server1xsshtoolsxnet", SshKeyUtils.getPublicKey(getClass().getResourceAsStream("/openssh/rsa1024.pub"))));
		
	}
	
	public void testNegatedEntries() throws SshException, IOException {
		
		HostKeyVerification k = loadKnownHosts(getClass().getResourceAsStream("/known_hosts"));
		
		assertFalse(k.verifyHost("localhost", SshKeyUtils.getPublicKey(getClass().getResourceAsStream("/openssh/ecdsa256.pub"))));
		assertTrue(k.verifyHost("127.0.0.2", SshKeyUtils.getPublicKey(getClass().getResourceAsStream("/openssh/ecdsa256.pub"))));
	}
	
	public void testRevokedEntries() throws SshException, IOException {
		
		HostKeyVerification k = loadKnownHosts(getClass().getResourceAsStream("/known_hosts"));
		
		assertFalse(k.verifyHost("localhost", SshKeyUtils.getPublicKey(getClass().getResourceAsStream("/openssh/ecdsa384.pub"))));
		assertFalse(k.verifyHost("127.0.0.1", SshKeyUtils.getPublicKey(getClass().getResourceAsStream("/openssh/ecdsa384.pub"))));
		assertFalse(k.verifyHost("sshtools.com", SshKeyUtils.getPublicKey(getClass().getResourceAsStream("/openssh/ecdsa384.pub"))));
		
	}
	
	public void testNonCanonicalEntries() throws SshException, IOException {
		
		KnownHostsKeyVerification k = loadKnownHosts(getClass().getResourceAsStream("/known_hosts"));
		k.setUseCanonicalHostnames(false);
		
		assertFalse(k.verifyHost("127.0.0.1", SshKeyUtils.getPublicKey(getClass().getResourceAsStream("/openssh/dsa1024.pub"))));
		assertTrue(k.verifyHost("localhost", SshKeyUtils.getPublicKey(getClass().getResourceAsStream("/openssh/dsa1024.pub"))));
		
		// These are hashed entries, only the first is valid
		assertFalse(k.verifyHost("[127.0.0.1]:4022", SshKeyUtils.getPublicKey(getClass().getResourceAsStream("/openssh/rsa1024.pub"))));
	}
	
	public void testNonReverseDNSEntries() throws SshException, IOException {
		
		KnownHostsKeyVerification k = loadKnownHosts(getClass().getResourceAsStream("/known_hosts"));
		k.setUseReverseDNS(false);
		k.setUseCanonicalHostnames(false);
		
		assertTrue(k.verifyHost("127.0.0.1", SshKeyUtils.getPublicKey(getClass().getResourceAsStream("/openssh/ecdsa521.pub"))));
		assertFalse(k.verifyHost("localhost", SshKeyUtils.getPublicKey(getClass().getResourceAsStream("/openssh/ecdsa521.pub"))));
		
		// These are hashed entries, only the first is valid
		assertTrue(k.verifyHost("[localhost]:4022", SshKeyUtils.getPublicKey(getClass().getResourceAsStream("/openssh/rsa1024.pub"))));
		assertFalse(k.verifyHost("[127.0.0.1]:4022", SshKeyUtils.getPublicKey(getClass().getResourceAsStream("/openssh/rsa1024.pub"))));
		
	}
}

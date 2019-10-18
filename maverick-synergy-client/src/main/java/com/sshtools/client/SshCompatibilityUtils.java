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
package com.sshtools.client;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshPublicKey;

public class SshCompatibilityUtils {

	public static String[] getSupportedHostKeyAlgorithms(String hostname, int port) throws SshException, IOException {

		try(SshClient ssh = new SshClient(hostname, port, "guest")) {
			return ssh.getRemotePublicKeys();
		}
	}

	public static SshPublicKey getHostKey(String hostname, int port)
			throws SshException, IOException {
		return getHostKey(hostname, port, null);
	}

	public static Set<SshPublicKey> getSupportedHostKeys(String hostname, int port) throws SshException, IOException {

		Set<SshPublicKey> hostkeys = new HashSet<SshPublicKey>();

		for (String algorithm : getSupportedHostKeyAlgorithms(hostname, port)) {
			hostkeys.add(getHostKey(hostname, port, algorithm));
		}

		return hostkeys;
	}

	public static SshPublicKey getHostKey(String hostname, int port, String algorithm) throws SshException, IOException {
		
		try(SshClient ssh = new SshClient(hostname, port, "guest") {
			public void configure(SshClientContext context) {
				context.setPreferredPublicKey(algorithm);
			}
		}) {
			return ssh.getConnection().getHostKey();
		}
	}
	

	public static SshConnection getRemoteConfiguration(String hostname, int port) throws IOException, SshException {
		
		try(SshClient ssh = new SshClient(hostname, port, "guest")) {
			return ssh.getConnection();
		}
	}
	
	public static SshClient getRemoteClient(String hostname, int port, String username, String password, boolean tcpNoDelay) throws SshException, IOException {
		
		SshClient ssh = new SshClient(hostname, port, username, password.toCharArray()) {
			public void configure(SshClientContext context) {
				context.setSocketOptionTcpNoDelay(tcpNoDelay);
			}
		};
		
		ssh.authenticate(new PasswordAuthenticator(password), 30000L);
		
		if(!ssh.isAuthenticated()) {
			throw new IOException("Bas username or password");
		}
		
		return ssh;
		
	}

	static class ConfigurationCollector  {

		SshConnection con;


		public String getRemoteIdentification() {
			return con.getRemoteIdentification();
		}
		
		public Set<String> getSupportedHostKeys() {
			return new HashSet<String>(Arrays.asList(con.getRemotePublicKeys()));
		}
		
		public Set<String> getSupportedKeyExchanges() {
			return new HashSet<String>(Arrays.asList(con.getRemoteKeyExchanges()));
		}
		
		public Set<String> getSupportedCompressions() {
			Set<String> tmp = new HashSet<>();
			tmp.addAll(Arrays.asList(con.getRemoteCompressionsCS()));
			tmp.addAll(Arrays.asList(con.getRemoteCompressionsSC()));
			return tmp;
		}
		
		public Set<String> getSupportedCiphers() {
			Set<String> tmp = new HashSet<>();
			tmp.addAll(Arrays.asList(con.getRemoteCiphersCS()));
			tmp.addAll(Arrays.asList(con.getRemoteCiphersSC()));
			return tmp;
		}
		
		public Set<String> getSupportedMacs() {
			Set<String> tmp = new HashSet<>();
			tmp.addAll(Arrays.asList(con.getRemoteMacsCS()));
			tmp.addAll(Arrays.asList(con.getRemoteMacsSC()));
			return tmp;
		}
		
		public SshPublicKey getKey() {
			return con.getHostKey();
		}

		public String getNegotiatedKeyExchange() {
			return con.getKeyExchangeInUse();
		}
		
		public String getNegotiatedHostKey() {
			return con.getHostKeyInUse();
		}
		
		public String getNegotiatedCipherCS() {
			return con.getCipherInUseCS();
		}
		
		public String getNegotiatedCipherSC() {
			return con.getCipherInUseSC();
		}
		
		public String getNegotiatedMacCS() {
			return con.getMacInUseCS();
		}
		
		public String getNegotiatedMacSC() {
			return con.getMacInUseSC();
		}
		
		public String getNegotiatedCompressionCS() {
			return con.getCompressionInUseCS();
		}
		
		public String getNegotiatedCompressionSC() {
			return con.getCompressionInUseSC();
		}
		
	}

}

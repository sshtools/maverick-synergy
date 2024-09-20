package com.sshtools.common.auth;

/*-
 * #%L
 * Base API
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.publickey.SshKeyUtils;
import com.sshtools.common.publickey.SshPublicKeyFile;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshPublicKey;

public class UniversalAuthenticationProvider implements PublicKeyAuthenticationProvider {

	String hostname;
	int port;
	
	UniversalAuthenticatorAccountDatabase accountDatabase;
	
	public UniversalAuthenticationProvider(UniversalAuthenticatorAccountDatabase accountDatabase) {
		this(accountDatabase, "gateway.jadaptive.com", 443);
	}
	
	public UniversalAuthenticationProvider(UniversalAuthenticatorAccountDatabase accountDatabase, String hostname) {
		this(accountDatabase, hostname, 443);
	}
	
	public UniversalAuthenticationProvider(UniversalAuthenticatorAccountDatabase accountDatabase, String hostname, int port) {
		this.accountDatabase = accountDatabase;
		this.hostname = hostname;
		this.port = port;
	}
	
	@Override
	public boolean isAuthorizedKey(SshPublicKey key, SshConnection con) throws IOException {
		
		Set<String> gatewayAccounts = accountDatabase.getAccounts(con.getUsername());
		if(gatewayAccounts==null) {
			return false;
		}
		for(String gatewayAccount : gatewayAccounts) {
			for(SshPublicKey gatewayKey : getGatewayKeys(gatewayAccount)) {
				if(gatewayKey.equals(key)) {
					return true;
				}
			}
		}
		return false;
		
	}

	private Collection<SshPublicKey> getGatewayKeys(String username) throws IOException {
		
		StringBuffer buf = new StringBuffer();
		buf.append("https://");
		buf.append(hostname);
		if(port!=443) {
			buf.append(":");
			buf.append(port);
		}
		buf.append("/app/api/agent/authorizedKeys/");
		buf.append(username);
		
		URL url = new URL(buf.toString());
		InputStream in = url.openStream();
		Collection<SshPublicKey> keys = new ArrayList<>();
		
		try(BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
			String line;
			while((line = reader.readLine()) != null) {
				if(line.startsWith("#")) {
					continue;
				}
				keys.add(SshKeyUtils.getPublicKey(line));
			}
		}
		
		return keys;
	}
	
	@Override
	public Iterator<SshPublicKeyFile> getKeys(SshConnection con)
			throws PermissionDeniedException, IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void remove(SshPublicKey key, SshConnection con)
			throws IOException, PermissionDeniedException, SshException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void add(SshPublicKey key, String comment, SshConnection con)
			throws IOException, PermissionDeniedException, SshException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean checkKey(SshPublicKey key, SshConnection con) throws IOException {
		return isAuthorizedKey(key, con);
	}
}

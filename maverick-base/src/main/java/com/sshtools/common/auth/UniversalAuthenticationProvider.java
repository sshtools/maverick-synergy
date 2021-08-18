/*
 *    _           _             _   _
 *   (_) __ _  __| | __ _ _ __ | |_(_)_   _____
 *   | |/ _` |/ _` |/ _` | '_ \| __| \ \ / / _ \
 *   | | (_| | (_| | (_| | |_) | |_| |\ V /  __/
 *  _/ |\__,_|\__,_|\__,_| .__/ \__|_| \_/ \___|
 * |__/                  |_|
 *
 * This file is part of the Maverick Synergy Hotfixes Java SSH API
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Copyright (C) 2002-2021 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
 */
package com.sshtools.common.auth;

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

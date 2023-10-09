package com.sshtools.common.auth;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class InMemoryUniversalAuthenticatorAccountDatabase implements UniversalAuthenticatorAccountDatabase {

	Map<String,Set<String>> usernameToGatewayAccounts = new HashMap<>();
	
	public InMemoryUniversalAuthenticatorAccountDatabase mapUser(String username, String gatewayAccount) {
		Set<String> accounts = usernameToGatewayAccounts.get(username);
		if(accounts==null) {
			accounts = new HashSet<>();
			usernameToGatewayAccounts.put(username, accounts);
		}
		accounts.add(gatewayAccount);
		return this;
	}

	@Override
	public Set<String> getAccounts(String username) {
		return usernameToGatewayAccounts.get(username);
	}
}

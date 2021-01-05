/**
 * (c) 2002-2021 JADAPTIVE Limited. All Rights Reserved.
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

package com.sshtools.common.auth;

import java.util.Set;

public interface UniversalAuthenticatorAccountDatabase {

	Set<String> getAccounts(String username);

}

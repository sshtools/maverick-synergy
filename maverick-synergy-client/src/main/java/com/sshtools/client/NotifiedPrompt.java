package com.sshtools.client;

@FunctionalInterface
public interface NotifiedPrompt {
	void completed(boolean success, String value, ClientAuthenticator authenticator);
}

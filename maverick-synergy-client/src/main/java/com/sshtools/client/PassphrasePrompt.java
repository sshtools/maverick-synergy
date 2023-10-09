package com.sshtools.client;

@FunctionalInterface
public interface PassphrasePrompt extends NotifiedPrompt {

	String getPasshrase(String keyinfo);
	
	@Override
	default void completed(boolean success, String value, ClientAuthenticator authenticator) {
	}
}

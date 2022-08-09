package com.sshtools.client;

@FunctionalInterface
public interface PassphrasePrompt {

	String getPasshrase(String keyinfo);
}

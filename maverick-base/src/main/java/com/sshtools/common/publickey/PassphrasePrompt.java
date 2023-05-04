package com.sshtools.common.publickey;

import java.io.IOException;

@FunctionalInterface
public interface PassphrasePrompt {

	String getPasshrase(String keyinfo) throws IOException;

}
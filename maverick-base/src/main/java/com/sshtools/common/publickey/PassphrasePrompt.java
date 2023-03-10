package com.sshtools.common.publickey;

import java.io.IOException;

import com.sshtools.common.ssh.SshException;

@FunctionalInterface
public interface PassphrasePrompt {

	String getPasshrase(String keyinfo) throws IOException;

}
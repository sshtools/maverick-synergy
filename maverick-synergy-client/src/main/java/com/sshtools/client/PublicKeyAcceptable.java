package com.sshtools.client;

import java.nio.file.Path;

import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.SshPublicKey;

public interface PublicKeyAcceptable {

	SshKeyPair acceptedKey(Path path, SshPublicKey key);
}

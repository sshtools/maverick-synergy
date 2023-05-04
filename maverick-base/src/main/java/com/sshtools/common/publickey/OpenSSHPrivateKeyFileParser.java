package com.sshtools.common.publickey;

import java.io.IOException;

import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;

public interface OpenSSHPrivateKeyFileParser {

	void decode(ByteArrayReader privateReader, SshKeyPair pair) throws IOException;

	void encode(ByteArrayWriter privateWriter, SshKeyPair pair) throws IOException;

}

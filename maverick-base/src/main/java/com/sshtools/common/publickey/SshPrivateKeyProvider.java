package com.sshtools.common.publickey;

import java.io.IOException;

public interface SshPrivateKeyProvider {
	boolean isFormatted(byte[] formattedkey);

	SshPrivateKeyFile create(byte[] formattedkey) throws IOException;
}

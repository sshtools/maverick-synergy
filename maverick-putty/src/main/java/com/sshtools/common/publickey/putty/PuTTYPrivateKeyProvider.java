package com.sshtools.common.publickey.putty;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import com.sshtools.common.publickey.SshPrivateKeyFile;
import com.sshtools.common.publickey.SshPrivateKeyProvider;

public class PuTTYPrivateKeyProvider implements SshPrivateKeyProvider {

	@Override
	public boolean isFormatted(byte[] formattedkey) {
		BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(formattedkey)));
		try {
			String line = reader.readLine();

			return (line != null && (line.startsWith("PuTTY-User-Key-File-3:")
					|| line.startsWith("PuTTY-User-Key-File-2:") || line.equals("PuTTY-User-Key-File-1:")));
		} catch (IOException ex) {
			return false;
		}
	}

	@Override
	public SshPrivateKeyFile create(byte[] formattedkey) throws IOException {
		if (!isFormatted(formattedkey)) {
			throw new IOException("Key is not formatted in the PuTTY key format!");
		}
		return new PuTTYPrivateKeyFile(formattedkey);
	}

}

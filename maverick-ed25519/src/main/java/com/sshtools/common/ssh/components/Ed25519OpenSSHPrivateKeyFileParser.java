package com.sshtools.common.ssh.components;

import java.io.IOException;

import com.sshtools.common.publickey.OpenSSHPrivateKeyFileParser;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;

public class Ed25519OpenSSHPrivateKeyFileParser implements OpenSSHPrivateKeyFileParser {

	@Override
	public void decode(ByteArrayReader privateReader, SshKeyPair pair) throws IOException {
		  byte[] publicKey = privateReader.readBinaryString();
		  byte[] privateKey = privateReader.readBinaryString();
		  pair.setPrivateKey(new SshEd25519PrivateKey(privateKey, publicKey));
	}

	@Override
	public void encode(ByteArrayWriter privateWriter, SshKeyPair pair) throws IOException {

		privateWriter.writeBinaryString(((SshEd25519PublicKey)pair.getPublicKey()).getA());
		  byte[] sk = ((SshEd25519PrivateKey)pair.getPrivateKey()).getSeed();
		  byte[] h = ((SshEd25519PrivateKey)pair.getPrivateKey()).getH();
		  privateWriter.writeInt(64);
		  privateWriter.write(sk);
		  privateWriter.write(h);
	}

}

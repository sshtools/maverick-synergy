
package com.sshtools.common.publickey.bc;

import java.io.IOException;
import java.io.StringWriter;

import org.bouncycastle.openssl.jcajce.JcaPEMWriter;

import com.sshtools.common.publickey.SshPublicKeyFile;
import com.sshtools.common.ssh.components.SshPublicKey;

public class OpenSSLPublicKeyFileBC implements SshPublicKeyFile {

	byte[] formattedKey;
	SshPublicKey key;
	String comment;
	OpenSSLPublicKeyFileBC(SshPublicKey key, String comment) {
		this.key = key;
		this.comment = comment;
	}
	
	@Override
	public SshPublicKey toPublicKey() throws IOException {
		return key;
	}

	@Override
	public String getComment() {
		return comment;
	}

	@Override
	public byte[] getFormattedKey() throws IOException {
		
		StringWriter out = new StringWriter();
		try(JcaPEMWriter writer = new JcaPEMWriter(out)) {
			writer.writeObject(key.getJCEPublicKey());
			writer.flush();
			return out.toString().getBytes("UTF-8");
		}
	}

	@Override
	public String getOptions() {
		return "";
	}

}

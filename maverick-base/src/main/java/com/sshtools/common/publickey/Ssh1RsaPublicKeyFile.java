/* HEADER */
package com.sshtools.common.publickey;

import java.io.IOException;
import java.math.BigInteger;
import java.util.StringTokenizer;

import com.sshtools.common.ssh.components.ComponentManager;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.ssh.components.SshRsaPublicKey;

/**
 * @author Lee David Painter
 */
class Ssh1RsaPublicKeyFile implements SshPublicKeyFile {

	String formattedkey;

	public Ssh1RsaPublicKeyFile(byte[] formattedkey) throws IOException {
		this.formattedkey = new String(formattedkey);
		toPublicKey(); // Validate
	}

	public Ssh1RsaPublicKeyFile(SshPublicKey key) throws IOException {

		if (key instanceof SshRsaPublicKey) {
			formattedkey = String.valueOf(((SshRsaPublicKey) key).getModulus()
					.bitLength())
					+ " "
					+ ((SshRsaPublicKey) key).getPublicExponent()
					+ " "
					+ ((SshRsaPublicKey) key).getModulus();
			toPublicKey(); // Validate
		} else {
			throw new IOException("SSH1 public keys must be rsa");
		}
	}

	public byte[] getFormattedKey() {
		return formattedkey.getBytes();
	}

	public SshPublicKey toPublicKey() throws IOException {
		StringTokenizer tokens = new StringTokenizer(formattedkey.trim(), " ");

		try {
			// int bitlength =
			Integer.parseInt((String) tokens.nextElement());
			String e = (String) tokens.nextElement();
			String n = (String) tokens.nextElement();

			BigInteger publicExponent = new BigInteger(e);
			BigInteger modulus = new BigInteger(n);

			return ComponentManager.getInstance().createRsaPublicKey(modulus,
					publicExponent);
		} catch (Throwable ex) {
			throw new IOException("Invalid SSH1 public key format");
		}

	}

	public String getComment() {
		return "";
	}

	public String getOptions() {
		return null;
	}

	public static boolean isFormatted(byte[] formattedkey) {

		try {
			StringTokenizer tokens = new StringTokenizer(new String(formattedkey, "UTF-8"), " ");
			
			Integer.parseInt((String) tokens.nextElement());
			tokens.nextElement();
			tokens.nextElement();

			return true;
		} catch (Throwable ex) {
			return false;
		}
	}

}

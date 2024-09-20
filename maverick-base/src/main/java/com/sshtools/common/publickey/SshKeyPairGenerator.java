package com.sshtools.common.publickey;

/*-
 * #%L
 * Base API
 * %%
 * Copyright (C) 2002 - 2024 JADAPTIVE Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.io.IOException;
import java.util.ServiceLoader;

import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.ComponentFactory;
import com.sshtools.common.ssh.components.ComponentManager;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.jce.JCEComponentManager;

/**
 * <p>
 * Generate public/private key pairs.
 * </p>
 * <p>
 * To generate a new pair use the following code <blockquote>
 * 
 * <pre>
 * SshKeyPair pair = SshKeyPairGenerator.generateKeyPair(SshKeyPairGenerator.SSH2_RSA, 1024);
 * </pre>
 * 
 * </blockquote> To create formatted key file for the public key use:
 * <blockquote>
 * 
 * <pre>
 * SshPublicKeyFile pubfile = SshPublicKeyFileFactory.create(pair.getPublicKey(), "Some comment",
 * 		SshPublicKeyFileFactory.OPENSSH_FORMAT);
 * FileOutputStream fout = new FileOutputStream("mykey.pub");
 * fout.write(pubfile.getFormattedKey());
 * fout.close();
 * </pre>
 * 
 * <blockquote> To create a formatted, encrypted private key file use:
 * <blockquote>
 * 
 * <pre>
 * SshPrivateKeyFile prvfile = SshPrivateKeyFileFactory.create(pair, "my passphrase", "Some comment",
 * 		SshPrivateKeyFileFactory.OPENSSH_FORMAT);
 * FileOutputStream fout = new FileOutputStream("mykey");
 * fout.write(prvfile.getFormattedKey());
 * fout.close();
 * </pre>
 * 
 * <blockquote>
 * </p>
 * 
 * @author Lee David Painter
 */
public class SshKeyPairGenerator {

	public static final String SSH2_RSA = "ssh-rsa";
	public static final String ECDSA = "ecdsa";
	public static final String ED25519 = "ed25519";
	public static final String ED448 = "ed448";

	/**
	 * Generate a new key pair using the default bit size.
	 * 
	 * @param algorithm
	 * @return
	 * @throws IOException
	 * @throws SshException
	 */
	public static SshKeyPair generateKeyPair(String algorithm) throws IOException, SshException {
		
		switch(algorithm) {
		case ECDSA:
			return generateKeyPair(algorithm, 256);
		case ED25519:
			return generateKeyPair(algorithm, 0);
		case ED448:
			return generateKeyPair(algorithm, 0);
		case SSH2_RSA:
		case "rsa":
		case "RSA":
			return generateKeyPair(algorithm, 2048)	;
		default:
			throw new IOException(String.format("Unexpected key algorithm %s", algorithm));
	}
	}
	/**
	 * Generates a new key pair.
	 * 
	 * @param algorithm
	 * @param bits
	 * @return SshKeyPair
	 * @throws IOException
	 */
	public static SshKeyPair generateKeyPair(String algorithm, int bits) throws IOException, SshException {

		
		switch(algorithm) {
		case ED25519:
		case "ssh-ed25519":
			return ComponentManager.getDefaultInstance().generateEd25519KeyPair();
		case ED448:
		case "ssh-ed448":
			return ComponentManager.getDefaultInstance().generateEd448KeyPair();
		case ECDSA:
			return ComponentManager.getDefaultInstance().generateEcdsaKeyPair(bits);
		case SSH2_RSA:
		case "rsa":
		case "RSA":
			return ComponentManager.getDefaultInstance().generateRsaKeyPair(bits, 2);
		default:
			var generators = new ComponentFactory<KeyGenerator>(JCEComponentManager.getDefaultInstance());
			for(var s : ServiceLoader.load(KeyGeneratorFactory.class, JCEComponentManager.getDefaultInstance().getClassLoader())) {
				if(ComponentManager.isDefaultEnabled(KeyGenerator.class, s.getKeys()[0]).orElse(false))
					generators.add(s);
			}
			return generators.getInstance(algorithm).generateKey(bits);
		}
	}

}

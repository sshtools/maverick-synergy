package com.sshtools.common.ssh.components;

import java.math.BigInteger;
import java.security.interfaces.DSAPublicKey;

/**
 * This interface should be implemented by all DSA public key implementations. 
 * 
 * @author Lee David Painter
 *
 */
public interface SshDsaPublicKey extends SshPublicKey {
	public BigInteger getP();
	public BigInteger getQ();
	public BigInteger getG();
	public BigInteger getY();
	public DSAPublicKey getJCEPublicKey();
}

package com.sshtools.common.ssh.components.jce;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.DSAPrivateKey;
import java.security.interfaces.DSAPublicKey;
import java.security.spec.DSAPrivateKeySpec;
import java.security.spec.InvalidKeySpecException;

import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshDsaPrivateKey;
import com.sshtools.common.ssh.components.SshDsaPublicKey;

/**
 * DSA private key implementation for the SSH2 protocol. 
 * 
 * @author Lee David Painter
 */
public class Ssh2DsaPrivateKey extends Ssh2BaseDsaPrivateKey implements SshDsaPrivateKey  {

	
	
	protected DSAPrivateKey prv;
	protected Ssh2DsaPublicKey pub;

	public Ssh2DsaPrivateKey(DSAPrivateKey prv, DSAPublicKey pub) {
		super(prv);
		this.prv = prv;
		this.pub = new Ssh2DsaPublicKey(pub);
	}
	
	public Ssh2DsaPrivateKey(DSAPrivateKey prv) throws NoSuchAlgorithmException, InvalidKeySpecException {
		super(prv);
		this.prv = prv;
		generatePublic();
	}

	public Ssh2DsaPrivateKey(BigInteger p,
            BigInteger q,
            BigInteger g,
            BigInteger x,
            BigInteger y) throws SshException {
		super(null);
		try {
			KeyFactory kf = JCEProvider.getProviderForAlgorithm(JCEAlgorithms.JCE_DSA)==null ? KeyFactory.getInstance(JCEAlgorithms.JCE_DSA) : KeyFactory.getInstance(JCEAlgorithms.JCE_DSA, JCEProvider.getProviderForAlgorithm(JCEAlgorithms.JCE_DSA));
			DSAPrivateKeySpec spec = new DSAPrivateKeySpec(x,p,q,g);
			super.prv = this.prv = (DSAPrivateKey) kf.generatePrivate(spec);

			pub = new Ssh2DsaPublicKey(p, q, g, y);
		} catch (Throwable e) {
			throw new SshException(e);
		}
	}
	
	private void generatePublic() throws NoSuchAlgorithmException, InvalidKeySpecException {
		BigInteger y = prv.getParams().getG().modPow(prv.getX(), prv.getParams().getP());
		pub = new Ssh2DsaPublicKey(prv.getParams().getP(), 
				prv.getParams().getQ(),
				prv.getParams().getG(),
				y);
	}
	
	public DSAPrivateKey getJCEPrivateKey() {
		return prv;
	}

	public SshDsaPublicKey getPublicKey() {
		return pub;
	}

	public BigInteger getX() {
		return ((DSAPrivateKey)prv).getX();
	}
}

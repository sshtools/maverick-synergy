/*
 *    _           _             _   _
 *   (_) __ _  __| | __ _ _ __ | |_(_)_   _____
 *   | |/ _` |/ _` |/ _` | '_ \| __| \ \ / / _ \
 *   | | (_| | (_| | (_| | |_) | |_| |\ V /  __/
 *  _/ |\__,_|\__,_|\__,_| .__/ \__|_| \_/ \___|
 * |__/                  |_|
 *
 * This file is part of the Maverick Synergy Hotfixes Java SSH API
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Copyright (C) 2002-2021 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
 */

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

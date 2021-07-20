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

package com.sshtools.common.ssh.components;

import java.math.BigInteger;
import java.security.PrivateKey;

import com.sshtools.common.ssh.SshException;

/**
 * This interface should be implemented by all RSA private co-efficient
 * private key implementations. 
 *  
 * @author Lee David Painter
 */
public interface SshRsaPrivateCrtKey extends SshRsaPrivateKey {

	public BigInteger getPublicExponent();

	public BigInteger getPrimeP();

	public BigInteger getPrimeQ();

	public BigInteger getPrimeExponentP();

	public BigInteger getPrimeExponentQ();

	public BigInteger getCrtCoefficient();
	
	BigInteger doPrivate(BigInteger input) throws SshException;

	public PrivateKey getJCEPrivateKey();
}
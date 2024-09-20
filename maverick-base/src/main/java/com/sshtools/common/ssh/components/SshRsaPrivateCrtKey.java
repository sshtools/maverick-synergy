package com.sshtools.common.ssh.components;

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

package com.sshtools.common.ssh.components.jce;

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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.sshtools.common.ssh.components.Digest;
import com.sshtools.common.util.ByteArrayWriter;

/**
 * An abstract class that implements the {@link com.sshtools.common.ssh.components.Digest}
 * interface to provide support for JCE based digests.
 * @author Lee David Painter
 *
 */
public class AbstractDigest implements Digest {

	MessageDigest digest;
	String jceAlgorithm;
	
	public AbstractDigest(String jceAlgorithm) throws NoSuchAlgorithmException {
		digest = JCEProvider.getProviderForAlgorithm(jceAlgorithm)==null ? 
				MessageDigest.getInstance(jceAlgorithm) : 
					MessageDigest.getInstance(jceAlgorithm, JCEProvider.getProviderForAlgorithm(jceAlgorithm));
	}
	
	public byte[] doFinal() {
		return digest.digest();
	}

	public void putBigInteger(BigInteger bi) {
		
	    byte[] data = bi.toByteArray();
	    putInt(data.length);
	    putBytes(data);
	}

	public void putByte(byte b) {
		digest.update(b);
	}

	public void putBytes(byte[] data) {
		digest.update(data, 0, data.length);
	}

	public void putBytes(byte[] data, int offset, int len) {
		digest.update(data, offset, len);
	}

	public void putInt(int i) {
		putBytes(ByteArrayWriter.encodeInt(i));
	}

	public void putString(String str) {
	    putInt(str.length());
	    putBytes(str.getBytes());
	}

	public void reset() {
		digest.reset();
	}
	
	public String getProvider() {
		if(digest==null){ 
			return null;
		}
		return digest.getProvider().getName();
	}

}

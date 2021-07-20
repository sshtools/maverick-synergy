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


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

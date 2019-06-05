package com.sshtools.common.ssh.components.jce;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.sshtools.common.ssh.SshException;


/**
 * SHA-1 message authentication implementation.
 * @author Lee David Painter
 *
 */
public class HmacSha256 extends AbstractHmac {

	public HmacSha256() {
		super(JCEAlgorithms.JCE_HMACSHA256, 32);
	}
	
	protected HmacSha256(int size) {
		super(JCEAlgorithms.JCE_HMACSHA256, 32, size);
	}
	
	public String getAlgorithm() {
		return "hmac-sha2-256";
	}
	

	public void init(byte[] keydata) throws SshException {
        try {
            mac = JCEProvider.getProviderForAlgorithm(jceAlgorithm)==null ? Mac.getInstance(jceAlgorithm) : Mac.getInstance(jceAlgorithm, JCEProvider.getProviderForAlgorithm(jceAlgorithm));

            // Create a key of 16 bytes
            byte[] key = new byte[System.getProperty("miscomputes.ssh2.hmac.keys", "false").equalsIgnoreCase("true") ? 16 : 32];
            System.arraycopy(keydata, 0, key, 0, key.length);

            SecretKeySpec keyspec = new SecretKeySpec(key, jceAlgorithm);
            mac.init(keyspec);
        } catch (Throwable t) {
            throw new SshException(t);
        }
	}


}

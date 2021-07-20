
package com.sshtools.common.ssh.components.jce;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.sshtools.common.ssh.SecurityLevel;
import com.sshtools.common.ssh.SshException;


/**
 * SHA-1 message authentication implementation.
 * @author Lee David Painter
 *
 */
public class HmacSha256_at_ssh_dot_com extends AbstractHmac {

	public HmacSha256_at_ssh_dot_com() {
		super(JCEAlgorithms.JCE_HMACSHA256, 32, SecurityLevel.STRONG, 2000);
	}
	
	protected HmacSha256_at_ssh_dot_com(int size) {
		super(JCEAlgorithms.JCE_HMACSHA256, 32, size, SecurityLevel.STRONG, 2000);
	}
	
	public String getAlgorithm() {
		return "hmac-sha256@ssh.com";
	}
	

	public void init(byte[] keydata) throws SshException {
        try {
            mac = JCEProvider.getProviderForAlgorithm(jceAlgorithm)==null ? Mac.getInstance(jceAlgorithm) : Mac.getInstance(jceAlgorithm, JCEProvider.getProviderForAlgorithm(jceAlgorithm));

            // Create a key of 16 bytes
            byte[] key = new byte[16];
            System.arraycopy(keydata, 0, key, 0, key.length);

            SecretKeySpec keyspec = new SecretKeySpec(key, jceAlgorithm);
            mac.init(keyspec);
        } catch (Throwable t) {
            throw new SshException(t);
        }
	}


}

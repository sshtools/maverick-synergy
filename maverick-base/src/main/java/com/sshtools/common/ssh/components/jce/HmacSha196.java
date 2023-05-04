package com.sshtools.common.ssh.components.jce;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.sshtools.common.ssh.SecurityLevel;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshHmacFactory;


/**
 * SHA-1 message authentication implementation.
 * @author Lee David Painter
 *
 */
public class HmacSha196 extends AbstractHmac {
	
	public static class HmacSha196Factory implements SshHmacFactory<HmacSha196> {
		@Override
		public HmacSha196 create() throws NoSuchAlgorithmException, IOException {
			return new HmacSha196();
		}

		@Override
		public String[] getKeys() {
			return new String[] { "hmac-sha1-96" };
		}
	}

	public HmacSha196() {
		super(JCEAlgorithms.JCE_HMACSHA1, 20, 12, SecurityLevel.WEAK, 5);
	}

	
	public String getAlgorithm() {
		return "hmac-sha1-96";
	}
	

	public void init(byte[] keydata) throws SshException {
        try {
            mac = JCEProvider.getProviderForAlgorithm(jceAlgorithm)==null ? Mac.getInstance(jceAlgorithm) : Mac.getInstance(jceAlgorithm, JCEProvider.getProviderForAlgorithm(jceAlgorithm));

            // Create a key of 16 bytes
            byte[] key = new byte[System.getProperty("miscomputes.ssh2.hmac.keys", "false").equalsIgnoreCase("true") ? 16 : 20];
            System.arraycopy(keydata, 0, key, 0, key.length);

            SecretKeySpec keyspec = new SecretKeySpec(key, jceAlgorithm);
            mac.init(keyspec);
        } catch (Throwable t) {
            throw new SshException(t);
        }
	}


}

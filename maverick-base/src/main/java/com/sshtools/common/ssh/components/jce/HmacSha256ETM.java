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
public class HmacSha256ETM extends AbstractHmac {
	
	public static class HmacSha256ETMFactory implements SshHmacFactory<HmacSha256ETM> {
		@Override
		public HmacSha256ETM create() throws NoSuchAlgorithmException, IOException {
			return new HmacSha256ETM();
		}

		@Override
		public String[] getKeys() {
			return new String[] { "hmac-sha2-256-etm@openssh.com" };
		}
	}

	public HmacSha256ETM() {
		super(JCEAlgorithms.JCE_HMACSHA256, 32, SecurityLevel.STRONG, 2001);
	}
	
	protected HmacSha256ETM(int size) {
		super(JCEAlgorithms.JCE_HMACSHA256, 32, size, SecurityLevel.STRONG, 2001);
	}
	
	public String getAlgorithm() {
		return "hmac-sha2-256-etm@openssh.com";
	}
	
	public boolean isETM() {
		return true;
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

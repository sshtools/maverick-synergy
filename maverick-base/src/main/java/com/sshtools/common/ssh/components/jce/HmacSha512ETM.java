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
public class HmacSha512ETM extends AbstractHmac {
	public static class HmacSha512ETMFactory implements SshHmacFactory<HmacSha512ETM> {
		@Override
		public HmacSha512ETM create() throws NoSuchAlgorithmException, IOException {
			return new HmacSha512ETM();
		}

		@Override
		public String[] getKeys() {
			return new String[] { "hmac-sha2-512-etm@openssh.com" };
		}
	}
	
	public HmacSha512ETM() {
		super(JCEAlgorithms.JCE_HMACSHA512, 64, SecurityLevel.PARANOID, 3001);
	}
	
	protected HmacSha512ETM(int size) {
		super(JCEAlgorithms.JCE_HMACSHA512, 64, size, SecurityLevel.PARANOID, 3001);
	}

	
	public String getAlgorithm() {
		return "hmac-sha2-512-etm@openssh.com";
	}
	
	public boolean isETM() {
		return true;
	}

	public void init(byte[] keydata) throws SshException {
        try {
            mac = JCEProvider.getProviderForAlgorithm(jceAlgorithm)==null ? Mac.getInstance(jceAlgorithm) : Mac.getInstance(jceAlgorithm, JCEProvider.getProviderForAlgorithm(jceAlgorithm));

            // Create a key of 16 bytes
            byte[] key = new byte[64];
            System.arraycopy(keydata, 0, key, 0, key.length);

            SecretKeySpec keyspec = new SecretKeySpec(key, jceAlgorithm);
            mac.init(keyspec);
        } catch (Throwable t) {
            throw new SshException(t);
        }
	}


}

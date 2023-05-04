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
public class HmacSha512 extends AbstractHmac {
	
	public static class HmacSha512Factory implements SshHmacFactory<HmacSha512> {
		@Override
		public HmacSha512 create() throws NoSuchAlgorithmException, IOException {
			return new HmacSha512();
		}

		@Override
		public String[] getKeys() {
			return new String[] { "hmac-sha2-512" };
		}
	}

	public HmacSha512() {
		super(JCEAlgorithms.JCE_HMACSHA512, 64, SecurityLevel.PARANOID, 3000);
	}
	
	protected HmacSha512(int size) {
		super(JCEAlgorithms.JCE_HMACSHA512, 64, size, SecurityLevel.PARANOID, 3000);
	}

	
	public String getAlgorithm() {
		return "hmac-sha2-512";
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

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
public class HmacRipeMd160ETM extends AbstractHmac {
	
	private static final String ALGORITHM = "hmac-ripemd160-etm@openssh.com";

	public static class HmacRipeMd160ETMFactory implements SshHmacFactory<HmacRipeMd160ETM> {
		@Override
		public HmacRipeMd160ETM create() throws NoSuchAlgorithmException, IOException {
			return new HmacRipeMd160ETM();
		}

		@Override
		public String[] getKeys() {
			return new String[] { ALGORITHM };
		}
	}

	public HmacRipeMd160ETM() {
		super(JCEAlgorithms.JCE_HMACRIPEMD160, 20, SecurityLevel.WEAK, 2);
	}
	
	protected HmacRipeMd160ETM(int size) {
		super(JCEAlgorithms.JCE_HMACRIPEMD160, 20, size, SecurityLevel.WEAK, 2);
	}

	public String getAlgorithm() {
		return ALGORITHM;
	}
	
	public boolean isETM() {
		return true;
	}

	public void init(byte[] keydata) throws SshException {
        try {
            mac = JCEProvider.getProviderForAlgorithm(jceAlgorithm)==null ? Mac.getInstance(jceAlgorithm) : Mac.getInstance(jceAlgorithm, JCEProvider.getProviderForAlgorithm(jceAlgorithm));

            // Create a key of 16 bytes
            byte[] key = new byte[20];
            System.arraycopy(keydata, 0, key, 0, key.length);

            SecretKeySpec keyspec = new SecretKeySpec(key, jceAlgorithm);
            mac.init(keyspec);
        } catch (Throwable t) {
            throw new SshException(t);
        }
	}


}

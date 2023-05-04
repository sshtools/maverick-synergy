package com.sshtools.common.ssh.components.jce;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import com.sshtools.common.ssh.SecurityLevel;
import com.sshtools.common.ssh.components.SshHmacFactory;

/**
 * MD5 message authentication implementation.
 * @author Lee David Painter
 *
 */
public class HmacMD596 extends AbstractHmac {
	
	private static final String ALGORITHM = "hmac-md5-96";

	public static class HmacMD596Factory implements SshHmacFactory<HmacMD596> {
		@Override
		public HmacMD596 create() throws NoSuchAlgorithmException, IOException {
			return new HmacMD596();
		}

		@Override
		public String[] getKeys() {
			return new String[] { ALGORITHM };
		}
	}

	public HmacMD596() {
		super(JCEAlgorithms.JCE_HMACMD5, 16, 12, SecurityLevel.WEAK, 0);
	}
	
	public String getAlgorithm() {
		return ALGORITHM;
	}

}

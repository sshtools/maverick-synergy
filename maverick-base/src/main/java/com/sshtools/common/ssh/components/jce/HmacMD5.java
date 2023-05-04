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
public class HmacMD5 extends AbstractHmac {
	private static final String ALGORITHM = "hmac-md5";
	
	public static class HmacMD5Factory implements SshHmacFactory<HmacMD5> {

		@Override
		public HmacMD5 create() throws NoSuchAlgorithmException, IOException {
			return new HmacMD5();
		}

		@Override
		public String[] getKeys() {
			return new String[] { ALGORITHM };
		}
	}
	
	public HmacMD5() {
		super(JCEAlgorithms.JCE_HMACMD5, 16, SecurityLevel.WEAK, 0);
	}

	
	public String getAlgorithm() {
		return ALGORITHM;
	}

}

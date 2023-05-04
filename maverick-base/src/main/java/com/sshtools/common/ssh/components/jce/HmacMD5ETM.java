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
public class HmacMD5ETM extends AbstractHmac {
	
	private static final String ALGORITHM = "hmac-md5-etm@openssh.com";

	public static class HmacMD5ETMFactory implements SshHmacFactory<HmacMD5ETM> {
		@Override
		public HmacMD5ETM create() throws NoSuchAlgorithmException, IOException {
			return new HmacMD5ETM();
		}

		@Override
		public String[] getKeys() {
			return new String[] { ALGORITHM };
		}
	}

	public HmacMD5ETM() {
		super(JCEAlgorithms.JCE_HMACMD5, 16, SecurityLevel.WEAK, 1);
	}

	
	public String getAlgorithm() {
		return ALGORITHM;
	}
	
	@Override
	public boolean isETM() {
		return true;
	}

}

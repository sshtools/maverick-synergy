
package com.sshtools.common.ssh.components.jce;

import com.sshtools.common.ssh.SecurityLevel;

/**
 * MD5 message authentication implementation.
 * @author Lee David Painter
 *
 */
public class HmacMD5ETM extends AbstractHmac {

	public HmacMD5ETM() {
		super(JCEAlgorithms.JCE_HMACMD5, 16, SecurityLevel.WEAK, 1);
	}

	
	public String getAlgorithm() {
		return "hmac-md5-etm@openssh.com";
	}
	
	@Override
	public boolean isETM() {
		return true;
	}

}

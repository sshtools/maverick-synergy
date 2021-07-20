
package com.sshtools.common.ssh.components.jce;

import com.sshtools.common.ssh.SecurityLevel;

/**
 * MD5 message authentication implementation.
 * @author Lee David Painter
 *
 */
public class HmacMD5 extends AbstractHmac {

	public HmacMD5() {
		super(JCEAlgorithms.JCE_HMACMD5, 16, SecurityLevel.WEAK, 0);
	}

	
	public String getAlgorithm() {
		return "hmac-md5";
	}

}

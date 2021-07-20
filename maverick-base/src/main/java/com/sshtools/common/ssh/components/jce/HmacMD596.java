
package com.sshtools.common.ssh.components.jce;

import com.sshtools.common.ssh.SecurityLevel;

/**
 * MD5 message authentication implementation.
 * @author Lee David Painter
 *
 */
public class HmacMD596 extends AbstractHmac {

	public HmacMD596() {
		super(JCEAlgorithms.JCE_HMACMD5, 16, 12, SecurityLevel.WEAK, 0);
	}
	
	public String getAlgorithm() {
		return "hmac-md5-96";
	}

}

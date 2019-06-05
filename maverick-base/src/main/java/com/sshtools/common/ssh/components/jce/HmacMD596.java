package com.sshtools.common.ssh.components.jce;

/**
 * MD5 message authentication implementation.
 * @author Lee David Painter
 *
 */
public class HmacMD596 extends AbstractHmac {

	public HmacMD596() {
		super(JCEAlgorithms.JCE_HMACMD5, 16, 12);
	}
	
	public String getAlgorithm() {
		return "hmac-md5-96";
	}

}

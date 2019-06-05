package com.sshtools.common.ssh.components.jce;

/**
 * MD5 message authentication implementation.
 * @author Lee David Painter
 *
 */
public class HmacMD5 extends AbstractHmac {

	public HmacMD5() {
		super(JCEAlgorithms.JCE_HMACMD5, 16);
	}

	
	public String getAlgorithm() {
		return "hmac-md5";
	}

}

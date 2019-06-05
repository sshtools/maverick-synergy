package com.sshtools.common.ssh.components.jce;



/**
 * SHA-1 message authentication implementation.
 * @author Lee David Painter
 *
 */
public class HmacSha256_96 extends HmacSha256 {

	public HmacSha256_96() {
		super(12);
	}
	
	public String getAlgorithm() {
		return "hmac-sha2-256-96";
	}
}

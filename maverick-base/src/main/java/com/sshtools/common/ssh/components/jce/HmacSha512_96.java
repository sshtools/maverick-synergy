package com.sshtools.common.ssh.components.jce;



/**
 * SHA-1 message authentication implementation.
 * @author Lee David Painter
 *
 */
public class HmacSha512_96 extends HmacSha512 {

	public HmacSha512_96() {
		super(12);
	}
	
	public String getAlgorithm() {
		return "hmac-sha2-512-96";
	}
}

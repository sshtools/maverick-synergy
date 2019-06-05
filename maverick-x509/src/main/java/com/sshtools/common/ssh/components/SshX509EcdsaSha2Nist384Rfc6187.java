package com.sshtools.common.ssh.components;

import java.io.IOException;
import java.security.cert.Certificate;
import java.security.interfaces.ECPublicKey;

import com.sshtools.common.ssh.components.jce.JCEAlgorithms;

public class SshX509EcdsaSha2Nist384Rfc6187 extends
		SshX509EcdsaSha2NistPublicKeyRfc6187 {

	public SshX509EcdsaSha2Nist384Rfc6187(ECPublicKey pk) throws IOException {
		super(pk, "secp384r1");
	}

	public SshX509EcdsaSha2Nist384Rfc6187() {
		super("ecdsa-sha2-nistp384", JCEAlgorithms.JCE_SHA384WithECDSA, "secp384r1", "nistp384");
	}

	public SshX509EcdsaSha2Nist384Rfc6187(Certificate[] chain)
			throws IOException {
		super(chain, "secp384r1");
	}

	@Override
	public String getAlgorithm() {
		return "x509v3-ecdsa-sha2-nistp384";
	}

}

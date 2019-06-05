package com.sshtools.common.ssh.components;

import com.sshtools.common.ssh.components.jce.JCEAlgorithms;
import com.sshtools.common.ssh.components.jce.Ssh2EcdsaSha2NistPublicKey;

public class OpenSshEcdsaSha2Nist256Certificate extends Ssh2EcdsaSha2NistPublicKey {

	public OpenSshEcdsaSha2Nist256Certificate() {
		super("ecdsa-sha2-nistp256-cert-v01@openssh.com", JCEAlgorithms.JCE_SHA256WithECDSA, "secp256r1", "nistp256");
	}
}

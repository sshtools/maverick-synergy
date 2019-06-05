package com.sshtools.common.ssh.components;

import com.sshtools.common.ssh.components.jce.JCEAlgorithms;
import com.sshtools.common.ssh.components.jce.Ssh2EcdsaSha2NistPublicKey;

public class OpenSshEcdsaSha2Nist521Certificate extends Ssh2EcdsaSha2NistPublicKey {

	public OpenSshEcdsaSha2Nist521Certificate() {
		super("ecdsa-sha2-nistp521-cert-v01@openssh.com", JCEAlgorithms.JCE_SHA512WithECDSA, "secp521r1", "nistp521");
	}
}

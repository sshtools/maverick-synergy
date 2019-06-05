package com.sshtools.common.ssh.components;

import com.sshtools.common.ssh.components.jce.JCEAlgorithms;
import com.sshtools.common.ssh.components.jce.Ssh2EcdsaSha2NistPublicKey;

public class OpenSshEcdsaSha2Nist384Certificate extends Ssh2EcdsaSha2NistPublicKey {

	public OpenSshEcdsaSha2Nist384Certificate() {
		super("ecdsa-sha2-nistp384-cert-v01@openssh.com", JCEAlgorithms.JCE_SHA384WithECDSA, "secp384r1", "nistp384");
	}
}

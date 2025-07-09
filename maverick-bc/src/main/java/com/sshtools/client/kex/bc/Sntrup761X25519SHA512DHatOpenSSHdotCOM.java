package com.sshtools.client.kex.bc;

import com.sshtools.synergy.ssh.SshContext;

public class Sntrup761X25519SHA512DHatOpenSSHdotCOM extends Sntrup761X25519SHA512DH {

	public Sntrup761X25519SHA512DHatOpenSSHdotCOM() {
		super(SshContext.KEX_SNTRUP761_25519_SHA512_OPENSSH);
	}
}

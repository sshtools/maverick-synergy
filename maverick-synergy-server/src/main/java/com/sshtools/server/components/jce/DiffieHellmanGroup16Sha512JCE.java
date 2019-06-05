package com.sshtools.server.components.jce;

import com.sshtools.common.ssh.components.DiffieHellmanGroups;
import com.sshtools.common.ssh.components.jce.JCEAlgorithms;

public class DiffieHellmanGroup16Sha512JCE extends DiffieHellmanGroup {

	public static final String DIFFIE_HELLMAN_GROUP16_SHA512 = "diffie-hellman-group16-sha512";
	
	public DiffieHellmanGroup16Sha512JCE() {
		super(DIFFIE_HELLMAN_GROUP16_SHA512, JCEAlgorithms.JCE_SHA512, DiffieHellmanGroups.group16);
	}

}

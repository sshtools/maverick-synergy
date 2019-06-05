package com.sshtools.server.components.jce;

import com.sshtools.common.ssh.components.DiffieHellmanGroups;
import com.sshtools.common.ssh.components.jce.JCEAlgorithms;

public class DiffieHellmanGroup18Sha512JCE extends DiffieHellmanGroup {

	public static final String DIFFIE_HELLMAN_GROUP18_SHA512 = "diffie-hellman-group18-sha512";
	
	public DiffieHellmanGroup18Sha512JCE() {
		super(DIFFIE_HELLMAN_GROUP18_SHA512, JCEAlgorithms.JCE_SHA512, DiffieHellmanGroups.group18);
	}

}

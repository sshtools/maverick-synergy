
package com.sshtools.server.components.jce;

import com.sshtools.common.ssh.SecurityLevel;
import com.sshtools.common.ssh.components.DiffieHellmanGroups;
import com.sshtools.common.ssh.components.jce.JCEAlgorithms;

public class DiffieHellmanGroup17Sha512JCE extends DiffieHellmanGroup {

	public static final String DIFFIE_HELLMAN_GROUP17_SHA512 = "diffie-hellman-group17-sha512";
	
	public DiffieHellmanGroup17Sha512JCE() {
		super(DIFFIE_HELLMAN_GROUP17_SHA512, JCEAlgorithms.JCE_SHA512, DiffieHellmanGroups.group17, SecurityLevel.PARANOID, 3017);
	}

}

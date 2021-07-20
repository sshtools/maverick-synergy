

package com.sshtools.client.components;

import com.sshtools.common.ssh.SecurityLevel;
import com.sshtools.common.ssh.components.DiffieHellmanGroups;
import com.sshtools.common.ssh.components.jce.JCEAlgorithms;

/**
 * Implementation of the required SSH Transport Protocol key exchange method
 * "diffie-hellman-group14-sha1".
 */
public class DiffieHellmanGroup15Sha512JCE extends DiffieHellmanGroup {

  /**
   * Constant for the algorithm name "diffie-hellman-group15-sha512".
   */
  public static final String DIFFIE_HELLMAN_GROUP15_SHA512 = "diffie-hellman-group15-sha512";

  public DiffieHellmanGroup15Sha512JCE() {
	  super(DIFFIE_HELLMAN_GROUP15_SHA512, JCEAlgorithms.JCE_SHA512,  DiffieHellmanGroups.group15, SecurityLevel.PARANOID, 3015);
  }

}

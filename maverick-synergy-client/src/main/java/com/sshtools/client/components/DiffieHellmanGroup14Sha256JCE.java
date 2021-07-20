

package com.sshtools.client.components;

import com.sshtools.common.ssh.SecurityLevel;
import com.sshtools.common.ssh.components.DiffieHellmanGroups;
import com.sshtools.common.ssh.components.jce.JCEAlgorithms;

/**
 * Implementation of the required SSH Transport Protocol key exchange method
 * "diffie-hellman-group14-sha1".
 */
public class DiffieHellmanGroup14Sha256JCE extends DiffieHellmanGroup {

  /**
   * Constant for the algorithm name "diffie-hellman-group14-sha256".
   */
  public static final String DIFFIE_HELLMAN_GROUP14_SHA256 = "diffie-hellman-group14-sha256";

  public DiffieHellmanGroup14Sha256JCE() {
	  super(DIFFIE_HELLMAN_GROUP14_SHA256, JCEAlgorithms.JCE_SHA256, DiffieHellmanGroups.group14, SecurityLevel.STRONG, 2001);
  }

}

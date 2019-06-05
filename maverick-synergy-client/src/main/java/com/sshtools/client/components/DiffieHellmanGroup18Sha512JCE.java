
package com.sshtools.client.components;

import com.sshtools.common.ssh.components.DiffieHellmanGroups;
import com.sshtools.common.ssh.components.jce.JCEAlgorithms;

/**
 * Implementation of the required SSH Transport Protocol key exchange method
 * "diffie-hellman-group14-sha1".
 */
public class DiffieHellmanGroup18Sha512JCE extends DiffieHellmanGroup {

  /**
   * Constant for the algorithm name "diffie-hellman-group16-sha512".
   */
  public static final String DIFFIE_HELLMAN_GROUP18_SHA512 = "diffie-hellman-group18-sha512";

  public DiffieHellmanGroup18Sha512JCE() {
	  super(DIFFIE_HELLMAN_GROUP18_SHA512, JCEAlgorithms.JCE_SHA512,  DiffieHellmanGroups.group18);
  }

}

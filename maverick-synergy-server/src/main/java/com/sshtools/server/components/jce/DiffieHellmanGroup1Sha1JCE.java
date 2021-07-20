

package com.sshtools.server.components.jce;

import com.sshtools.common.ssh.SecurityLevel;
import com.sshtools.common.ssh.components.DiffieHellmanGroups;
import com.sshtools.common.ssh.components.jce.JCEAlgorithms;

/**
 *
 * <p>Implementation of the required SSH Transport Protocol key exchange method
 * "diffie-hellman-group1-sha1".</p>
 * 
 */
public class DiffieHellmanGroup1Sha1JCE extends DiffieHellmanGroup {

  /**
   * Constant for the algorithm name "diffie-hellman-group1-sha1".
   */
  public static final String DIFFIE_HELLMAN_GROUP1_SHA1 = "diffie-hellman-group1-sha1";
  
  /**
   * Construct an uninitialized instance.
   */
  public DiffieHellmanGroup1Sha1JCE() {
	  super(DIFFIE_HELLMAN_GROUP1_SHA1, JCEAlgorithms.JCE_SHA1, DiffieHellmanGroups.group1, SecurityLevel.WEAK, 1000);
  }
}

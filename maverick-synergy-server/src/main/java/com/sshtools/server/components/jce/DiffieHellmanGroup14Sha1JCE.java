
package com.sshtools.server.components.jce;

import com.sshtools.common.ssh.SecurityLevel;
import com.sshtools.common.ssh.components.DiffieHellmanGroups;
import com.sshtools.common.ssh.components.jce.JCEAlgorithms;

/**
 *
 * <p>Implementation of the required SSH Transport Protocol key exchange method
 * "diffie-hellman-group14-sha1".</p>
 * 
 */
public class DiffieHellmanGroup14Sha1JCE
    extends DiffieHellmanGroup  {

  public static final String DIFFIE_HELLMAN_GROUP14_SHA1 = "diffie-hellman-group14-sha1";

  /**
   * Construct an uninitialized instance.
   */
  public DiffieHellmanGroup14Sha1JCE() {
	  super(DIFFIE_HELLMAN_GROUP14_SHA1, JCEAlgorithms.JCE_SHA1, DiffieHellmanGroups.group14, SecurityLevel.WEAK, 1001);
  }
}

/**
 * (c) 2002-2019 JADAPTIVE Limited. All Rights Reserved.
 *
 * This file is part of the Maverick Synergy Java SSH API.
 *
 * Maverick Synergy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Maverick Synergy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.sshtools.server.components.jce;

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
	  super(DIFFIE_HELLMAN_GROUP1_SHA1, JCEAlgorithms.JCE_SHA1, DiffieHellmanGroups.group1);
  }
}

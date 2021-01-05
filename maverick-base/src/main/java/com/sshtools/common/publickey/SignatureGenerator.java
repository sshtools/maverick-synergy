/**
 * (c) 2002-2021 JADAPTIVE Limited. All Rights Reserved.
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
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.common.publickey;

import java.io.IOException;
import java.util.Collection;

import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshPublicKey;

/**
 * Provides a callback when a private key signature is required. This
 * is suitable for use when you do not
 * have direct access to the private key, but know its public key and
 * have access to some mechanism that enables you to request a signature
 * from the corresponding private key (such as an sshagent).
 */
public interface SignatureGenerator {

  /**
   * Sign the data using the private key of the public key provided.
   * @param key
   * @param data
   * @return byte[]
   * @throws IOException
   */
  public byte[] sign(SshPublicKey key, String signingAlgorithm, byte[] data) throws SshException;
  
  /**
   * List the public keys supported by this signature generator.
   * @return
   * @throws IOException
   */
  public Collection<SshPublicKey> getPublicKeys() throws IOException;
  
}

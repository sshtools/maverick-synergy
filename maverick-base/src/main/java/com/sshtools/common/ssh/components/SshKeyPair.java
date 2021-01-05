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
/* HEADER */
package com.sshtools.common.ssh.components;

/**
 * Storage class for a public/private key pair.
 * @author Lee David Painter
 */
public class SshKeyPair {
  SshPrivateKey privatekey;
  SshPublicKey publickey;

  /**
   * Get the private key.
   * @return SshPrivateKey
   */
  public SshPrivateKey getPrivateKey() {
    return privatekey;
  }

  /**
   * Get the public key.
   * @return SshPublicKey
   */
  public SshPublicKey getPublicKey() {
    return publickey;
  }

  /**
   * Wraps a public/private key pair into an SshKeyPair instance.
   *
   * @param prv
   * @param pub
   * @return SshKeyPair
   */
  public static SshKeyPair getKeyPair(SshPrivateKey prv, SshPublicKey pub) {
    SshKeyPair pair = new SshKeyPair();
    pair.publickey = pub;
    pair.privatekey = prv;
    return pair;
  }

  /**
   * Set the private key
   * 
   * @param privatekey
   */
  public void setPrivateKey(SshPrivateKey privatekey) {
      this.privatekey = privatekey;
    
  }

  /**
   * Set the public key
   * 
   * @param publickey
   */
  public void setPublicKey(SshPublicKey publickey) {
      this.publickey = publickey;
    
  }

	@Override
	public int hashCode() {
		return privatekey.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof SshKeyPair) {
			SshKeyPair other = (SshKeyPair)obj;
			if(other.privatekey!=null && other.publickey!=null) {
				if(privatekey!=null && publickey !=null) {
					return other.privatekey.equals(privatekey) && other.publickey.equals(publickey);
				}
			}
		}
		return false;
	}

  
}

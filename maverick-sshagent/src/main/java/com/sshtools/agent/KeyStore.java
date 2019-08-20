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
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.agent;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.sshtools.agent.exceptions.KeyTimeoutException;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.SshPrivateKey;
import com.sshtools.common.ssh.components.SshPublicKey;

public interface KeyStore {

	/**
	 * Return a Map of all the installed keys.
	 *
	 * @return
	 */
	Map<SshPublicKey, String> getPublicKeys();

	/**
	 * Get the constraints for a key stored in this keystore.
	 *
	 * @param key The public key.
	 *
	 * @return 
	 */
	KeyConstraints getKeyConstraints(SshPublicKey key);

	/**
	 * How many keys are in this store?
	 *
	 * @return The number of keys in this store.
	 */
	int size();

	/**
	 * Add a key to this keystore
	 *
	 * @param prvkey The private key
	 * @param pubkey The public key
	 * @param description A description for this key pair.
	 * @param cs Any constraints.
	 *
	 * @return <code>true</code> if the key was added to the keystore.
	 *
	 * @throws IOException
	 */
	boolean addKey(SshPrivateKey prvkey, SshPublicKey pubkey, String description, KeyConstraints cs) throws IOException;

	boolean addKey(SshKeyPair pair, String description, KeyConstraints cs) throws IOException;

	/**
	 * Delete all the keys in this keystore.
	 */
	boolean deleteAllKeys();

	/**
	 * Hash and sign some data using a key stored in this keystore.
	 *
	 * @param pubkey The public key for which the signing should be untaken.
	 * @param forwardingNodes A list of forwarding notices for this operation.
	 * @param data The data to sign.
	 *
	 * @return
	 *
	 * @throws KeyTimeoutException
	 * @throws InvalidSshKeyException
	 * @throws InvalidSshKeySignatureException
	 */
	byte[] performHashAndSign(SshPublicKey pubkey, List<ForwardingNotice> forwardingNodes, byte[] data, int flags)
			throws KeyTimeoutException, SshException;

	/**
	 * Delete a key from the keystore.
	 *
	 * @param pubkey The public key to delete.
	 *
	 * @return <code>true</code> if the key was deleted.
	 *
	 * @throws IOException
	 */
	boolean deleteKey(SshPublicKey pubkey) throws IOException;

	/**
	 * Lock the keystore.
	 *
	 * @param password A password to secure the store. Only the same password will unlock the store.
	 *
	 * @return <code>true</code> if the store was locked.
	 *
	 * @throws IOException
	 */
	boolean lock(String password) throws IOException;

	/**
	 * Unlock the keystore.
	 *
	 * @param password The password that was provided when locking the store.
	 *
	 * @return <code>true</true> if the store was unlocked.
	 *
	 * @throws IOException
	 */
	boolean unlock(String password) throws IOException;

	/**
	 * Determine if the store is currently locked.
	 * @return
	 */
	boolean isLocked();


}
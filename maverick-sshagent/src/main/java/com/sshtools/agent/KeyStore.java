/*
 *    _           _             _   _
 *   (_) __ _  __| | __ _ _ __ | |_(_)_   _____
 *   | |/ _` |/ _` |/ _` | '_ \| __| \ \ / / _ \
 *   | | (_| | (_| | (_| | |_) | |_| |\ V /  __/
 *  _/ |\__,_|\__,_|\__,_| .__/ \__|_| \_/ \___|
 * |__/                  |_|
 *
 * This file is part of the Maverick Synergy Hotfixes Java SSH API
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Copyright (C) 2002-2021 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
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
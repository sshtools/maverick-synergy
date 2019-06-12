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
package com.sshtools.agent;

/**
 * An interface for listening to events in the agent keystore.
 * @author lee
 *
 */
public interface KeyStoreListener {
	
    /**
     * A key has been deleted.
     *
     * @param keystore
     */
    public void onDeleteKey(KeyStore keystore);

    /**
     * The keystore has been locked.
     *
     * @param keystore
     */
    public void onLock(KeyStore keystore);

    /**
     * The keystore has been unlocked.
     *
     * @param keystore
     */
    public void onUnlock(KeyStore keystore);

    /**
     * A key has been added to the keystore.
     *
     * @param keystore
     */
    public void onAddKey(KeyStore keystore);

    /**
     * All the keys were deleted from the keystore.
     *
     * @param keystore
     */
    public void onDeleteAllKeys(KeyStore keystore);

    /**
     * An operation was performed on the keystore.
     *
     * @param keystore
     * @param operation
     */
    public void onKeyOperation(KeyStore keystore, String operation);
}

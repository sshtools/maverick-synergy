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

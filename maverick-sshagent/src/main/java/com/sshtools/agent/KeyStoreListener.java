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

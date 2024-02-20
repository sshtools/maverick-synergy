package com.sshtools.agent;

/*-
 * #%L
 * Key Agent
 * %%
 * Copyright (C) 2002 - 2024 JADAPTIVE Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.sshtools.agent.exceptions.KeyTimeoutException;
import com.sshtools.agent.openssh.OpenSSHAgentMessages;
import com.sshtools.common.publickey.SshKeyUtils;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.SshPrivateKey;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.ssh.components.SshRsaPublicKey;


/**
 * A store for maintaining public keys in agent 
 */
public class InMemoryKeyStore implements KeyStore {
 
    HashMap<String,String> descriptions = new HashMap<String,String>();
    HashMap<String,SshPublicKey> publickeys = new HashMap<String,SshPublicKey>(); 
    HashMap<String,SshPrivateKey> privatekeys = new HashMap<String,SshPrivateKey>(); 
    HashMap<String,KeyConstraints> constraints = new HashMap<String,KeyConstraints>();
    Vector<SshPublicKey> index = new Vector<SshPublicKey>();
    Vector<KeyStoreListener> listeners = new Vector<KeyStoreListener>();
    String lockedPassword = null;

    /**
     * Creates a new KeyStore object.
     */
    public InMemoryKeyStore() {
    }

    /* (non-Javadoc)
	 * @see com.maverick.agent.IKeyStore#getPublicKeys()
	 */
    @Override
	public Map<SshPublicKey, String> getPublicKeys() {
    	Map<SshPublicKey, String> res = new HashMap<SshPublicKey, String>();
    	for(Map.Entry<String,SshPublicKey> key : publickeys.entrySet()) {
    		res.put(key.getValue(), descriptions.get(key.getKey()));
    	}
        return Collections.unmodifiableMap(res);
    }

    /**
     * Find the index of a key.
     *
     * @param key The key to look for.
     *
     * @return
     */
    public int indexOf(SshPublicKey  key) {
        return index.indexOf(key);
    }

    /**
     * Get the public key at the specified index
     *
     * @param i The index of the key.
     *
     * @return
     */
    public SshPublicKey elementAt(int i) {
        return index.elementAt(i);
    }

    /* (non-Javadoc)
	 * @see com.maverick.agent.IKeyStore#getKeyConstraints(com.maverick.ssh.components.SshPublicKey)
	 */
    @Override
	public KeyConstraints getKeyConstraints(SshPublicKey key) {
        return (KeyConstraints) constraints.get(SshKeyUtils.getFingerprint(key));
    }

    /* (non-Javadoc)
	 * @see com.maverick.agent.IKeyStore#size()
	 */
    @Override
	public int size() {
        return index.size();
    }

    /**
     * Add a listener.
     *
     * @param listener
     */
    public void addKeyStoreListener(KeyStoreListener listener) {
        listeners.add(listener);
    }

    /**
     * Remove a listener.
     *
     * @param listener
     */
    public void removeKeyStoreListener(KeyStoreListener listener) {
        listeners.remove(listener);
    }

    /* (non-Javadoc)
	 * @see com.maverick.agent.IKeyStore#addKey(com.maverick.ssh.components.SshPrivateKey, com.maverick.ssh.components.SshPublicKey, java.lang.String, com.maverick.agent.KeyConstraints)
	 */
    @Override
	public boolean addKey(SshPrivateKey prvkey, SshPublicKey pubkey,
        String description, KeyConstraints cs) throws IOException {
    	String fingerprint = getFingerprint(pubkey);
        synchronized (publickeys) {
            if (!publickeys.containsKey(fingerprint)) {
                publickeys.put(fingerprint, pubkey);
                privatekeys.put(fingerprint, prvkey); 
                constraints.put(fingerprint, cs);
                descriptions.put(fingerprint, description);
                index.add(pubkey);

                Iterator<KeyStoreListener> it = listeners.iterator();
                KeyStoreListener listener;

                while (it.hasNext()) {
                    listener = it.next();
                    listener.onAddKey(this);
                }

                return true;
            } else {
                return false;
            }
        }
    }

    private String getFingerprint(SshPublicKey key) {
    	try {
			return key.getFingerprint();
		} catch (SshException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
    }
    /* (non-Javadoc)
	 * @see com.maverick.agent.IKeyStore#addKey(com.maverick.ssh.components.SshKeyPair, java.lang.String, com.maverick.agent.KeyConstraints)
	 */
    @Override
	public boolean addKey(SshKeyPair pair, String description, KeyConstraints cs) throws IOException {
    	return addKey(pair.getPrivateKey(), pair.getPublicKey(), description, cs);
    }
    
    /* (non-Javadoc)
	 * @see com.maverick.agent.IKeyStore#deleteAllKeys()
	 */
    @Override
	public boolean deleteAllKeys() {
        synchronized (publickeys) {
            publickeys.clear();
            privatekeys.clear();
            constraints.clear();
            index.clear();

            Iterator<KeyStoreListener> it = listeners.iterator();
            KeyStoreListener listener;

            while (it.hasNext()) {
                listener = it.next();
                listener.onDeleteAllKeys(this);
            }
        }
        
        return true;
    }

    /* (non-Javadoc)
	 * @see com.maverick.agent.IKeyStore#performHashAndSign(com.maverick.ssh.components.SshPublicKey, java.util.List, byte[])
	 */
    @Override
	public byte[] performHashAndSign(SshPublicKey pubkey, List<ForwardingNotice> forwardingNodes,
        byte[] data, int flags)
        throws KeyTimeoutException, SshException   {
    	String fingerprint = getFingerprint(pubkey);
        synchronized (publickeys) {
            if (privatekeys.containsKey(fingerprint)) {
                SshPrivateKey key = (SshPrivateKey) privatekeys.get(fingerprint);
                KeyConstraints cs = (KeyConstraints) constraints.get(fingerprint); 

                if (cs.canUse()) {
                    if (!cs.hasTimedOut()) {
                        cs.use();
                        try{
                        	String signingAlgorithm = pubkey.getSigningAlgorithm();
                        	switch(flags) {
                        	case OpenSSHAgentMessages.SSH_AGENT_RSA_SHA2_256:
                        		if(pubkey instanceof SshRsaPublicKey) {
                        			signingAlgorithm = "rsa-sha2-256";
                        		}
                        		break;
                        	case OpenSSHAgentMessages.SSH_AGENT_RSA_SHA2_512:
                        		if(pubkey instanceof SshRsaPublicKey) {
                        			signingAlgorithm = "rsa-sha2-512";
                        		}
                        		break;
                        	default:
                        	}
                        	byte[] sig = key.sign(data, signingAlgorithm);
                        	Iterator<KeyStoreListener> it = listeners.iterator();
                        	KeyStoreListener listener;

                        	while (it.hasNext()) {
                        		listener = it.next();
                        		listener.onKeyOperation(this, "hash-and-sign");
                        	}
                            return sig;
                        }catch(IOException ioe){
                        	throw new SshException(ioe.getMessage(),SshException.AGENT_ERROR);
                        }
                    } else {
                        throw new KeyTimeoutException();
                    }
                } else {
                    throw new KeyTimeoutException();
                }
            } else {
                throw new SshException("The key does not exist",SshException.AGENT_ERROR);
            }
        }
    }

    /* (non-Javadoc)
	 * @see com.maverick.agent.IKeyStore#deleteKey(com.maverick.ssh.components.SshPublicKey)
	 */
    @Override
	public boolean deleteKey(SshPublicKey pubkey)
        throws IOException {
        synchronized (publickeys) {
        		String fingerprint = SshKeyUtils.getFingerprint(pubkey);
            if (publickeys.containsKey(fingerprint)) {
                
                publickeys.remove(fingerprint);
                privatekeys.remove(fingerprint);
                constraints.remove(fingerprint);
                index.remove(pubkey);

                Iterator<KeyStoreListener> it = listeners.iterator();
                KeyStoreListener listener;

                while (it.hasNext()) {
                    listener = it.next();
                    listener.onDeleteKey(this);
                }

                return true;
            
            }

            return false;
        }
    }

    /* (non-Javadoc)
	 * @see com.maverick.agent.IKeyStore#lock(java.lang.String)
	 */
    @Override
	public boolean lock(String password) throws IOException {
        synchronized (publickeys) {
            if (lockedPassword == null) {
                lockedPassword = password;

                Iterator<KeyStoreListener> it = listeners.iterator();
                KeyStoreListener listener;

                while (it.hasNext()) {
                    listener = it.next();
                    listener.onLock(this);
                }

                return true;
            } else {
                return false;
            }
        }
    }

    /* (non-Javadoc)
	 * @see com.maverick.agent.IKeyStore#unlock(java.lang.String)
	 */
    @Override
	public boolean unlock(String password) throws IOException {
        synchronized (publickeys) {
            if (lockedPassword != null) {
                if (password.equals(lockedPassword)) {
                    lockedPassword = null;

                    Iterator<KeyStoreListener> it = listeners.iterator();
                    KeyStoreListener listener;

                    while (it.hasNext()) {
                        listener = it.next();
                        listener.onUnlock(this);
                    }

                    return true;
                }
            }

            return false;
        }
    }
    
    /* (non-Javadoc)
	 * @see com.maverick.agent.IKeyStore#isLocked()
	 */
    @Override
	public boolean isLocked(){
    	return (lockedPassword !=null);
    }

}

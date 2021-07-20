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

package com.sshtools.common.ssh.components;

import java.io.IOException;

import com.sshtools.common.ssh.SecurityLevel;

/**
 * <p>This special cipher implementation provides an unencrypted connection. This
 * is not enabled by default and should be used with caution. To enable 
 * and use the cipher you should add the following code before you connect 
 * your SSH client.</p> 
 * 
 * <blockquote><pre>
 * SshConnector con = SshConnector.getInstance();
 * Ssh2Context ssh2Context = (Ssh2Context) con.getContext(SshConnector.SSH2);
 * ssh2Context.supportedCiphers().add("none", NoneCipher.class);
 * ssh2Context.setPreferredCipherCS("none");
 * ssh2Context.setPreferredCipherSC("none");
 * </pre><blockquote>  
 * 
 * 
 * @author Lee David Painter
 *
 */
public class NoneCipher extends SshCipher {
    public NoneCipher() {
        super("none", SecurityLevel.WEAK, 0);
    }

    /**
     * Get the cipher block size.
     *
     * @return the block size in bytes.
     * @todo Implement this com.maverick.ssh.cipher.SshCipher method
     */
    public int getBlockSize() {
        return 8;
    }
    
    public int getKeyLength() {
    	return 8;
    }

    /**
     * Initialize the cipher with up to 40 bytes of iv and key data.
     *
     * @param mode the mode to operate
     * @param iv the initiaization vector
     * @param keydata the key data
     * @throws IOException
     * @todo Implement this com.maverick.ssh.cipher.SshCipher method
     */
    public void init(int mode, byte[] iv, byte[] keydata) throws IOException {
    }

    /**
     * Transform the byte array according to the cipher mode; it is legal for
     * the source and destination arrays to reference the same physical array
     * so care should be taken in the transformation process to safeguard
     * this rule.
     *
     * @param src byte[]
     * @param start int
     * @param dest byte[]
     * @param offset int
     * @param len int
     * @throws IOException
     * @todo Implement this com.maverick.ssh.cipher.SshCipher method
     */
    public void transform(byte[] src, int start, byte[] dest, int offset,
                          int len) throws IOException {
    }

	@Override
	public String getProviderName() {
		return "None";
	}
}

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

import com.sshtools.common.ssh.SecureComponent;
import com.sshtools.common.ssh.SshException;

/**
 * This interface should be implemented by all message authentication
 * implementations.
 * @author Lee David Painter
 *
 */
public interface SshHmac extends SshComponent, SecureComponent {
	
   /**
    * The size of the message digest output by the hmac algorithm
    * @return
    */
   public int getMacSize();
   
   /**
    * The length of the message digest output by this implementation (maybe lower than mac size);
    * @return
    */
   public int getMacLength();

   public void generate(long sequenceNo, byte[] data, int offset,
           int len, byte[] output, int start);

   public void init(byte[] keydata) throws SshException;

   public boolean verify(long sequenceNo, byte[] data, int start, int len,
           byte[] mac, int offset);
   
   public void update(byte[] b);
   
   public byte[] doFinal();
   
   public String getAlgorithm();
   
   boolean isETM();
   
}

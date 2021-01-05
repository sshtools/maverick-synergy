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

/*
 * (c) 2002-2023 JADAPTIVE Limited. All Rights Reserved.
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

package com.sshtools.common.publickey;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Enumeration;
import java.util.Hashtable;

import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.SshIOException;
import com.sshtools.common.ssh.components.ComponentManager;
import com.sshtools.common.ssh.components.SshCipher;
import com.sshtools.common.ssh.components.jce.AES128Cbc;
import com.sshtools.common.util.Base64;

class PEMWriter
   extends PEM {
  private String type;
  private Hashtable<String,String> header = new Hashtable<String,String>();

  /**
   * Creates a new PEMWriter object.
   */
  public PEMWriter() {
  }

  /**
   *
   *
   * @param w
   *
   * @throws IOException
   */
  public void write(Writer w, byte[] payload) {
    PrintWriter writer = new PrintWriter(w, true);
    writer.println(PEM_BEGIN + type + PEM_BOUNDARY);

    if(!header.isEmpty()) {
      for(Enumeration<String> e = header.keys(); e.hasMoreElements(); ) {
        String key = e.nextElement();
        String value = (String)header.get(key);

        writer.print(key + ": ");

        if((key.length() + value.length() + 2) > MAX_LINE_LENGTH) {
          int offset = Math.max(MAX_LINE_LENGTH - key.length() - 2, 0);
          writer.println(value.substring(0, offset) + "\\");

          for(; offset < value.length();
              offset += MAX_LINE_LENGTH) {
            if((offset + MAX_LINE_LENGTH) >= value.length()) {
              writer.println(value.substring(offset));
            }
            else {
              writer.println(value.substring(offset,
                                             offset + MAX_LINE_LENGTH) + "\\");
            }
          }
        }
        else {
          writer.println(value);
        }
      }

      writer.println();
    }

    writer.println(Base64.encodeBytes(payload, false));
    writer.println(PEM_END + type + PEM_BOUNDARY);
  }

  /**
   *
   *
   * @param payload
   * @param passphrase
   *
   */
  public byte[] encryptPayload(byte[] payload, String passphrase)
     throws
     IOException {
    try {
		if((passphrase == null) || (passphrase.length() == 0)) {
		  // Simple case: no passphrase means no encryption of the private key
		  return payload;
		}

		byte[] iv = new byte[16];
		ComponentManager.getInstance().getRND().nextBytes(iv);

		StringBuffer ivString = new StringBuffer(16);

		for(int i = 0; i < iv.length; i++) {
		  ivString.append(HEX_CHARS[((iv[i] >>> 4)& 0x0f) ]);
		  ivString.append(HEX_CHARS[iv[i] & 0x0f]);
		}

		header.put("DEK-Info", System.getProperty("maverick.privatekey.encryption", "AES-128-CBC") + "," + ivString);
		header.put("Proc-Type", "4,ENCRYPTED");

		byte[] keydata = getKeyFromPassphrase(passphrase, iv, 16);

		SshCipher cipher = new AES128Cbc();
		cipher.init(SshCipher.ENCRYPT_MODE, iv, keydata);

		int padding = cipher.getBlockSize() - (payload.length % cipher.getBlockSize());
		if(padding > 0) {
		  byte[] payloadWithPadding = new byte[payload.length + padding];
		  System.arraycopy(payload, 0, payloadWithPadding, 0, payload.length);
		  for(int i = payload.length; i < payloadWithPadding.length; i++) {
		    payloadWithPadding[i] = (byte)padding;
		  }
		  payload = payloadWithPadding;
		}

		  cipher.transform(payload, 0, payload, 0, payload.length);

		  return  payload;

	} catch (SshException e) {
		throw new SshIOException(e);
	}
  }

  /**
   *
   *
   * @return Hashtable
   */
  public Hashtable<String,String> getHeader() {
    return header;
  }

  /**
   *
   *
   * @return String
   */
  public String getType() {
    return type;
  }


  /**
   *
   *
   * @param string
   */
  public void setType(String string) {
    type = string;
  }
}

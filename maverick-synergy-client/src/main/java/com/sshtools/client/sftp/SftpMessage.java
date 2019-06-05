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
package com.sshtools.client.sftp;

import java.io.IOException;

import com.sshtools.client.tasks.Message;
import com.sshtools.common.ssh.ByteArrays;
import com.sshtools.common.util.ByteArrayReader;

public class SftpMessage extends ByteArrayReader implements Message {

      int type;
      int requestId;

      SftpMessage(byte[] msg) throws IOException {
          super(msg);
          type = read();
          requestId = (int) readInt();
      }

      public int getType() {
          return type;
      }

      public int getMessageId() {
          return requestId;
      }

      public void release() {
    	  ByteArrays.getInstance().releaseByteArray(super.buf);
    	  close();
      }
  }

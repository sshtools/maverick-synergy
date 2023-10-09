package com.sshtools.client.sftp;

import java.io.IOException;

import com.sshtools.client.tasks.Message;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.synergy.ssh.ByteArrays;

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

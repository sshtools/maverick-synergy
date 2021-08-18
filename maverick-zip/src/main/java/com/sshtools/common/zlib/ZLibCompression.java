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

package com.sshtools.common.zlib;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.jcraft.jzlib.JZlib;
import com.jcraft.jzlib.ZStream;
import com.sshtools.common.ssh.compression.SshCompression;

@SuppressWarnings("deprecation")
public class ZLibCompression
   implements SshCompression {

  public ZLibCompression() {
    stream = new ZStream();
  }

  public String getAlgorithm() {
    return "zlib";
  }

  static private final int BUF_SIZE = 65535;

  ByteArrayOutputStream compressOut = new ByteArrayOutputStream(BUF_SIZE);
  ByteArrayOutputStream uncompressOut = new ByteArrayOutputStream(BUF_SIZE);

//  private int type;
  private ZStream stream;

  private byte[] inflated_buf = new byte[BUF_SIZE];
  private byte[] tmpbuf = new byte[BUF_SIZE];

  public void init(int type, int level) {
    if(type == SshCompression.DEFLATER) {
      stream.deflateInit(level);
//      this.type = SshCompression.DEFLATER;
    }
    else if(type == SshCompression.INFLATER) {
      stream.inflateInit();
//      this.type = SshCompression.INFLATER;
    }
  }


  public byte[] compress(byte[] buf, int start, int len) throws IOException {

    compressOut.reset();
    stream.next_in = buf;
    stream.next_in_index = start;
    stream.avail_in = len - start;
    int status;

    do {
      stream.next_out = tmpbuf;
      stream.next_out_index = 0;
      stream.avail_out = BUF_SIZE;
      status = stream.deflate(JZlib.Z_PARTIAL_FLUSH);
      switch(status) {
        case JZlib.Z_OK:
          compressOut.write(tmpbuf, 0, BUF_SIZE - stream.avail_out);
          break;
        default:
          throw new IOException("compress: deflate returnd " + status);
      }
    }
    while(stream.avail_out == 0);

    return compressOut.toByteArray();
  }

  public byte[] uncompress(byte[] buffer, int start, int length) throws IOException {

//    int inflated_end = 0;
    uncompressOut.reset();

    stream.next_in = buffer;
    stream.next_in_index = start;
    stream.avail_in = length;

    while(true) {
      stream.next_out = inflated_buf;
      stream.next_out_index = 0;
      stream.avail_out = BUF_SIZE;
      int status = stream.inflate(JZlib.Z_PARTIAL_FLUSH);
      switch(status) {
        case JZlib.Z_OK:
          uncompressOut.write(inflated_buf, 0, BUF_SIZE - stream.avail_out);
          break;
        case JZlib.Z_BUF_ERROR:
          return uncompressOut.toByteArray();
        default:
          throw new IOException("uncompress: inflate returnd " + status);
      }
    }
  }

}

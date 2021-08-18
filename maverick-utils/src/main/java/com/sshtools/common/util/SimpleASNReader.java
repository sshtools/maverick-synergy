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


package com.sshtools.common.util;

import java.io.IOException;

/**
 *
 *
 * @author $author$
 */
public class SimpleASNReader {
  private byte[] data;
  private int offset;

  /**
   * Creates a new SimpleASNReader object.
   *
   * @param data
   */
  public SimpleASNReader(byte[] data) {
    this.data = data;
    this.offset = 0;
  }

  /**
   *
   *
   * @param b
   *
   * @throws IOException
   */
  public void assertByte(int b) throws IOException {
    int x = getByte();

    if (x != b) {
      throw new IOException("Assertion failed, next byte value is "
                            + Integer.toHexString(x) + " instead of asserted "
                            + Integer.toHexString(b));
    }
  }

  /**
   *
   *
   * @return
   */
  public int getByte() {
    return data[offset++] & 0xff;
  }

  /**
   *
   *
   * @return
   */
  public byte[] getData() {
    int length = getLength();

    return getData(length);
  }

  /**
   *
   *
   * @return
   */
  public int getLength() {
    int b = data[offset++] & 0xff;

    if ( (b & 0x80) != 0) {
      int length = 0;

      for (int bytes = b & 0x7f; bytes > 0; bytes--) {
        length <<= 8;
        length |= (data[offset++] & 0xff);
      }

      return length;
    }

    return b;
  }

  private byte[] getData(int length) {
    byte[] result = new byte[length];
    System.arraycopy(data, offset, result, 0, length);
    offset += length;

    return result;
  }

  /**
   *
   *
   * @return
   */
  public boolean hasMoreData() {
    return offset < data.length;
  }
}

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

import java.io.ByteArrayOutputStream;

/**
 *
 *
 * @author $author$
 */
public class SimpleASNWriter {
  private ByteArrayOutputStream data;

  /**
   * Creates a new SimpleASNWriter object.
   */
  public SimpleASNWriter() {
    this.data = new ByteArrayOutputStream();
  }

  /**
   *
   *
   * @param b
   */
  public void writeByte(int b) {
    data.write(b);
  }

  
  public void write(byte[] b) {
	  data.write(b, 0, b.length);
  }
  
  /**
   *
   *
   * @param b
   */
  public void writeData(byte[] b) {
    writeLength(b.length);
    this.data.write(b, 0, b.length);
  }

  /**
   *
   *
   * @param length
   */
  public void writeLength(int length) {
    if (length < 0x80) {
      data.write(length);
    }
    else {
      if (length < 0x100) {
        data.write(0x81);
        data.write(length);
      }
      else if (length < 0x10000) {
        data.write(0x82);
        data.write(length >>> 8);
        data.write(length);
      }
      else if (length < 0x1000000) {
        data.write(0x83);
        data.write(length >>> 16);
        data.write(length >>> 8);
        data.write(length);
      }
      else {
        data.write(0x84);
        data.write(length >>> 24);
        data.write(length >>> 16);
        data.write(length >>> 8);
        data.write(length);
      }
    }
  }

  /**
   *
   *
   * @return
   */
  public byte[] toByteArray() {
    return data.toByteArray();
  }
}

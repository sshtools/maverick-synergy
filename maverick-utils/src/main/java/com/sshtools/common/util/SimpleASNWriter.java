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

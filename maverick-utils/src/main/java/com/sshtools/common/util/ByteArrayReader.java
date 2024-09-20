package com.sshtools.common.util;

/*-
 * #%L
 * Utils
 * %%
 * Copyright (C) 2002 - 2024 JADAPTIVE Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;

/**
 *
 * <p>Utiltiy class to read common parameter types from a byte array.</p>
 *
 * @author Lee David Painter
 */
public class ByteArrayReader
    extends ByteArrayInputStream {

  private static String CHARSET_ENCODING = "UTF8";

  public static boolean encode;

  static {
      setCharsetEncoding(CHARSET_ENCODING);
  }
  /**
   * Construct a reader.
   * @param buffer
   * @param start
   * @param len
   */
  public ByteArrayReader(byte[] buffer, int start, int len) {
    super(buffer, start, len);
  }

  public ByteArrayReader(byte[] buffer) {
    super(buffer, 0, buffer.length);
  }

  /**
   * Provides access to the underlying array
   * @return byte[]
   */
  public byte[] array() {
      return this.buf;
  }

  /**
   * Allows the default encoding to be overriden for String variables processed
   * by the class. This currently defaults to UTF-8.
   *
   * @param charset
   * @throws UnsupportedEncodingException
   */
  public static void setCharsetEncoding(String charset) {
    try {

      String test = "123456890";
      test.getBytes(charset);
      CHARSET_ENCODING = charset;
      encode = true;
    }
    catch (UnsupportedEncodingException ex) {
      // Reset the encoding to default
      CHARSET_ENCODING = "";
      encode = false;
    }
  }

  /**
   * Get the current encoding being used for Strings variables.
   * @return
   */
  public static String getCharsetEncoding() {
    return CHARSET_ENCODING;
  }

  private void checkLength(long len) throws IOException {
    if(len > available()) {
    	throw new IOException(
    			String.format("Unexpected length of %d bytes exceeds available data of %d bytes", 
    					len, available()));
    }
  }
  /**
   * Read until the buffer supplied is full.
   * @param b
   * @param off
   * @param len
   * @throws IOException
   */
  public void readFully(byte b[], int off, int len) throws IOException {
   if (len < 0)
       throw new IndexOutOfBoundsException();
   checkLength(len);
   int n = 0;
   while (n < len) {
       int count = read(b, off + n, len - n);
       if (count < 0)
        throw new EOFException("Could not read number of bytes requested: " + len + ", got " + n + " into buffer size " + b.length + " at offset " + off);
       n += count;
   }
  }
  
  /**
   * Write a boolean value to the array.
   * @param b
   * @throws IOException
   */
  public boolean readBoolean() throws IOException {
    return read()==1;
  }  

  public void readFully(byte[] b) throws IOException {
    readFully(b,0,b.length);
  }

  /**
   * Read a BigInteger from the array.
   * @return the BigInteger value.
   * @throws IOException
   */
  public BigInteger readBigInteger() throws IOException {
    int len = (int) readInt();
    checkLength(len);
    byte[] raw = new byte[len];
    readFully(raw);
    return new BigInteger(raw);
  }

  public UnsignedInteger64 readUINT64() throws IOException {
    byte[] raw = new byte[9];
    readFully(raw, 1, 8);
    return new UnsignedInteger64(raw);
  }

  public UnsignedInteger32 readUINT32() throws IOException {
    return new UnsignedInteger32(readInt());
  }

  /**
   * Read an integer (4 bytes) from the array. This is returned as a long
   * as we deal with unsigned ints so the value may be higher than the
   * standard java int.
   * @param data
   * @param start
   * @return the value represent by a long.
   */
  public static long readInt(byte[] data, int start) {
    long ret = ( ( (long) (data[start] & 0xFF) << 24) & 0xFFFFFFFFL)
        | ( (data[start + 1] & 0xFF) << 16)
        | ( (data[start + 2] & 0xFF) << 8) | ( (data[start + 3] & 0xFF) << 0);

    return ret;
  }
  
  
  public static short readShort(byte[] data, int start) {
	  short ret = (short)((data[start] & 0xFF) << 8 | (data[start+1] & 0xFF) << 0);

	  return ret;
  }


  /**
   * Read a binary string from the array.
   * @return the byte array.
   * @throws IOException
   */
  public byte[] readBinaryString() throws IOException {
    int len = (int) readInt();
    checkLength(len);
    byte[] buf = new byte[len];
    readFully(buf);
    return buf;
  }

  /**
   * Read an integer (4 bytes) from the array. This is returned as a long
   * as we deal with unsigned ints so the value may be higher than the
   * standard java int.
   * @return the integer value as a long.
   * @throws IOException
   */
  public long readInt() throws IOException {
    int ch1 = read();
    int ch2 = read();
    int ch3 = read();
    int ch4 = read();
    if ((ch1 | ch2 | ch3 | ch4) < 0)
        throw new EOFException();
    return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0)) & 0xFFFFFFFFL;

  }

  /**
   * Read a String from the array.
   * @return the String value.
   * @throws IOException
   */
  public String readString() throws IOException {
    return readString(CHARSET_ENCODING);
  }

  /**
   * Read a String from the array converting using the given character set.
   * @param charset
   * @return
   * @throws IOException
   */
  public String readString(String charset) throws IOException {
    long len = readInt();

   checkLength(len);

    byte[] raw = new byte[ (int) len];
    readFully(raw);
    if (encode) {
      return new String(raw, charset);
    }
	return new String(raw);

  }

  public short readShort() throws IOException {
    int ch1 = read();
    int ch2 = read();

    if ( (ch1 | ch2) < 0) {
      throw new EOFException();
    }

    return (short) ( (ch1 << 8) + (ch2 << 0));
  }

  /**
   * Reads an MPINT using the first 32 bits as the length prefix
   * @return
   * @throws IOException
   */
  public BigInteger readMPINT32() throws IOException {
    int bits = (int)readInt();
    checkLength((bits + 7) / 8);
    byte[] raw = new byte[ (bits + 7) / 8 + 1];

    raw[0] = 0;
    readFully(raw, 1, raw.length - 1);

    return new BigInteger(raw);

  }

  /**
   * Reads a standard SSH1 MPINT using the first 16 bits as the length prefix
   * @return
   * @throws IOException
   */
  public BigInteger readMPINT() throws IOException {
    short bits = readShort();
    checkLength((bits + 7)/ 8);
    byte[] raw = new byte[ (bits + 7) / 8 + 1];

    raw[0] = 0;
    readFully(raw, 1, raw.length - 1);

    return new BigInteger(raw);
  }

  /**
   * Get the current position within the array.
   * @return the current position within the array
   */
  public int getPosition() {
    return this.pos;
  }
  
  public void silentClose() {
	  close();
  }
  
  public void dispose() {
	  super.buf = null;
  }
  
  public void close() {
	  try {
		  super.close();
	  } catch(IOException e) { }
  }

  public static String decodeString(byte[] data) throws IOException {
	
	try(ByteArrayReader r = new ByteArrayReader(data)) {
		return r.readString();
	}
  }
}

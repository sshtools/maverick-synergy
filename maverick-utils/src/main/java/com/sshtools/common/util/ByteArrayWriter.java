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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;

/**
 *
 * <p>Utility class to write common parameter types to a byte array.</p>
 * @author Lee David Painter
 */
public class ByteArrayWriter
    extends ByteArrayOutputStream {

  /**
   * Contruct an empty writer.
   */
  public ByteArrayWriter() {

  }

  /**
   * Construct a writer with an array size of the length supplied.
   * @param length
   */
  public ByteArrayWriter(int length) {
    super(length);
  }

  /**
   * Get the underlying byte array
   * @return the underlying byte array.
   */
  public byte[] array() {
    return buf;
  }

  /**
   * Move the position of the next byte to be written.
   * @param numBytes
   */
  public void move(int numBytes) {
    count += numBytes;
  }

  /**
   * Write a BigInteger to the array.
   * @param bi
   * @throws IOException
   */
  public void writeBigInteger(BigInteger bi) throws IOException {
    byte[] raw = bi.toByteArray();

    writeInt(raw.length);
    write(raw);
  }
  
  /**
   * Write a boolean value to the array.
   * @param b
   * @throws IOException
   */
  public void writeBoolean(boolean b) {
    write(b ? 1 : 0);
  }

  /**
   * Write a binary string to the array.
   * @param data
   * @throws IOException
   */
  public void writeBinaryString(byte[] data) throws IOException {
	  if(data==null)
		  writeInt(0);
	  else
		  writeBinaryString(data, 0, data.length);
  }

  /**
   * Write a binary string to the array.
   * @param data
   * @param offset
   * @param len
   * @throws IOException
   */
  public void writeBinaryString(byte[] data, int offset, int len) throws
      IOException {
	if(data==null)
		writeInt(0);
	else {
	    writeInt(len);
	    write(data, offset, len);
	}
  }

  public void writeMPINT(BigInteger b) {
    short bytes = (short) ( (b.bitLength() + 7) / 8);
    byte[] raw = b.toByteArray();
    writeShort( (short) b.bitLength());
    if (raw[0] == 0) {
      write(raw, 1, bytes);
    }
    else {
      write(raw, 0, bytes);
    }
  }

  public void writeShort(short s) {
    write( (s >>> 8) & 0xFF);
    write( (s >>> 0) & 0xFF);
  }

  /**
   * Write an integer to the array
   * @param i
   * @throws IOException
   */
  public void writeInt(long i) throws IOException {
    byte[] raw = new byte[4];

    raw[0] = (byte) (i >> 24);
    raw[1] = (byte) (i >> 16);
    raw[2] = (byte) (i >> 8);
    raw[3] = (byte) (i);

    write(raw);
  }

  /**
   * Write an integer to the array.
   * @param i
   * @throws IOException
   */
  public void writeInt(int i) throws IOException {
    byte[] raw = new byte[4];

    raw[0] = (byte) (i >> 24);
    raw[1] = (byte) (i >> 16);
    raw[2] = (byte) (i >> 8);
    raw[3] = (byte) (i);

    write(raw);
  }

  /**
   * Encode an integer into a 4 byte array.
   * @param i
   * @return a byte[4] containing the encoded integer.
   */
  public static byte[] encodeInt(int i) {
    byte[] raw = new byte[4];
    raw[0] = (byte) (i >> 24);
    raw[1] = (byte) (i >> 16);
    raw[2] = (byte) (i >> 8);
    raw[3] = (byte) (i);
    return raw;
  }
  
  public static byte[] encodeInt(long i) {
	    byte[] raw = new byte[4];
	    raw[0] = (byte) (i >> 24);
	    raw[1] = (byte) (i >> 16);
	    raw[2] = (byte) (i >> 8);
	    raw[3] = (byte) (i);
	    return raw;
  }
  
  public static byte[] encodeInt(UnsignedInteger32 val) {
	    byte[] raw = new byte[4];
	    long i = val.longValue();
	    raw[0] = (byte) (i >> 24);
	    raw[1] = (byte) (i >> 16);
	    raw[2] = (byte) (i >> 8);
	    raw[3] = (byte) (i);
	    return raw;
  }

  public static void encodeInt(byte[] buf, int off, int i) {
    buf[off++] = (byte) (i >> 24);
    buf[off++] = (byte) (i >> 16);
    buf[off++] = (byte) (i >> 8);
    buf[off] = (byte) (i);
  }

  public void writeUINT32(UnsignedInteger32 value) throws IOException {
    writeInt(value.longValue());
  }

  public void writeUINT64(UnsignedInteger64 value) throws IOException {
    byte[] raw = new byte[8];
    byte[] bi = stripLeadingZeros(value.bigIntValue().toByteArray());
    System.arraycopy(bi, 0, raw, raw.length - bi.length, bi.length);
    // Pad the raw data
    write(raw);
  }
  
  public static byte[] stripLeadingZeros(byte[] data) {
		int x;
		for(x=0;x<data.length;x++) {
			if(data[x] != 0) {
				break;
			}
		}
		if(x > 0) {
			byte[] tmp = new byte[data.length - x];
			System.arraycopy(data, x, tmp, 0, tmp.length);
			return tmp;
		} else {
			return data;
		}
	}

  public void writeUINT64(long value) throws IOException {
    writeUINT64(new UnsignedInteger64(value));
  }

  /*public static void writeIntToArray(byte[] array, int pos, int value) throws
      IOException {
    if ( (array.length - pos) < 4) {
      throw new IOException(
          "Not enough data in array to write integer at position "
          + String.valueOf(pos));
    }
    array[pos] = (byte) (value >> 24);
    array[pos + 1] = (byte) (value >> 16);
    array[pos + 2] = (byte) (value >> 8);
    array[pos + 3] = (byte) (value);
   }*/

  /**
   * Write a string to the byte array.
   * @param str
   * @throws IOException
   */
    public void writeString(String str) throws IOException {
      writeString(str, ByteArrayReader.getCharsetEncoding());
    }

    /**
     * Write a String to the byte array converting the bytes using the
     * given character set.
     * @param str
     * @param charset
     * @throws IOException
     */
  public void writeString(String str, String charset) throws IOException {

    if (str == null) {
      writeInt(0);
    }
    else {
      byte[] tmp;

      if(ByteArrayReader.encode)
        tmp = str.getBytes(charset);
      else
        tmp = str.getBytes();

      writeInt(tmp.length);
      write(tmp);
    }
  }
  
  public void silentClose() {
	  try {
		close();
	} catch (IOException e) {
	}
  }
  
  public void dispose() {
	  super.buf = null;
  }

  public static byte[] encodeString(String memo) throws IOException {
	try(ByteArrayWriter w = new ByteArrayWriter()) {
		w.writeString(memo);
		return w.toByteArray();
	}
  }	

}

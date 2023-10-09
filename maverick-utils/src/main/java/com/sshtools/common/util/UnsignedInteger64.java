package com.sshtools.common.util;

/*-
 * #%L
 * Utils
 * %%
 * Copyright (C) 2002 - 2023 JADAPTIVE Limited
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

import java.math.BigInteger;

/**
 * This class provides a 64bit unsigned integer.
 *
 * @author Lee David Painter
 */
public class UnsignedInteger64 {

  public final static UnsignedInteger64 ZERO = new UnsignedInteger64(0L);
  public final static UnsignedInteger64 ONE = new UnsignedInteger64(1L);
  
  /**  */
  public final static BigInteger MAX_VALUE = new BigInteger(
      "18446744073709551615");
       
  /**  */
  public final static BigInteger MIN_VALUE = new BigInteger("0");
  private BigInteger bigInt;

  /**
   * Creates a new UnsignedInteger64 object.
   *
   * @param sval
   *
   * @throws NumberFormatException
   */
  public UnsignedInteger64(String sval) throws NumberFormatException {
    bigInt = new BigInteger(sval);

    if ( (bigInt.compareTo(MIN_VALUE) < 0)
        || (bigInt.compareTo(MAX_VALUE) > 0)) {
      throw new NumberFormatException();
    }
  }

  /**
   * Creates a new UnsignedInteger64 object.
   *
   * @param bval
   *
   * @throws NumberFormatException
   */
  public UnsignedInteger64(byte[] bval) throws NumberFormatException {
    bigInt = new BigInteger(bval);

    if ( (bigInt.compareTo(MIN_VALUE) < 0)
        || (bigInt.compareTo(MAX_VALUE) > 0)) {
      throw new NumberFormatException();
    }
  }

  public UnsignedInteger64(long value) {
    bigInt = BigInteger.valueOf(value);
  }

  /**
   * Creates a new UnsignedInteger64 object.
   *
   * @param input
   *
   * @throws NumberFormatException
   */
  public UnsignedInteger64(BigInteger input) {
    bigInt = new BigInteger(input.toString());

    if ( (bigInt.compareTo(MIN_VALUE) < 0)
        || (bigInt.compareTo(MAX_VALUE) > 0)) {
      throw new NumberFormatException();
    }
  }

  /**
   *
   * Compares this unsigned integer to an object.
   *
   * @param o
   *
   * @return
   */
  public boolean equals(Object o) {
    if(o==null){
    	return false;
    }
	try {
      UnsignedInteger64 u = (UnsignedInteger64) o;

      return u.bigInt.equals(this.bigInt);
    }
    catch (ClassCastException ce) {
      // This was not an UnsignedInt64, so equals should fail.
      return false;
    }
  }

  /**
   * Return a BigInteger value of the unsigned integer.
   *
   * @return
   */
  public BigInteger bigIntValue() {
    return bigInt;
  }

  /**
   * Return a long value of the unsigned integer.
   *
   * @return
   */
  public long longValue() {
    return bigInt.longValue();
  }


  /**
   * Return a String representation of the unsigned integer
   *
   * @return
   */
  public String toString() {
    return bigInt.toString(10);
  }

  /**
   * Return the objects hash code.
   *
   * @return
   */
  public int hashCode() {
    return bigInt.hashCode();
  }

  /**
   * Add an unsigned integer to another unsigned integer.
   *
   * @param x
   * @param y
   *
   * @return
   */
  public static UnsignedInteger64 add(UnsignedInteger64 x, UnsignedInteger64 y) {
    return new UnsignedInteger64(x.bigInt.add(y.bigInt));
  }

  /**
   * Add an unsigned integer to an int.
   *
   * @param x
   * @param y
   *
   * @return
   */
  public static UnsignedInteger64 add(UnsignedInteger64 x, int y) {
    return new UnsignedInteger64(x.bigInt.add(BigInteger.valueOf(y)));
  }

  /**
   * Returns a byte array encoded with the unsigned integer.
   * @return
   */
  public byte[] toByteArray() {
    byte[] raw = new byte[8];
    byte[] bi = bigIntValue().toByteArray();
    System.arraycopy(bi, 0, raw, raw.length - bi.length, bi.length);
    return raw;
  }
}

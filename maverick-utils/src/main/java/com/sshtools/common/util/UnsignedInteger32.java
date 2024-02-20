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

/**
 * This class provides a 32bit unsigned integer.
 *
 * @author Lee David Painter
 */
public class UnsignedInteger32 {

  /** The maximum value of a 32bit unsigned integer */
  public final static long MAX_VALUE = 0xffffffffL;

  /** The minimum value of a 32bit unsigned integer */
  public final static long MIN_VALUE = 0;

  public final static UnsignedInteger32 ZERO = new UnsignedInteger32(0);
  
  private final Long value;

  /**
   * Creates a new UnsignedInteger32 object.
   *
   * @param a
   *
   * @throws NumberFormatException
   */
  public UnsignedInteger32(long a) {
    if ( (a < MIN_VALUE) || (a > MAX_VALUE)) {
      throw new NumberFormatException();
    }

    value = Long.valueOf(a);
  }

  /**
   * Creates a new UnsignedInteger32 object.
   *
   * @param a
   *
   * @throws NumberFormatException
   */
  public UnsignedInteger32(String a) throws NumberFormatException {

    long longValue = Long.parseLong(a);

    if ( (longValue < MIN_VALUE) || (longValue > MAX_VALUE)) {
      throw new NumberFormatException();
    }

    value = Long.valueOf(longValue);
  }

  /**
   * Returns the long value of the unsigned integer cast to an int
   *
   * @return
   */
  public int intValue() {
    return (int) value.longValue();
  }

  /**
   * Returns the long value of this unsigned integer.
   *
   * @return
   */
  public long longValue() {
    return value.longValue();
  }


  /**
   * Returns a String representation of the unsigned integer.
   *
   * @return
   */
  public String toString() {
    return value.toString();
  }

  /**
   * Returns the objects hash code.
   *
   * @return
   */
  public int hashCode() {
    return value.hashCode();
  }

  /**
   * Compares an object.
   *
   * @param o
   *
   * @return
   */
  public boolean equals(Object o) {
    if (! (o instanceof UnsignedInteger32)) {
      return false;
    }

    return ( ( (UnsignedInteger32) o).value.equals(this.value));
  }

  /**
   * Add two unsigned integers together.
   *
   * @param x
   * @param y
   *
   * @return UnsignedInteger32
   */
  public static UnsignedInteger32 add(UnsignedInteger32 x, UnsignedInteger32 y) {
    return new UnsignedInteger32(x.longValue() + y.longValue());
  }

  /**
   * Add an int to an unsigned integer.
   *
   * @param x
   * @param y
   *
   * @return UnsignedInteger32
   */
  public static UnsignedInteger32 add(UnsignedInteger32 x, long y) {
    return new UnsignedInteger32(x.longValue() + y);
  }


  public static UnsignedInteger32 deduct(UnsignedInteger32 x, UnsignedInteger32 y) {
		return new UnsignedInteger32(x.longValue() - y.longValue());
  }
  
  public static UnsignedInteger32 deduct(UnsignedInteger32 x, long y) {
		return new UnsignedInteger32(x.longValue() - y);
  }
}

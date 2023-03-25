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
  
  private Long value;

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

/***
 *
 * The contents of this package has been derived from the TelnetD library
 * available from http://sourceforge.net/projects/telnetd
 *
 * The original license of the source code is as follows:
 *
 * TelnetD library (embeddable telnet daemon)
 * Copyright (C) 2000 Dieter Wimberger
 *
 * This library is free software; you can either redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License version 2.1,1999 as published by the Free Software Foundation
 * (see copy received along with the library), or under the terms of the
 * BSD-style license received along with this library.
 ***/
package com.sshtools.server.vshell.terminal;

import java.util.Vector;

class CharBuffer {

	// Members

	private Vector<Character> myBuffer;

	private int mySize;

	public CharBuffer(int size) {

		myBuffer = new Vector<Character>(size);

		mySize = size;

	}

	// constructor

	public char getCharAt(int pos) throws IndexOutOfBoundsException {

		return ((Character) myBuffer.elementAt(pos)).charValue();

	}

	// getCharAt

	public void setCharAt(int pos, char ch) throws IndexOutOfBoundsException {

		myBuffer.setElementAt(new Character(ch), pos);

	}

	// setCharAt

	public void insertCharAt(int pos, char ch) throws IndexOutOfBoundsException {

		myBuffer.insertElementAt(new Character(ch), pos);

	}

	// insertCharAt

	public void append(char aChar) {

		myBuffer.addElement(new Character(aChar));

	}

	// append

	public void removeCharAt(int pos) throws IndexOutOfBoundsException {

		myBuffer.removeElementAt(pos);

	}

	// removeCharAt

	public void clear() {

		myBuffer.removeAllElements();

	}

	// clear

	public int size() {

		return myBuffer.size();

	}

	// size

	public String toString() {

		StringBuffer sbuf = new StringBuffer();

		for (int i = 0; i < myBuffer.size(); i++) {

			sbuf.append(((Character) myBuffer.elementAt(i)).charValue());

		}

		return sbuf.toString();

	}

	// toString

	public void ensureSpace(int chars) throws BufferOverflowException {

		if (chars > (mySize - myBuffer.size())) {

			throw new BufferOverflowException();

		}

	}

	// ensureSpace

}

// class CharBuffer


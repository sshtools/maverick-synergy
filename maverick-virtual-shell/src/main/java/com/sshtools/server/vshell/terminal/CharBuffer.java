/**
 * (c) 2002-2019 JADAPTIVE Limited. All Rights Reserved.
 *
 * This file is part of the Maverick Synergy Java SSH API.
 *
 * Maverick Synergy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Maverick Synergy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
 */
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


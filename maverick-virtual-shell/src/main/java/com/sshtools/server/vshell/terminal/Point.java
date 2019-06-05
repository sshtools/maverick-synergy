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
 * along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
 */
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

public class Point {

	// Members

	private int myRow;

	private int myCol;

	public Point() {

		myCol = 0;

		myRow = 0;

	}

	// constructor

	public Point(int col, int row) {

		myCol = col;

		myRow = row;

	}

	// constructor

	public void setLocation(int col, int row) {

		myCol = col;

		myRow = row;

	}

	// setLocation

	public void move(int col, int row) {

		myCol = col;

		myRow = row;

	}

	// move

	public int getColumn() {

		return myCol;

	}

	// getColumn

	public void setColumn(int col) {

		myCol = col;

	}

	// setColumn

	public int getRow() {

		return myRow;

	}

	// getRow

	public void setRow(int row) {

		myRow = row;

	}

	// setRow

}

// class Point


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


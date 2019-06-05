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

public class Dimension {

	// Members

	private int myHeight;

	private int myWidth;

	public Dimension() {

		myHeight = 0;

		myWidth = 0;

	}

	// constructor

	public Dimension(int width, int height) {

		myHeight = height;

		myWidth = width;

	}

	// constructor

	public int getWidth() {

		return myWidth;

	}

	// getWidth

	public void setWidth(int width) {

		myWidth = width;

	}

	// setWidth

	public int getHeight() {

		return myHeight;

	}

	// getHeight

	public void setHeight(int height) {

		myHeight = height;

	}

	// setHeight

}

// class Dimension


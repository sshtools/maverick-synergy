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

import java.io.IOException;

public abstract class Component {

	protected String myName;

	protected TerminalOutput myIO;

	protected Point myPosition;

	protected Dimension myDim;

	public Component(TerminalOutput io, String name) {

		myIO = io;

		myName = name;

	}

	// constructor

	abstract public void draw() throws IOException;

	public String getName() {

		return myName;

	}

	// getName

	public Point getLocation() {

		return myPosition;

	}

	// getLocation

	public void setLocation(Point pos) {

		myPosition = pos;

	}

	// setLocation

	public void setLocation(int col, int row) {

		if (myPosition != null) {

			myPosition.setColumn(col);

			myPosition.setRow(row);

		}

		else {

			myPosition = new Point(col, row);

		}

	}

	// set Location

	public Dimension getDimension() {

		return myDim;

	}

	// getDimension

	protected void setDimension(Dimension dim) {

		myDim = dim;

	}

	// setDimension

}

// class Component


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

public class vt100 extends BasicTerminal {
	// getEraseSequence

	public byte[] getSpecialSequence(int function) {
		byte[] sequence = null;
		switch (function) {
		case TerminalOutput.GRAPHICS_ON:
			sequence = new byte[3];
			sequence[0] = ESC;
			sequence[1] = '(';
			sequence[2] = '0';
			break;
		case TerminalOutput.GRAPHICS_OFF:
			sequence = new byte[3];
			sequence[0] = ESC;
			sequence[1] = '(';
			sequence[2] = 'B';
			break;
		case TerminalOutput.BOX_LEFT:
			return new byte[] { 0x78 };
		case TerminalOutput.BOX_CENTER:
			return new byte[] { 0x78 };
		case TerminalOutput.BOX_RIGHT:
			return new byte[] { 0x78 };
		case TerminalOutput.BOX_TOP_LEFT:
			return new byte[] { 0x6c };
		case TerminalOutput.BOX_TOP:
			return new byte[] { 0x71 };
		case TerminalOutput.BOX_TOP_MIDDLE:
			return new byte[] { 0x77 };
		case TerminalOutput.BOX_TOP_RIGHT:
			return new byte[] { 0x6b };
		case TerminalOutput.BOX_MIDDLE_LEFT:
			return new byte[] { 0x74 };
		case TerminalOutput.BOX_MIDDLE:
			return new byte[] { 0x71 };
		case TerminalOutput.BOX_MIDDLE_MIDDLE:
			return new byte[] { 0x6e };
		case TerminalOutput.BOX_MIDDLE_RIGHT:
			return new byte[] { 0x75 };
		case TerminalOutput.BOX_BOTTOM_LEFT:
			return new byte[] { 0x6d };
		case TerminalOutput.BOX_BOTTOM:
			return new byte[] { 0x71 };
		case TerminalOutput.BOX_BOTTOM_MIDDLE:
			return new byte[] { 0x76 };
		case TerminalOutput.BOX_BOTTOM_RIGHT:
			return new byte[] { 0x6a };
		}
		return sequence;

	}

	public boolean supportsSGR() {
		return true;
	}

	// supportsSGR
	public boolean supportsScrolling() {
		return true;
	}
	// supportsSoftScroll
}

// class vt100


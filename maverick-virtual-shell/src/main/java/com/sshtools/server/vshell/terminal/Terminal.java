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

public interface Terminal {

	// Constants

	public static final byte EOT = 4;

	public static final byte BS = 8;

	public static final byte DEL = 127;

	public static final byte HT = 9;

	public static final byte FF = 12;

	public static final byte SGR = 1;

	public static final byte CAN = 24;

	public static final byte ESC = 27;

	public static final byte LSB = 91;

	public static final byte SEMICOLON = 59;

	public static final byte A = 65;

	public static final byte B = 66;

	public static final byte C = 67;

	public static final byte D = 68;

	public static final byte E = 69; // for next Line (like CR/LF)

	public static final byte H = 72; // for Home and Positionsetting or f

	public static final byte f = 102;

	public static final byte r = 114;

	public static final byte LE = 75; // K...line erase actions related

	public static final byte SE = 74; // J...screen erase actions related

	public byte[] getEraseSequence(int eraseFunc);

	public byte[] getCursorMoveSequence(int dir, int times);

	public byte[] getCursorPositioningSequence(int[] pos);

	public byte[] getSpecialSequence(int sequence);

	public byte[] getScrollMarginsSequence(int topmargin, int bottommargin);

	public byte[] getGRSequence(int type, int param);

	public String format(String str);

	public byte[] getInitSequence();

	public boolean supportsSGR();

	public boolean supportsScrolling();

	public int getAtomicSequenceLength();

}

// interface Terminal


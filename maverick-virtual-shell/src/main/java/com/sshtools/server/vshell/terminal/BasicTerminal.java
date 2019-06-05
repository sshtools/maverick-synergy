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

public abstract class BasicTerminal

implements Terminal {

	// Associations

	protected Colorizer myColorizer;

	public BasicTerminal() {
		myColorizer = Colorizer.getReference();
	}

	// translateEscapeSequence

	public byte[] getCursorMoveSequence(int direction, int times) {
		byte[] sequence = null;
		if (times == 1) {
			sequence = new byte[3];
		} else {
			sequence = new byte[times * 3];
		}

		for (int g = 0; g < (times * 3); g++) {
			sequence[g] = ESC;
			sequence[g + 1] = LSB;
			switch (direction) {
			case TerminalOutput.UP:
				sequence[g + 2] = A;
				break;
			case TerminalOutput.DOWN:
				sequence[g + 2] = B;
				break;
			case TerminalOutput.RIGHT:
				sequence[g + 2] = C;
				break;
			case TerminalOutput.LEFT:
				sequence[g + 2] = D;
				break;
			default:
				break;
			}
			g = g + 2;
		}

		return sequence;

	}

	// getCursorMoveSequence

	public byte[] getCursorPositioningSequence(int[] pos) {
		byte[] sequence = null;
		if (pos == TerminalOutput.HOME) {
			sequence = new byte[3];
			sequence[0] = ESC;
			sequence[1] = LSB;
			sequence[2] = H;
		} else {
			// first translate integer coords into digits
			byte[] rowdigits = translateIntToDigitCodes(pos[0]);
			byte[] columndigits = translateIntToDigitCodes(pos[1]);
			int offset = 0;

			// now build up the sequence:
			sequence = new byte[4 + rowdigits.length + columndigits.length];
			sequence[0] = ESC;
			sequence[1] = LSB;

			// now copy the digit bytes
			System.arraycopy(rowdigits, 0, sequence, 2, rowdigits.length);
			// offset is now 2+rowdigits.length

			offset = 2 + rowdigits.length;
			sequence[offset] = SEMICOLON;

			offset++;
			System.arraycopy(columndigits, 0, sequence, offset, columndigits.length);
			offset = offset + columndigits.length;

			sequence[offset] = H;

		}

		return sequence;
	}

	// getCursorPositioningSequence

	public byte[] getEraseSequence(int eraseFunc) {
		byte[] sequence = null;
		switch (eraseFunc) {
		case TerminalOutput.EEOL:

			sequence = new byte[3];

			sequence[0] = ESC;

			sequence[1] = LSB;

			sequence[2] = LE;

			break;

		case TerminalOutput.EBOL:

			sequence = new byte[4];

			sequence[0] = ESC;

			sequence[1] = LSB;

			sequence[2] = 49; // Ascii Code of 1

			sequence[3] = LE;

			break;

		case TerminalOutput.EEL:

			sequence = new byte[4];

			sequence[0] = ESC;

			sequence[1] = LSB;

			sequence[2] = 50; // Ascii Code 2

			sequence[3] = LE;

			break;

		case TerminalOutput.EEOS:

			sequence = new byte[3];

			sequence[0] = ESC;

			sequence[1] = LSB;

			sequence[2] = SE;

			break;

		case TerminalOutput.EBOS:

			sequence = new byte[4];

			sequence[0] = ESC;

			sequence[1] = LSB;

			sequence[2] = 49; // Ascii Code of 1

			sequence[3] = SE;

			break;

		case TerminalOutput.EES:

			sequence = new byte[4];

			sequence[0] = ESC;

			sequence[1] = LSB;

			sequence[2] = 50; // Ascii Code of 2

			sequence[3] = SE;

			break;
		case TerminalOutput.BOX_LEFT:
			return new byte[] { '|' };
		case TerminalOutput.BOX_RIGHT:
			return new byte[] { '|' };
		case TerminalOutput.BOX_CENTER:
			return new byte[] { '|' };
		case TerminalOutput.BOX_TOP_LEFT:
			return new byte[] { '+' };
		case TerminalOutput.BOX_TOP:
			return new byte[] { '-' };
		case TerminalOutput.BOX_TOP_MIDDLE:
			return new byte[] { '+' };
		case TerminalOutput.BOX_TOP_RIGHT:
			return new byte[] { '+' };
		case TerminalOutput.BOX_MIDDLE_LEFT:
			return new byte[] { '+' };
		case TerminalOutput.BOX_MIDDLE:
			return new byte[] { '-' };
		case TerminalOutput.BOX_MIDDLE_MIDDLE:
			return new byte[] { '+' };
		case TerminalOutput.BOX_MIDDLE_RIGHT:
			return new byte[] { '+' };
		case TerminalOutput.BOX_BOTTOM_LEFT:
			return new byte[] { '+' };
		case TerminalOutput.BOX_BOTTOM:
			return new byte[] { '-' };
		case TerminalOutput.BOX_BOTTOM_MIDDLE:
			return new byte[] { '+' };
		case TerminalOutput.BOX_BOTTOM_RIGHT:
			return new byte[] { '+' };

		default:

			break;

		}

		return sequence;

	}

	// getEraseSequence

	public byte[] getSpecialSequence(int function) {

		byte[] sequence = null;

		switch (function) {

		case TerminalOutput.STORECURSOR:

			sequence = new byte[2];

			sequence[0] = ESC;

			sequence[1] = 55; // Ascii Code of 7

			break;

		case TerminalOutput.RESTORECURSOR:

			sequence = new byte[2];

			sequence[0] = ESC;

			sequence[1] = 56; // Ascii Code of 8

			break;

		}

		return sequence;

	}

	// getSpecialSequence

	public byte[] getGRSequence(int type, int param) {

		byte[] sequence = new byte[0];

		int offset = 0;

		switch (type) {

		case TerminalOutput.FCOLOR:

		case TerminalOutput.BCOLOR:

			byte[] color = translateIntToDigitCodes(param);

			sequence = new byte[3 + color.length];

			sequence[0] = ESC;

			sequence[1] = LSB;

			// now copy the digit bytes

			System.arraycopy(color, 0, sequence, 2, color.length);

			// offset is now 2+color.length

			offset = 2 + color.length;

			sequence[offset] = 109; // ASCII Code of m

			break;

		case TerminalOutput.STYLE:

			byte[] style = translateIntToDigitCodes(param);

			sequence = new byte[3 + style.length];

			sequence[0] = ESC;

			sequence[1] = LSB;

			// now copy the digit bytes

			System.arraycopy(style, 0, sequence, 2, style.length);

			// offset is now 2+style.length

			offset = 2 + style.length;

			sequence[offset] = 109; // ASCII Code of m

			break;

		case TerminalOutput.RESET:
			sequence = new byte[3];
			sequence[0] = ESC;
			sequence[1] = LSB;
			sequence[2] = 109; // ASCII Code of m
			break;
		}

		return sequence;

	}

	// getGRsequence

	public byte[] getScrollMarginsSequence(int topmargin, int bottommargin) {

		byte[] sequence = new byte[0];

		if (supportsScrolling()) {

			// first translate integer coords into digits

			byte[] topdigits = translateIntToDigitCodes(topmargin);

			byte[] bottomdigits = translateIntToDigitCodes(bottommargin);

			int offset = 0;

			// now build up the sequence:

			sequence = new byte[4 + topdigits.length + bottomdigits.length];

			sequence[0] = ESC;

			sequence[1] = LSB;

			// now copy the digit bytes

			System.arraycopy(topdigits, 0, sequence, 2, topdigits.length);

			// offset is now 2+topdigits.length

			offset = 2 + topdigits.length;

			sequence[offset] = SEMICOLON;

			offset++;

			System.arraycopy(bottomdigits, 0, sequence, offset,

			bottomdigits.length);

			offset = offset + bottomdigits.length;

			sequence[offset] = r;

		}

		return sequence;

	}

	// getScrollMarginsSequence

	public String format(String str) {

		return myColorizer.colorize(str, supportsSGR());

	}

	// format

	public byte[] getInitSequence() {

		byte[] sequence = new byte[0];

		return sequence;

	}

	// getInitSequence

	public int getAtomicSequenceLength() {

		return 2;

	}

	// getAtomicSequenceLength

	public byte[] translateIntToDigitCodes(int in) {

		return Integer.toString(in).getBytes();

	}

	// translateIntToDigitCodes

	public abstract boolean supportsSGR();

	public abstract boolean supportsScrolling();

}

// class BasicTerminal


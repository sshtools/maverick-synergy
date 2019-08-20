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

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class TerminalOutput {

	// implements BasicTerminalIO {

	public static final int EOL_CRLF = 1;

	public static final int EOL_CR = 2;

	public static final int[] HOME = {

	0, 0 };

	public static final int IOERROR = -1; // CTRL-D beim login

	public static final int
	// Positioning 10xx

	UP = 1001; // CTRL-D beim login

	public static final int DOWN = 1002;

	public static final int RIGHT = 1003;

	public static final int LEFT = 1004;

	public static final int STORECURSOR = 1051;

	public static final int RESTORECURSOR = 1052;

	public static final int GRAPHICS_ON = 1053;

	public static final int GRAPHICS_OFF = 1054;

	public static final int BOX_TOP_LEFT = 8000;
	public static final int BOX_TOP = 8001;
	public static final int BOX_TOP_MIDDLE = 8002;
	public static final int BOX_TOP_RIGHT = 8003;
	public static final int BOX_MIDDLE_LEFT = 8004;
	public static final int BOX_MIDDLE = 8005;
	public static final int BOX_MIDDLE_MIDDLE = 8006;
	public static final int BOX_MIDDLE_RIGHT = 8007;
	public static final int BOX_BOTTOM_LEFT = 8008;
	public static final int BOX_BOTTOM = 8009;
	public static final int BOX_BOTTOM_MIDDLE = 8010;
	public static final int BOX_BOTTOM_RIGHT = 8011;
	public static final int BOX_LEFT = 8012;
	public static final int BOX_RIGHT = 8013;
	public static final int BOX_CENTER = 8014;

	public static final int
	// Erasing 11xx

	EEOL = 1100; // CTRL-D beim login

	public static final int EBOL = 1101; // CTRL-D beim login

	public static final int EEL = 1103; // CTRL-D beim login

	public static final int EEOS = 1104; // CTRL-D beim login

	public static final int EBOS = 1105; // CTRL-D beim login

	public static final int EES = 1106; // CTRL-D beim login

	public static final int
	// Escape Sequence-ing 12xx

	ESCAPE = 1200; // CTRL-D beim login

	public static final int BYTEMISSING = 1201; // CTRL-D beim login

	public static final int UNRECOGNIZED = 1202; // CTRL-D beim login

	public static final int
	// Control Characters 13xx

	ENTER = 10; // CTRL-D beim login

	public static final int
	// ENTER = 1300, //LF is ENTER at the moment

	TABULATOR = 1301; // CTRL-D beim login

	public static final int DELETE = 1302; // CTRL-D beim login

	public static final int BACKSPACE = 1303; // CTRL-D beim login

	public static final int COLORINIT = 1304; // CTRL-D beim login

	public static final int HANDLED = 1305; // CTRL-D beim login

	public static final int LOGOUTREQUEST = 1306; // CTRL-D beim login

	public static final int LineUpdate = 475;

	public static final int CharacterUpdate = 476;

	public static final int ScreenpartUpdate = 477;

	public static final int EditBuffer = 575;

	public static final int LineEditBuffer = 576;

	public static final int BEL = 7;

	public static final int BS = 8;

	public static final int DEL = 127;

	public static final int CR = 13;

	public static final int LF = 10;

	public static final int FCOLOR = 10001;

	public static final int BCOLOR = 10002;

	public static final int STYLE = 10003;

	public static final int RESET = 10004;

	public static final int BOLD = 1;

	public static final int BOLD_OFF = 22;

	public static final int ITALIC = 3;

	public static final int ITALIC_OFF = 23;

	public static final int BLINK = 5;

	public static final int BLINK_OFF = 25;

	public static final int UNDERLINED = 4;

	public static final int UNDERLINED_OFF = 24;

	// Constants

	public static final int BLACK = 30;

	public static final int RED = 31;

	public static final int GREEN = 32;

	public static final int YELLOW = 33;

	public static final int BLUE = 34;

	public static final int MAGENTA = 35;

	public static final int CYAN = 36;

	public static final int WHITE = 37;

	public static final String CRLF = "\r\n";

	private Terminal terminal;

	private DataOutputStream out;

	private boolean cr;

	private boolean acousticSignalling; // flag for accoustic signalling

	private boolean autoflush; // flag for autoflushing mode

	private int eol = EOL_CRLF;

	private String term;
	private int cols;
	private int rows;
	private int width;
	private int height;
	private byte[] modes;
	private boolean display = true;

	private ByteArrayOutputStream bout = new ByteArrayOutputStream();

	private int cursorCol = 1;
	private int cursorRow = 1;

	public TerminalOutput(OutputStream out) throws IOException {
		this(out, null, -1, -1, -1, -1, null);
	}

	public TerminalOutput(OutputStream out, String term, int cols, int rows, int width, int height, byte[] modes)
			throws IOException {
		attachStream(out);
		this.term = term;
		this.cols = cols;
		this.rows = rows;
		this.width = width;
		this.height = height;
		this.modes = modes;

		acousticSignalling = true;
		autoflush = true;
		cr = false;

		// set default terminal

		setDefaultTerminal();

	}

	public void setModes(byte[] modes) {
		this.modes = modes;
	}

	public int getRows() {
		return rows;
	}

	public int getColumns() {
		return cols;
	}

	public int getHeight() {

		return height;

	}

	public int getWidth() {

		return width;

	}

	public String getTerm() {

		return term;

	}

	public byte[] getEncodedTerminalModes() {
		return modes;
	}

	public OutputStream getAttachedOutputStream() throws IOException {

		if (out == null) {

			throw new IOException(

			"The terminal is not attached to an OutputStream");

		}

		return out;

	}

	public void detachStream() {
		this.out = null;
	}

	public int getEOL() {
		return eol;
	}

	public String getEOLString() {
		return ((eol == EOL_CR) ? "\r" : "\r\n");
	}

	public void setEOL(int eol) {
		this.eol = eol;
	}

	public void attachStream(OutputStream out) {
		this.out = new DataOutputStream(new BufferedOutputStream(out));
	}

	public int getCols() {
		return cols;
	}

	public void setCols(int cols) {
		this.cols = cols;
	}

	public void setRows(int rows) {
		this.rows = rows;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public void eraseToEndOfLine() throws IOException {
		doErase(EEOL);
	}

	public void eraseToBeginOfLine() throws IOException {
		doErase(EBOL);
	}

	public void eraseLine() throws IOException {

		doErase(EEL);

	}

	public void eraseToEndOfScreen() throws IOException {

		doErase(EEOS);

	}

	public void eraseToBeginOfScreen() throws IOException {

		doErase(EBOS);

	}

	public void eraseScreen() throws IOException {

		doErase(EES);

	}

	private void doErase(int funcConst) throws IOException {

		write(terminal.getEraseSequence(funcConst));

		if (autoflush) {

			flush();

		}

	}

	public int getCursorRow() {

		return cursorRow;

	}

	public int getCursorCol() {

		return cursorCol;

	}

	public void moveCursor(int direction, int times) throws IOException {

		write(terminal.getCursorMoveSequence(direction, times));

		if (autoflush) {

			flush();

		}

	}

	public void moveLeft(int times) throws IOException {

		moveCursor(LEFT, times);

		// Track the cursor to the beginning

		if ((cursorCol - times) > 0) {

			cursorCol -= times;

		}

		else {

			cursorCol = 1;

		}

		/*
		 * System.out.println("moveLeft Row=" + String.valueOf(cursorRow) +
		 * " Col=" + String.valueOf(cursorCol));
		 */

	}

	public void moveRight(int times) throws IOException {

		moveCursor(RIGHT, times);

		// Track the cursor to the end

		if ((cursorCol + times) <= cols) {

			cursorCol += times;

		}

		else {

			cursorCol = cols;

		}

		/*
		 * System.out.println("moveRight Row=" + String.valueOf(cursorRow) +
		 * " Col=" + String.valueOf(cursorCol));
		 */

	}

	public void moveUp(int times) throws IOException {

		moveCursor(UP, times);

		// Track the cursor to the top

		if ((cursorRow - times) > 0) {

			cursorRow -= times;

		}

		else {

			cursorRow = 1;

		}

		/*
		 * System.out.println("moveUp Row=" + String.valueOf(cursorRow) +
		 * " Col=" + String.valueOf(cursorCol));
		 */

	}

	public void moveDown(int times) throws IOException {

		moveCursor(DOWN, times);

		// Track the cursor to the bottom

		if ((cursorRow + times) < rows) {

			cursorRow += times;

		}

		else {

			cursorRow = rows;

		}

		/*
		 * System.out.println("moveDown Row=" + String.valueOf(cursorRow) +
		 * " Col=" + String.valueOf(cursorCol));
		 */

	}

	public void setCursor(int row, int col) throws IOException {

		int[] pos = new int[2];

		pos[0] = row;

		pos[1] = col;

		write(terminal.getCursorPositioningSequence(pos));

		if (autoflush) {

			flush();

		}

		cursorRow = (row > 0) ? row : 1;

		cursorCol = (col > 0) ? col : 1;

		/*
		 * System.out.println("setCursor Row=" + String.valueOf(cursorRow) +
		 * " Col=" + String.valueOf(cursorCol));
		 */

	}

	public void homeCursor() throws IOException {
		write(terminal.getCursorPositioningSequence(HOME));
		if (autoflush) {
			flush();
		}
	}

	public void boxCharacter(int specialSequence) throws IOException {

	}

	public void enableGraphics() throws IOException {
		write(terminal.getSpecialSequence(GRAPHICS_ON));
	}

	public void disableGraphics() throws IOException {
		write(terminal.getSpecialSequence(GRAPHICS_OFF));
	}

	public void storeCursor() throws IOException {
		write(terminal.getSpecialSequence(STORECURSOR));
	}

	public void restoreCursor() throws IOException {
		write(terminal.getSpecialSequence(RESTORECURSOR));
	}

	private void write(byte b) throws IOException {
		if (display) {
			if (out == null) {
				throw new IOException("The terminal is not attached to an outputstream");
			}

			if (eol == EOL_CRLF) {
				if (!cr && (b == 10)) {
					out.write(13);
				}

				// ensure CRLF(\r\n) is written for CR(\r) to adhere
				// to the telnet protocol.
				if (cr && (b != 10)) {
					out.write(10);
				}
				out.write(b);
				if (b == 13) {
					cr = true;
				} else {
					cr = false;
				}
			} else {
				out.write(b);
			}
		}

		if (autoflush) {
			flush();
		}

	}

	private byte[] prepareWriteOperation(byte[] sequence) {
		bout.reset();
		for (int i = 0; i < sequence.length; i++) {
			// Process correct EOL
			if (eol == EOL_CRLF) {
				if (!cr && (sequence[i] == 10)) {
					bout.write(13);
				}

				// ensure CRLF(\r\n) is written for CR(\r) to adhere
				// to the telnet protocol.
				if (cr && (sequence[i] != 10)) {
					bout.write(10);
				}
				bout.write(sequence[i]);
				if (sequence[i] == 13) {
					cr = true;
				} else {
					cr = false;
				}
			} else {
				bout.write(sequence[i]);
			}
		}
		return bout.toByteArray();
	}

	public void setDisplay(boolean display) {
		this.display = display;
	}

	private void write(int i) throws IOException {
		write((byte) i);
	}

	private void write(byte[] sequence) throws IOException {
		if (display) {
			if (out != null) {
				out.write(prepareWriteOperation(sequence));
			}
			if (autoflush) {
				flush();
			}

		}

		/*
		 * for (int z = 0; z < sequence.length; z++) { write(sequence[z]);
		 * 
		 * }
		 */

	}

	public void flush() throws IOException {
		if (out == null) {
			throw new IOException("The terminal is not attached to an outputstream");
		}

		// If were attached then flush, else ignore
		out.flush();
	}

	public void closeOutput() throws IOException {
		if (out == null) {
			throw new IOException("The terminal is not attached to an outputstream");
		}
		out.close();
	}

	public void setSignalling(boolean bool) {
		acousticSignalling = bool;
	}

	public boolean isSignalling() {
		return acousticSignalling;
	}

	public void bell() throws IOException {
		if (acousticSignalling) {
			write(BEL);
		}
		if (autoflush) {
			flush();
		}
	}

	public boolean defineScrollRegion(int topmargin, int bottommargin) throws IOException {
		if (terminal.supportsScrolling()) {
			write(terminal.getScrollMarginsSequence(topmargin, bottommargin));
			flush();
			return true;

		}
		return false;
	}

	public void setForegroundColor(int color) throws IOException {
		if (terminal.supportsSGR()) {
			write(terminal.getGRSequence(FCOLOR, color));
			if (autoflush) {
				flush();
			}
		}

	}

	public void setBackgroundColor(int color) throws IOException {
		if (terminal.supportsSGR()) {
			// this method adds the offset to the fg color by itself
			write(terminal.getGRSequence(BCOLOR, color + 10));
			if (autoflush) {
				flush();
			}
		}
	}

	public void setBold(boolean b) throws IOException {
		if (terminal.supportsSGR()) {
			if (b) {
				write(terminal.getGRSequence(STYLE, BOLD));
			} else {
				write(terminal.getGRSequence(STYLE, BOLD_OFF));
			}
			if (autoflush) {
				flush();
			}
		}
	}

	public void setUnderlined(boolean b) throws IOException {
		if (terminal.supportsSGR()) {
			if (b) {
				write(terminal.getGRSequence(STYLE, UNDERLINED));
			} else {
				write(terminal.getGRSequence(STYLE, UNDERLINED_OFF));
			}
			if (autoflush) {
				flush();
			}
		}

	}

	public void setItalic(boolean b) throws IOException {
		if (terminal.supportsSGR()) {
			if (b) {
				write(terminal.getGRSequence(STYLE, ITALIC));
			} else {
				write(terminal.getGRSequence(STYLE, ITALIC_OFF));
			}

			if (autoflush) {
				flush();
			}
		}

	}

	public void setBlink(boolean b) throws IOException {
		if (terminal.supportsSGR()) {
			if (b) {
				write(terminal.getGRSequence(STYLE, BLINK));
			} else {
				write(terminal.getGRSequence(STYLE, BLINK_OFF));
			}
			if (autoflush) {
				flush();
			}
		}
	}

	public void resetAttributes() throws IOException {
		if (terminal.supportsSGR()) {
			write(terminal.getGRSequence(RESET, 0));
		}
	}

	public boolean isAutoflushing() {
		return autoflush;

	}

	public void setAutoflushing(boolean b) {
		autoflush = b;
	}

	public void close() throws IOException {
		closeOutput();
	}

	public Terminal getTerminal() {
		return terminal;
	}

	// getTerminal

	public void setDefaultTerminal() throws IOException {
		// set the terminal passing the negotiated string
		if (term != null) {
			setTerminal(term);
		}

	}

	public void setTerminal(String terminalName) throws IOException {
		term = terminalName;
		terminal = TerminalOutputFactory.newInstance(terminalName);
		// Terminal is set we init it....
		initTerminal();
	}

	private void initTerminal() throws IOException {
		write(terminal.getInitSequence());
		flush();
	}

}

// class TerminalIO

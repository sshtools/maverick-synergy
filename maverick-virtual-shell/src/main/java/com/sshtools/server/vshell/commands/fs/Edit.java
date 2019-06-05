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
package com.sshtools.server.vshell.commands.fs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.cli.CommandLine;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.logger.Log;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.server.vshell.ShellCommand;
import com.sshtools.server.vshell.VirtualProcess;
import com.sshtools.server.vshell.terminal.Console;
import com.sshtools.server.vshell.terminal.TerminalOutput;

public class Edit extends ShellCommand {
	 
	private TerminalOutput io;
	private Console console;
	private AbstractFile cwd;
	private EditBuffer buffer;
	private int yOffset;
	private int xOffset;
	private int cursorX;
	private int cursorY;
	private boolean quit;
	private String search;
	private boolean foundAtAll;
	private VirtualProcess process;

	public Edit() {
		super("edit", SUBSYSTEM_FILESYSTEM, "[<filename>]");
		setDescription("A simple text editor");
	}

	public void run(CommandLine args, VirtualProcess process) throws IOException, PermissionDeniedException {
		this.process = process;
		console = process.getConsole();
		buffer = new EditBuffer();
		cwd = process.getCurrentDirectory();
		io = process.getTerminal();
		if (args.getArgList().size() != 1) {
			loadFile((String) args.getArgList().get(1));
		}
		loop();
	}

	public void pageUp() throws IOException {
		cursorY -= io.getRows();
		if (cursorY < 0) {
			yOffset += cursorY;
			cursorY = 0;
			if (yOffset < 0) {
				io.bell();
				yOffset = 0;
			}
			redraw();
		}
		checkNewCursorLine();
		setCursor();
	}

	public void pageDown() throws IOException {
		cursorY += io.getRows();
		if (cursorY >= io.getRows()) {
			int diff = io.getRows() - cursorY;
			yOffset -= diff;
			cursorY = io.getRows() - 1;
			if (yOffset + cursorY > buffer.size()) {
				io.bell();
				yOffset = buffer.size() - io.getRows();
				if (yOffset < 0) {
					yOffset = 0;
				}
			}
			redraw();
		}
		checkNewCursorLine();
		setCursor();
	}

	public void cursorUp() throws IOException {
		cursorY--;
		if (cursorY < 0) {
			cursorY = 0;
			if (yOffset == 0) {
				io.bell();
			} else {
				yOffset--;
				redraw();
			}
		}
		checkNewCursorLine();
		setCursor();
	}

	public void cursorDown() throws IOException {
		if (cursorY == buffer.size() - yOffset - 1) {
			io.bell();
		} else {
			cursorY++;
			if (cursorY >= io.getRows()) {
				yOffset += 1;
				cursorY = io.getRows() - 1;
				if (yOffset + cursorY >= buffer.size()) {
					yOffset = buffer.size() - cursorY;
				}
				redraw();
			}
			checkNewCursorLine();
			setCursor();
		}
	}

	public void endOfLine() throws IOException {
		String line = getCursorLine();
		if (cursorX == line.length()) {
			io.bell();
		} else {
			cursorX = line.length();
			if (cursorX >= io.getCols()) {
				cursorX = 0;
				xOffset = 0;
				cursorY++;
			}
			redraw();
			checkNewCursorLine();
			setCursor();
		}
	}

	public void startOfLine() throws IOException {
		if (cursorX == 0) {
			io.bell();
		} else {
			cursorX = 0;
			redraw();
			checkNewCursorLine();
			setCursor();
		}
	}

	protected void checkNewCursorLine() {
		if (cursorY + yOffset >= buffer.size()) {
			cursorY--;
			if (cursorY < 0) {
				cursorY = 0;
				yOffset--;
				if (yOffset < 0) {
					yOffset = 0;
				}
			}
		}
		String newCurrentLine = getCursorLine();
		if (cursorX > newCurrentLine.length()) {
			cursorX = Math.max(0, newCurrentLine.length() - 1);
		}
	}

	public void cursorLeft() throws IOException {
		if (cursorX == 0) {
			io.bell();
		} else {
			cursorX--;
			setCursor();
		}
	}

	public void backspace() throws IOException {
		if (cursorX == 0) {
			io.bell();
		} else {
			String line = getCursorLine();
			line = line.substring(0, cursorX - 1) + line.substring(cursorX);
			buffer.set(yOffset + cursorY, line);
			io.eraseLine();
			io.setCursor(cursorY + 1, 1);
			drawLine(line);
			cursorX--;
			setCursor();
		}
	}

	public void delete() throws IOException {
		String line = getCursorLine();
		if (cursorX == 0 && line.length() == 0) {
			io.bell();
		} else {
			line = line.substring(0, cursorX) + line.substring(cursorX + 1);
			buffer.set(yOffset + cursorY, line);
			io.eraseLine();
			io.setCursor(cursorY + 1, 1);
			drawLine(line);
			setCursor();
		}
	}

	public void deleteLine() throws IOException {
		buffer.remove(cursorY + yOffset);
		redraw();
		checkNewCursorLine();
		setCursor();
	}

	public void cursorRight() throws IOException {
		String line = getCursorLine();
		if (cursorX == line.length() - 1) {
			io.bell();
		} else {
			cursorX++;
			setCursor();
		}
	}

	public void typeString(String str) throws IOException {
		for (char c : str.toCharArray()) {
			typeCharacter(c);
		}
	}

	public void typeCharacter(char ch) throws IOException {
		String line = getCursorLine();
		line = line.substring(0, xOffset + cursorX) + ch + line.substring(xOffset + cursorX);
		cursorX++;
		buffer.set(cursorY + yOffset, line);
		redraw();
		setCursor();
	}

	public void enter() throws IOException {
		String line = getCursorLine();
		String before = line.substring(0, cursorX);
		String after = line.substring(cursorX);
		buffer.set(yOffset + cursorY, before);
		buffer.add(yOffset + cursorY + 1, after);
		cursorY++;
		cursorX = 0;
		xOffset = 0;
		redraw();
	}

	protected String getCursorLine() {
		return buffer.get(yOffset + cursorY);
	}

	protected char getCursorChar() {
		return buffer.get(yOffset + cursorY).charAt(cursorX + xOffset);
	}

	private void setCursor() throws IOException {
		io.setCursor(io.getRows() - 1, io.getCols() - 20);
		io.getAttachedOutputStream().write(((cursorY + yOffset + 1) + "," + (cursorX + xOffset + 1)).getBytes());
		io.flush();
		io.setCursor(cursorY + 1, cursorX + 1);
	}

	private void loop() throws IOException {
		quit = false;
		while (!quit) {
			Object[] binding = console.readBinding();
			if (binding == null) {
				quit = true;
			} else {
				String c = (String) binding[0];
				int key = binding[1] == null ? 0 : ((Short) binding[1]).intValue();
				switch (key) {
				case Console.PREV_CHAR:
					cursorLeft();
					break;
				case Console.NEXT_CHAR:
					cursorRight();
					break;
				case Console.NEXT_HISTORY:
					cursorDown();
					break;
				case Console.PREV_HISTORY:
					cursorUp();
					break;
				case Console.START_OF_HISTORY:
					pageUp();
					break;
				case Console.END_OF_HISTORY:
					pageDown();
					break;
				case Console.DELETE_PREV_CHAR:
					backspace();
					break;
				case Console.DELETE_NEXT_CHAR:
					delete();
					break;
				case Console.CLEAR_LINE:
					deleteLine();
					break;
				case Console.MOVE_TO_BEG:
					startOfLine();
					break;
				case Console.MOVE_TO_END:
					endOfLine();
					break;
				case Console.EXIT:
					quit = true;
					break;
				case Console.SEARCH_NEXT:
					search();
					break;
				case Console.REPEAT_SEARCH_NEXT:
					searchAgain(true);
					break;
				case Console.NEWLINE:
					enter();
					break;
				default:
					if (key < 255) {
						typeString(c);
					}
				}
			}
		}
		if (buffer.isChanged()) {
			while (buffer.isChanged()) {
				io.setCursor(1, 1);
				io.setBackgroundColor(TerminalOutput.BLUE);
				io.setForegroundColor(TerminalOutput.WHITE);
				io.eraseToEndOfLine();
				try {
					if (buffer.getFile() == null) {
						String filename = console.readLine("File to save as: ");
						AbstractFile file = process.getCurrentDirectory().resolveFile(filename);
						buffer.setFile(file);
						buffer.save();
					} else {
						String save = console.readLine("Save Y/N: ");
						if (save.equals("y") || save.equals("Y")) {
							buffer.save();
						} else {
							buffer.discard();
						}
					}
				} catch (Exception e) {
					Log.error("Failed to save. ", e);
					io.setCursor(1, 1);
					console.readLine("Failed to save. " + e.getMessage() + ". Press RETURN");
				}
			}
			io.resetAttributes();
			redraw();
		}
		io.eraseScreen();
		io.setCursor(1, 1);
	}

	public void search() throws IOException {
		io.setCursor(1, 1);
		io.setBackgroundColor(TerminalOutput.BLUE);
		io.setForegroundColor(TerminalOutput.WHITE);
		io.eraseToEndOfLine();
		String newSearch = console.readLine("Search:");
		io.resetAttributes();
		redraw();
		foundAtAll = false;
		search = newSearch;
		searchAgain(true);
	}

	public boolean searchAgain(boolean wrap) throws IOException {
		if (search!=null && !search.equals("")) {
			boolean foundThisTime = false;
			int startOffset = cursorY + yOffset;
			for (int i = startOffset; i < buffer.size(); i++) {
				String line = expandTabs(buffer.get(i));
				int indexOf = line.indexOf(search, i == startOffset ? xOffset + cursorX + 1 : 0);
				if (indexOf != -1) {
					if (!foundAtAll) {
						foundAtAll = true;
					}
					foundThisTime = true;
					yOffset = i - cursorY;
					if (yOffset < 0) {
						yOffset = 0;
						cursorY = i;
					}
					cursorX = indexOf;
					if (xOffset + cursorX > io.getCols()) {
						xOffset += xOffset + cursorX - io.getCols();
					}
					break;
				}
			}
			if (!foundThisTime) {
				if (wrap) {
					// Start search from the top
					yOffset = 0;
					cursorY = 0;
					cursorX = 0;
					xOffset = 0;
					searchAgain(false);
				} else {
					// Not wrapping, so just beep and redraw
					io.bell();
					redraw();
				}
			} else {
				// We found something
				redraw();
				return true;
			}
		} else {
			io.bell();
		}
		
		return false;
	}

	public void redraw() throws IOException {
		io.eraseScreen();
		int i = 1;
		int rowsToPrint = Math.min(buffer.size(), yOffset + console.getTermheight());
		for (String line : buffer.subList(yOffset, rowsToPrint)) {
			io.setCursor(i, 1);
			drawLine(line);
			i++;
		}
		for (; i < console.getTermheight(); i++) {
			io.setCursor(i, 1);
			console.printString("~");
			console.flushConsole();
		}
		setCursor();
	}

	protected void drawLine(String line) throws IOException {
		String expandTabs = expandTabs(line);
		console.printString(expandTabs.substring(xOffset, Math.min(expandTabs.length(), console.getTermwidth())));
		console.flushConsole();
	}

	public String expandTabs(String line) {
		int x = 0;
		StringBuilder bufline = new StringBuilder();
		for (char c : line.toCharArray()) {
			if (c == '\t') {
				int nextTabStop = 8 - (x % 8);
				for (int i = 0; i < nextTabStop; i++) {
					bufline.append(' ');
					x++;
				}
			} else {
				bufline.append(c);
				x++;
			}
		}
		return bufline.toString();
	}

	public void loadFile(String path) throws IOException, PermissionDeniedException {
		yOffset = 0;
		xOffset = 0;
		cursorX = 0;
		cursorY = 0;
		AbstractFile resolveFile = cwd.resolveFile(path);
		if (resolveFile.exists()) {
			buffer.load(resolveFile);
		} else {
			buffer.setFile(resolveFile);
		}
		redraw();
	}

	public class EditBuffer extends ArrayList<String> {

		private static final long serialVersionUID = 1L;
		private boolean changed;
		private AbstractFile file;

		public EditBuffer() {
			add("");
		}

		public AbstractFile getFile() {
			return file;
		}

		public void discard() {
			changed = false;
		}

		@Override
		public boolean add(String e) {
			try {
				return super.add(e);
			} finally {
				changed = true;
			}
		}

		public void setFile(AbstractFile file) {
			this.file = file;
		}

		@Override
		public void add(int index, String element) {
			try {
				super.add(index, element);
			} finally {
				changed = true;
			}
		}

		@Override
		public void clear() {
			try {
				super.clear();
			} finally {
				changed = true;
			}
		}

		@Override
		public boolean addAll(Collection<? extends String> c) {
			try {
				return super.addAll(c);
			} finally {
				changed = true;
			}
		}

		@Override
		public boolean addAll(int index, Collection<? extends String> c) {
			try {
				return super.addAll(index, c);
			} finally {
				changed = true;
			}
		}

		@Override
		protected void removeRange(int fromIndex, int toIndex) {
			try {
				super.removeRange(fromIndex, toIndex);
			} finally {
				changed = true;
			}
		}

		@Override
		public boolean removeAll(Collection<?> c) {
			try {
				return super.removeAll(c);
			} finally {
				changed = true;
			}
		}

		@Override
		public String set(int index, String element) {
			try {
				return super.set(index, element);
			} finally {
				changed = true;
			}
		}

		@Override
		public String remove(int index) {
			try {
				return super.remove(index);
			} finally {
				changed = true;
			}
		}

		@Override
		public boolean remove(Object o) {
			try {
				return super.remove(o);
			} finally {
				changed = true;
			}
		}

		public boolean isChanged() {
			return changed;
		}

		public void save() throws IOException {
			OutputStream out = file.getOutputStream();
			try {
				PrintWriter pw = new PrintWriter(out, true);
				for (String line : this) {
					pw.println(line);
				}
				changed = false;
			} finally {
				out.close();
			}
		}

		public void load(AbstractFile object) throws IOException {
			this.file = object;
			changed = false;
			clear();
			InputStream in = object.getInputStream();
			try {
				BufferedReader r = new BufferedReader(new InputStreamReader(in));
				String line = null;
				while ((line = r.readLine()) != null) {
					add(line);
				}
				changed = false;
				if (size() == 0) {
					add("");
				}
			} finally {
				in.close();
			}
		}

	}
}

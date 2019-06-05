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
package com.sshtools.server.vshell.terminal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;

public class Table extends ArrayList<Row> {

	private static final long serialVersionUID = 1L;

	public enum Alignment {
		LEFT, CENTER, RIGHT
	}

	private Row header = null;
	private Row footer = null;
	private TerminalOutput io;
	private int maxCellWidth = -1;
	private int minCellWidth = -1;
	private boolean border = true;

	public Table(TerminalOutput io) {
		this.io = io;
	}

	public Row getHeader() {
		return header;
	}

	public void setHeader(Row header) {
		this.header = header;
	}

	public Row getFooter() {
		return footer;
	}

	public void setFooter(Row footer) {
		this.footer = footer;
	}

	public int getMaxCellWidth() {
		return maxCellWidth;
	}

	public void setMaxCellWidth(int maxCellWidth) {
		this.maxCellWidth = maxCellWidth;
	}
	
	public int getMinCellWidth() {
		return minCellWidth;
	}
	
	public void setMinCellWidth(int minCellWidth) {
		this.minCellWidth = minCellWidth;
	}
	
	public boolean isBordered() {
		return border;
	}
	
	public void setBordered(boolean border) {
		this.border = border;
	}

	public void render(Console console) throws IOException {
		List<Integer> maxColumnWidths = new ArrayList<Integer>();
		if (header != null) {
			checkRow(-1, maxColumnWidths, header);
		}
		for (int i = 0; i < size(); i++) {
			checkRow(i, maxColumnWidths, get(i));
		}
		if (footer != null) {
			checkRow(-1, maxColumnWidths, footer);
		}

		StringBuilder top = new StringBuilder(drawBoxChar(TerminalOutput.BOX_TOP_LEFT));
		StringBuilder bottom = new StringBuilder(drawBoxChar(TerminalOutput.BOX_BOTTOM_LEFT));
		StringBuilder middle = new StringBuilder(drawBoxChar(TerminalOutput.BOX_MIDDLE_LEFT));
		boolean first = true;
		for (Integer i : maxColumnWidths) {
			if (!first) {
				middle.append(drawBoxChar(TerminalOutput.BOX_MIDDLE_MIDDLE));
			}
			if (!first) {
				top.append(drawBoxChar(TerminalOutput.BOX_TOP_MIDDLE));
			}
			if (!first) {
				bottom.append(drawBoxChar(TerminalOutput.BOX_BOTTOM_MIDDLE));
			}
			first = false;
			middle.append(repeat(drawBoxChar(TerminalOutput.BOX_MIDDLE), i));
			top.append(repeat(drawBoxChar(TerminalOutput.BOX_TOP), i));
			bottom.append(repeat(drawBoxChar(TerminalOutput.BOX_BOTTOM), i));
		}
		middle.append(drawBoxChar(TerminalOutput.BOX_MIDDLE_RIGHT));
		top.append(drawBoxChar(TerminalOutput.BOX_TOP_RIGHT));
		bottom.append(drawBoxChar(TerminalOutput.BOX_BOTTOM_RIGHT));

		if(isBordered())
			console.printStringNewline(top.toString());
		if (header != null) {
			printRow(console, header, maxColumnWidths);
			if(isBordered())
				console.printStringNewline(middle.toString());
		}
		for (Row row : this) {
			printRow(console, row, maxColumnWidths);
		}
		if (footer != null) {
			console.printStringNewline(middle.toString());
			printRow(console, footer, maxColumnWidths);
		}
		
		if(isBordered())
			console.printStringNewline(bottom.toString());

	}

	private void printRow(Console console, Row row, List<Integer> maxColumnWidths) throws IOException {
		int rowLine = 0;
		while (true) {
			String formatRow = formatRow(rowLine, row, maxColumnWidths);
			if (formatRow == null) {
				break;
			}
			if(isBordered())
				console.printString(drawBoxChar(TerminalOutput.BOX_LEFT));
			
			console.printString(formatRow);
			
			if(isBordered())
				console.printString(drawBoxChar(TerminalOutput.BOX_RIGHT));
			
			console.printNewline();
			rowLine++;
		}
	}

	private String drawBoxChar(int boxChar) {
		StringBuilder bui = new StringBuilder();
		byte[] seq = io.getTerminal().getSpecialSequence(TerminalOutput.GRAPHICS_ON);
		if (seq != null) {
			bui.append(new String(seq));
		}
		seq = io.getTerminal().getSpecialSequence(boxChar);
		if (seq == null) {
			bui.append('*');
		} else {
			bui.append(new String(seq));
		}
		seq = io.getTerminal().getSpecialSequence(TerminalOutput.GRAPHICS_OFF);
		if (seq != null) {
			bui.append(new String(seq));
		}
		return bui.toString();
	}

	protected String formatRow(int rowLine, Row row, List<Integer> maxColumnWidths) {
		Iterator<Integer> width = maxColumnWidths.iterator();
		StringBuilder buf = new StringBuilder();
		int empty = 0;
		for (Cell<?> cell : row) {
			String val = renderCell(rowLine, cell);
			if (val.equals("")) {
				empty++;
			}
			int maxw = width.next();
			int repeat = maxw - val.length();
			if (repeat > 0) {
				// Line not big enough
				val += repeat(" ", repeat);
			}
			else if(repeat < 0) {
				// Line too big, buffer for next row
				//String next = val.substring(maxw);
				val = val.substring(0, maxw);
			}
			if(cell.isStrong() || row.isStrong()) {
				byte[] seq = io.getTerminal().getGRSequence(TerminalOutput.STYLE, TerminalOutput.BOLD);
				if(seq != null) {
					buf.append(new String(seq));
				}
			}
			buf.append(val);
			if(cell.isStrong() || row.isStrong()) {
				byte[] seq = io.getTerminal().getGRSequence(TerminalOutput.STYLE, TerminalOutput.BOLD_OFF);
				if(seq != null) {
					buf.append(new String(seq));
				}
			}
			if (width.hasNext() && isBordered()) {
				buf.append(drawBoxChar(TerminalOutput.BOX_CENTER));
			}
		}
		if (empty == row.size()) {
			return null;
		}
		return buf.toString();
	}

	protected String renderCell(int rowLine, Cell<?> cell) {
		String rendered = cell.render();
		String[] rows = rendered.split("\n");
		if (rows.length > rowLine) {
			rendered = rows[rowLine];
			if (maxCellWidth != -1 && rendered.length() > maxCellWidth) {
				rendered = rendered.substring(0, maxCellWidth);
			} else if(minCellWidth != -1 && rendered.length() < minCellWidth) {
				rendered = StringUtils.rightPad(rendered, minCellWidth);
			}
			return rendered;
		} else {
			return "";
		}
	}

	protected int checkCell(int rocwIndex, Cell<?> cell) {
		String rendered = cell.render();
		String[] rows = rendered.split("\n");
		int size = 0;
		for (String cellLine : rows) {
			if (maxCellWidth != -1 && cellLine.length() > maxCellWidth) {
				cellLine = cellLine.substring(0, maxCellWidth);
			}
			size = Math.max(size, cellLine.length());
		}
		if(cell.getMinWidth() != -1 && size < cell.getMinWidth()) {
			size = cell.getMinWidth();
		}
		if(cell.getMaxWidth() != -1 && size > cell.getMaxWidth()) {
			size = cell.getMaxWidth();
		}
		return size;
	}

	private String repeat(String s, int times) {
		StringBuilder bui = new StringBuilder();
		for (int i = 0; i < times; i++) {
			bui.append(s);
		}
		return bui.toString();
	}

	private void checkRow(int rowIndex, List<Integer> maxColumWidths, Row row) {
		while (maxColumWidths.size() < row.size()) {
			maxColumWidths.add(0);
		}
		for (int i = maxColumWidths.size() - 1; i >= 0; i--) {
			int cellWidth = checkCell(rowIndex, row.get(i));
			maxColumWidths.set(i, Math.max(cellWidth, maxColumWidths.get(i)));
		}
	}
}

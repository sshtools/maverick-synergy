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
package jline;

/**
 * A CursorBuffer is a holder for a {@link StringBuffer} that also contains the
 * current cursor position.
 *
 * @author <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 */
public class CursorBuffer {
    public int cursor = 0;

    StringBuffer buffer = new StringBuffer();

    private boolean overtyping = false;

    public int length() {
        return buffer.length();
    }

    public char current() {
        if (cursor <= 0) {
            return 0;
        }

        return buffer.charAt(cursor - 1);
    }

    public boolean clearBuffer() {
        if (buffer.length() == 0) {
            return false;
        }

        buffer.delete(0, buffer.length());
        cursor = 0;
        return true;
    }

    /**
     * Write the specific character into the buffer, setting the cursor position
     * ahead one. The text may overwrite or insert based on the current setting
     * of isOvertyping().
     *
     * @param c
     *            the character to insert
     */
    public void write(final char c) {
        buffer.insert(cursor++, c);
        if (isOvertyping() && cursor < buffer.length()) {
            buffer.deleteCharAt(cursor);
        }
    }

    /**
     * Insert the specified {@link String} into the buffer, setting the cursor
     * to the end of the insertion point.
     *
     * @param str
     *            the String to insert. Must not be null.
     */
    public void write(final String str) {
        if (buffer.length() == 0) {
            buffer.append(str);
        } else {
            buffer.insert(cursor, str);
        }

        cursor += str.length();

        if (isOvertyping() && cursor < buffer.length()) {
            buffer.delete(cursor, (cursor + str.length()));
        }
    }

    public String toString() {
        return buffer.toString();
    }

    public boolean isOvertyping() {
        return overtyping;
    }

    public void setOvertyping(boolean b) {
        overtyping = b;
    }

	public StringBuffer getBuffer() {
		return buffer;
	}

	public void setBuffer(StringBuffer buffer) {
		buffer.setLength(0);
		buffer.append(this.buffer.toString());
		
		this.buffer = buffer;
	}
    
    
}

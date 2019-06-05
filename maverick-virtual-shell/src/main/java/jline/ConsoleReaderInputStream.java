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

import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.Enumeration;

/**
 *  An {@link InputStream} implementation that wraps a {@link ConsoleReader}.
 *  It is useful for setting up the {@link System#in} for a generic
 *  console.
 *  @author  <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 */
public class ConsoleReaderInputStream extends SequenceInputStream {
    private static InputStream systemIn = System.in;

    public static void setIn() throws IOException {
        setIn(new ConsoleReader());
    }

    public static void setIn(final ConsoleReader reader) {
        System.setIn(new ConsoleReaderInputStream(reader));
    }

    /**
     *  Restore the original {@link System#in} input stream.
     */
    public static void restoreIn() {
        System.setIn(systemIn);
    }

    public ConsoleReaderInputStream(final ConsoleReader reader) {
        super(new ConsoleEnumeration(reader));
    }

    private static class ConsoleEnumeration implements Enumeration<InputStream> {
        private final ConsoleReader reader;
        private ConsoleLineInputStream next = null;
        private ConsoleLineInputStream prev = null;

        public ConsoleEnumeration(final ConsoleReader reader) {
            this.reader = reader;
        }

        public InputStream nextElement() {
            if (next != null) {
                InputStream n = next;
                prev = next;
                next = null;

                return n;
            }

            return new ConsoleLineInputStream(reader);
        }

        public boolean hasMoreElements() {
            // the last line was null
            if ((prev != null) && (prev.wasNull == true)) {
                return false;
            }

            if (next == null) {
                next = (ConsoleLineInputStream) nextElement();
            }

            return next != null;
        }
    }

    private static class ConsoleLineInputStream extends InputStream {
        private final ConsoleReader reader;
        private String line = null;
        private int index = 0;
        private boolean eol = false;
        protected boolean wasNull = false;

        public ConsoleLineInputStream(final ConsoleReader reader) {
            this.reader = reader;
        }

        public int read() throws IOException {
            if (eol) {
                return -1;
            }

            if (line == null) {
                line = reader.readLine();
            }

            if (line == null) {
                wasNull = true;
                return -1;
            }

            if (index >= line.length()) {
                eol = true;
                return '\n'; // lines are ended with a newline
            }

            return line.charAt(index++);
        }
    }
}

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
package jline;

import java.io.IOException;

/**
 *  A no-op unsupported terminal.
 *
 *  @author  <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 */
public class UnsupportedTerminal extends Terminal {
    private Thread maskThread = null;

    public void initializeTerminal() {
        // nothing we need to do (or can do) for windows.
    }

    public boolean getEcho() {
        return true;
    }


    public boolean isEchoEnabled() {
        return true;
    }


    public void enableEcho() {
    }


    public void disableEcho() {
    }


    /**
     *  Always returng 80, since we can't access this info on Windows.
     */
    public int getTerminalWidth() {
        return 80;
    }

    /**
     *  Always returng 24, since we can't access this info on Windows.
     */
    public int getTerminalHeight() {
        return 80;
    }

    public boolean isSupported() {
        return false;
    }

    public void beforeReadLine(final ConsoleReader reader, final String prompt,
       final Character mask) {
        if ((mask != null) && (maskThread == null)) {
            final String fullPrompt = "\r" + prompt
                + "                 "
                + "                 "
                + "                 "
                + "\r" + prompt;

            maskThread = new Thread("JLine Mask Thread") {
                public void run() {
                    while (!interrupted()) {
                        try {
                            reader.writer.write(fullPrompt);
                            reader.writer.flush();
                            sleep(3);
                        } catch (IOException ioe) {
                            return;
                        } catch (InterruptedException ie) {
                            return;
                        }
                    }
                }
            };

            maskThread.setPriority(Thread.MAX_PRIORITY);
            maskThread.setDaemon(true);
            maskThread.start();
        }
    }

    public void afterReadLine(final ConsoleReader reader, final String prompt,
        final Character mask) {
        if ((maskThread != null) && maskThread.isAlive()) {
            maskThread.interrupt();
        }

        maskThread = null;
    }
}

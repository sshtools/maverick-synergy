/**
 * (c) 2002-2021 JADAPTIVE Limited. All Rights Reserved.
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
/* HEADER */
package com.sshtools.common.util;

import java.io.IOException;
import java.io.InputStream;

class EOLProcessorInputStream extends InputStream {

    EOLProcessor processor;
    InputStream in;
    DynamicBuffer buf = new DynamicBuffer();
    byte[] tmp = new byte[32768];

    public EOLProcessorInputStream(int inputStyle,
            int outputStyle,
            InputStream in) throws IOException {
        this.in = in;
        processor = new EOLProcessor(inputStyle,
                    outputStyle,
                    buf.getOutputStream());
    }

    /**
     * Reads the next byte of data from the input stream.
     *
     * @return the next byte of data, or <code>-1</code> if the end of the
     *   stream is reached.
     * @throws IOException if an I/O error occurs.
     * @todo Implement this java.io.InputStream method
     */
    public int read() throws IOException {
        fillBuffer(1);
        return buf.getInputStream().read();
    }

    public int available() throws IOException {
        return in.available();
    }

    public int read(byte[] b, int off, int len) throws IOException {
        fillBuffer(len);
        return buf.getInputStream().read(b, off, len);
    }

    private void fillBuffer(int count) throws IOException {

        while(buf.available() < count) {
            int read = in.read(tmp);
            if(read == -1) {
            	processor.close();
                buf.close();
                return;
            }
            processor.processBytes(tmp, 0, read);
        }
    }
    
    public void close() throws IOException {
    	in.close();
    }
}

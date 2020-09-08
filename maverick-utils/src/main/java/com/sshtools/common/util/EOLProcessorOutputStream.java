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
/* HEADER */
package com.sshtools.common.util;

import java.io.IOException;
import java.io.OutputStream;

class EOLProcessorOutputStream extends OutputStream {

    EOLProcessor processor;
    public EOLProcessorOutputStream(int inputStyle,
                                    int outputStyle,
                                    OutputStream out)
       throws IOException {
        processor = new EOLProcessor(inputStyle, outputStyle, out);
    }

    public void write(byte[] buf, int off, int len) throws IOException {
        processor.processBytes(buf, off, len);
    }

    public void write(int b) throws IOException {
        processor.processBytes(new byte[] { (byte)b }, 0, 1);
    }

    public void close() throws IOException {
        processor.close();
    }

}

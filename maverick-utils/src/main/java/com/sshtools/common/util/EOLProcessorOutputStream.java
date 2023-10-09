package com.sshtools.common.util;

/*-
 * #%L
 * Utils
 * %%
 * Copyright (C) 2002 - 2023 JADAPTIVE Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

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

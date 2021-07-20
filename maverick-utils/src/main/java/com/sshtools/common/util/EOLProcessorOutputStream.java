

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

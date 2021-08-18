/*
 *    _           _             _   _
 *   (_) __ _  __| | __ _ _ __ | |_(_)_   _____
 *   | |/ _` |/ _` |/ _` | '_ \| __| \ \ / / _ \
 *   | | (_| | (_| | (_| | |_) | |_| |\ V /  __/
 *  _/ |\__,_|\__,_|\__,_| .__/ \__|_| \_/ \___|
 * |__/                  |_|
 *
 * This file is part of the Maverick Synergy Hotfixes Java SSH API
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Copyright (C) 2002-2021 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
 */


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

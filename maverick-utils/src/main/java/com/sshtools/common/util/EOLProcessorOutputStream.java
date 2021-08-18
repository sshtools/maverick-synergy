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

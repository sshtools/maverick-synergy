package com.sshtools.common.tests;

/*-
 * #%L
 * Base API Tests
 * %%
 * Copyright (C) 2002 - 2025 JADAPTIVE Limited
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

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.Objects;

import com.sshtools.common.logger.Log;
import com.sshtools.common.util.Arrays;
import com.sshtools.common.util.IOUtils;

class RandomSocketClient extends Thread {

    private final SocketChannel ch;
    private Throwable lastError;
    private final RandomSocketServer server;
    private Boolean matchingChecksums = false;
    private final long totalAmount;

    RandomSocketClient(RandomSocketServer server, SocketChannel ch, int index, long totalAmount) {
        super("RandomSocketClient_" + index);
        this.server = server;
        this.ch = ch;
        this.totalAmount = totalAmount;
    }

    public boolean hasError() {
        return !Objects.isNull(lastError);
    }

    public boolean hasMatchingChecksums() {
        return matchingChecksums;
    }

    public void run() {
        Log.info("Server client has started");
        try {
            // Wrap the channel as streams for existing digest logic
            InputStream baseIn = Channels.newInputStream(ch);
            OutputStream baseOut = Channels.newOutputStream(ch);

            DigestInputStream in = new DigestInputStream(baseIn, MessageDigest.getInstance("MD5"));
            DigestOutputStream out = new DigestOutputStream(baseOut, MessageDigest.getInstance("MD5"));

            long total = 0;
            byte[] tmp = new byte[32768];
            while (total < totalAmount) {
                int r = in.read(tmp);
                if (r == -1) break;
                out.write(tmp, 0, r);
                total += r;
            }

            IOUtils.closeStream(in);
            IOUtils.closeStream(out);
            ch.close();

            matchingChecksums = Arrays.areEqual(in.getMessageDigest().digest(), out.getMessageDigest().digest());
        } catch (Throwable e) {
            e.printStackTrace();
            lastError = e;
        } finally {
            Log.info("Server client has completed");
            server.report(this);
        }
    }
}

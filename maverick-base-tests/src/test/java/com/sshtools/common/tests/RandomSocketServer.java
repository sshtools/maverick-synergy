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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import java.net.StandardProtocolFamily;          // INET / UNIX
import java.net.UnixDomainSocketAddress;

class RandomSocketServer extends Thread {

    enum Mode { TCP, UDS }

    private final ServerSocketChannel server;
    private final Mode mode;
    private final int count;
    private final long totalAmount;

    private Path udsPath;                         // Only when mode == UDS
    private Throwable lastError;

    private final List<RandomSocketClient> completed      = new ArrayList<>();
    private final List<RandomSocketClient> fatalErrors    = new ArrayList<>();
    private final List<RandomSocketClient> checksumErrors = new ArrayList<>();

    /**
     * Create a TCP server on an ephemeral port.
     */
    RandomSocketServer(int count, long totalAmount) {
        this(Mode.TCP, null, 0, count, totalAmount);
    }

    /**
     * Create a server in the selected mode.
     * For TCP: host/port (port 0 for ephemeral). Host may be null to bind wildcard.
     * For UDS: provide socketPath (file will be created; removed on JVM exit).
     */
    RandomSocketServer(Mode mode, String hostOrPath, int port, int count, long totalAmount) {
        super("RandomSocketServer");
        this.mode = mode;
        this.count = count;
        this.totalAmount = totalAmount;
        
        try {

	        if (mode == Mode.TCP) {
	            server = ServerSocketChannel.open();
	            server.bind(new InetSocketAddress(hostOrPath == null ? "0.0.0.0" : hostOrPath, port));
	        } else {
	            // UDS
	            server = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
	            if (hostOrPath == null || hostOrPath.isBlank()) {
	                // default to a temp file
	                udsPath = Files.createTempFile("random-socket-", ".sock");
	            } else {
	                udsPath = Path.of(hostOrPath);
	                // ensure no stale socket file
	            }
                try { Files.deleteIfExists(udsPath); } catch (IOException ignored) {}
                Files.createDirectories(udsPath.getParent());
                
	            UnixDomainSocketAddress addr = UnixDomainSocketAddress.of(udsPath);
	            server.bind(addr);
	            // Clean up socket file on exit
	            udsPath.toFile().deleteOnExit();
	        }
	        start();
        }
        catch(IOException ioe) {
        	throw new UncheckedIOException(ioe);
        }
    }

    public int getFinishedCount() {
        return completed.size() + fatalErrors.size() + checksumErrors.size();
    }

    /**
     * Path to the Unix Domain Socket (for UDS mode). null for TCP mode.
     */
    public String getPath() {
        return mode == Mode.UDS ? (udsPath == null ? null : udsPath.toString()) : null;
    }

    /**
     * Port number for TCP mode. -1 for UDS mode.
     */
    public int getPort() {
    	try {
	        if (mode == Mode.UDS) return -1;
	        SocketAddress addr = server.getLocalAddress();
	        if (addr instanceof InetSocketAddress) {
	            return ((InetSocketAddress) addr).getPort();
	        }
	        return -1;
    	}
    	catch(IOException ioe) {
    		throw new UncheckedIOException(ioe);
    	}
    }

    public void report(RandomSocketClient client) {
        if (client.hasError()) {
            fatalErrors.add(client);
        } else if (!client.hasMatchingChecksums()) {
            checksumErrors.add(client);
        } else {
            completed.add(client);
        }
    }

    public boolean isComplete() {
        return getFinishedCount() == count;
    }

    public int getChecksumErrorCount() {
        return checksumErrors.size();
    }

    public int getFatalErrorCount() {
        return fatalErrors.size();
    }

    public void run() {
        try {
            int index = 0;
            while (index < count) {
                SocketChannel ch = server.accept();
                if (ch == null) continue;
                new RandomSocketClient(this, ch, index++, totalAmount).start();
            }
        } catch (Throwable e) {
            e.printStackTrace();
            lastError = e;
        } finally {
            try {
                server.close();
            } catch (IOException ignored) {}
            // Best-effort cleanup for UDS (deleteOnExit already set)
            if (mode == Mode.UDS && udsPath != null) {
                try { Files.deleteIfExists(udsPath); } catch (IOException ignored) {}
            }
        }
    }

	public Throwable getLastError() {
		return lastError;
	}
}

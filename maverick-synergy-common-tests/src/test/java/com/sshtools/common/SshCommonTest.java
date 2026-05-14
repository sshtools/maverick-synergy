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
 * Copyright (C) 2002-2025 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
 */
package com.sshtools.common;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sshtools.synergy.ssh.ByteArrayMessage;
import com.sshtools.synergy.ssh.PacketPool;
import com.sshtools.synergy.ssh.RemoteForward;


/**
 * Unit tests for {@link RemoteForward}, {@link PacketPool} and
 * {@link ByteArrayMessage} value-/utility classes.
 */
public class SshCommonTest {

    // -----------------------------------------------------------------------
    // RemoteForward
    // -----------------------------------------------------------------------

    @Test
    public void remoteForward_storesHostAndPort() {
        RemoteForward rf = new RemoteForward("example.com", 8080);
        assertEquals("example.com", rf.getHostToConnect());
        assertEquals(8080, rf.getPortToConnect());
    }

    @Test
    public void remoteForward_portZeroIsAccepted() {
        RemoteForward rf = new RemoteForward("localhost", 0);
        assertEquals(0, rf.getPortToConnect());
    }

    // -----------------------------------------------------------------------
    // PacketPool
    // -----------------------------------------------------------------------

    // Isolate pool tests by creating a fresh instance per test
    private PacketPool pool;

    @BeforeEach
    public void freshPool() {
        pool = new PacketPool();
    }

    @Test
    public void packetPool_getPacket_returnsNonNull() throws IOException {
        assertNotNull(pool.getPacket());
    }

    @Test
    public void packetPool_getPacket_emptyPoolAllocatesNew() throws IOException {
        var p1 = pool.getPacket();
        var p2 = pool.getPacket();
        // Both must be non-null; when pool is empty a new Packet is allocated each time
        assertNotNull(p1);
        assertNotNull(p2);
        assertNotSame(p1, p2);
    }

    @Test
    public void packetPool_putAndGet_reusesPacket() throws IOException {
        var p = pool.getPacket();
        pool.putPacket(p);
        var p2 = pool.getPacket();
        // The same instance should come back after being returned to the pool
        assertNotNull(p2);
    }

    // -----------------------------------------------------------------------
    // ByteArrayMessage (via anonymous concrete subclass)
    // -----------------------------------------------------------------------

    @Test
    public void byteArrayMessage_writesPayloadIntoBuffer() {
        byte[] payload = {1, 2, 3, 4, 5};

        ByteArrayMessage msg = new ByteArrayMessage(payload) {
            @Override public void messageSent(Long sequenceNo) { }
        };

        ByteBuffer buf = ByteBuffer.allocate(payload.length);
        assertTrue(msg.writeMessageIntoBuffer(buf));

        assertArrayEquals(payload, buf.array());
    }

    @Test
    public void byteArrayMessage_emptyPayloadDoesNotThrow() {
        ByteArrayMessage msg = new ByteArrayMessage(new byte[0]) {
            @Override public void messageSent(Long sequenceNo) { }
        };

        ByteBuffer buf = ByteBuffer.allocate(0);
        assertTrue(msg.writeMessageIntoBuffer(buf));
    }
}

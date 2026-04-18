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
package com.sshtools.synergy.ssh;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.sshtools.common.ssh.SshException;

/**
 * Unit tests for {@link ForwardingManager}.
 * <p>
 * Tests cover pure state operations that don't require a live SSH connection.
 */
@SuppressWarnings("rawtypes")
public class ForwardingManagerTest {

    // ------------------------------------------------------------------
    // Construction
    // ------------------------------------------------------------------

    @Test
    void constructor_producesNonNull() {
        assertNotNull(new ForwardingManager<>());
    }

    // ------------------------------------------------------------------
    // isListening
    // ------------------------------------------------------------------

    @Test
    void isListening_unregisteredPort_returnsFalse() {
        ForwardingManager<?> fm = new ForwardingManager<>();
        assertFalse(fm.isListening(22222));
    }

    @Test
    void isListening_portZero_returnsFalse() {
        assertFalse(new ForwardingManager<>().isListening(0));
    }

    // ------------------------------------------------------------------
    // ForwardingFactory
    // ------------------------------------------------------------------

    @Test
    void getForwardingFactory_initiallyNull() {
        assertNull(new ForwardingManager<>().getForwardingFactory());
    }

    @Test
    @SuppressWarnings("unchecked")
    void setForwardingFactory_storesAndReturns() {
        ForwardingManager fm = new ForwardingManager();
        // ForwardingFactory has one method: createChannelFactory(String, int)
        ForwardingFactory factory = (hostToConnect, portToConnect) -> null;
        fm.setForwardingFactory(factory);
        assertSame(factory, fm.getForwardingFactory());
    }

    // ------------------------------------------------------------------
    // RemoteForwardRequestHandlers
    // ------------------------------------------------------------------

    @Test
    void addRemoteForwardRequestHandler_increasesList() {
        ForwardingManager<?> fm = new ForwardingManager<>();
        fm.addRemoteForwardRequestHandler(makeHandler());
        assertEquals(1, fm.getRemoteForwardRequestHandlers().size());
    }

    @Test
    void removeRemoteForwardRequestHandler_decreasesList() {
        ForwardingManager fm = new ForwardingManager();
        RemoteForwardRequestHandler h = makeHandler();
        fm.addRemoteForwardRequestHandler(h);
        fm.removeRemoteForwardRequestHandler(h);
        assertTrue(fm.getRemoteForwardRequestHandlers().isEmpty());
    }

    @Test
    void getRemoteForwardRequestHandlers_returnsNonNull() {
        assertNotNull(new ForwardingManager<>().getRemoteForwardRequestHandlers());
    }

    @Test
    void addMultipleHandlers_allPresent() {
        ForwardingManager fm = new ForwardingManager();
        RemoteForwardRequestHandler h1 = makeHandler();
        RemoteForwardRequestHandler h2 = makeHandler();
        fm.addRemoteForwardRequestHandler(h1);
        fm.addRemoteForwardRequestHandler(h2);
        List<?> handlers = fm.getRemoteForwardRequestHandlers();
        assertEquals(2, handlers.size());
        assertTrue(handlers.contains(h1));
        assertTrue(handlers.contains(h2));
    }

    @Test
    void removeNonExistentHandler_doesNotThrow() {
        ForwardingManager fm = new ForwardingManager();
        fm.removeRemoteForwardRequestHandler(makeHandler()); // no exception expected
    }

    // ------------------------------------------------------------------
    // helper
    // ------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private static RemoteForwardRequestHandler makeHandler() {
        return new RemoteForwardRequestHandler() {
            @Override
            public boolean isHandled(String hostToBind, int portToBind,
                    String destinationHost, int destinationPort, ConnectionProtocol conn) {
                return false;
            }
            @Override
            public int startRemoteForward(String hostToBind, int portToBind,
                    String destinationHost, int destinationPort, ConnectionProtocol conn)
                    throws SshException {
                return portToBind;
            }
            @Override
            public void stopRemoteForward(String hostToBind, int portToBind,
                    String destinationHost, int destinationPort, ConnectionProtocol conn)
                    throws SshException {
            }
        };
    }
}

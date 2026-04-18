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
package com.sshtools.client.tests;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.sshtools.client.SshClient;

/**
 * Unit tests that verify observable, contractual properties of the
 * {@link SshClient} static constants.
 */
public class SshClientConstantsTest {

    @Test
    public void defaultConnectTimeout_isPositive() {
        assertTrue(SshClient.DEFAULT_CONNECT_TIMEOUT > 0,
            "DEFAULT_CONNECT_TIMEOUT should be a positive value");
    }

    @Test
    public void guestUsername_isNotBlank() {
        assertFalse(SshClient.GUEST_USERNAME == null || SshClient.GUEST_USERNAME.isBlank(),
            "GUEST_USERNAME must not be null or blank");
    }

    /**
     * The default connect timeout must be expressed in milliseconds; a
     * reasonable lower bound is 1 second and an upper bound is 10 minutes.
     */
    @Test
    public void defaultConnectTimeout_isWithinReasonableBounds() {
        long timeout = SshClient.DEFAULT_CONNECT_TIMEOUT;
        assertTrue(timeout >= 1_000,   "DEFAULT_CONNECT_TIMEOUT should be at least 1 second (1000 ms)");
        assertTrue(timeout <= 600_000, "DEFAULT_CONNECT_TIMEOUT should not exceed 10 minutes (600000 ms)");
    }
}

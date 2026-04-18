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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.sshtools.client.SshClient;
import com.sshtools.client.SshClient.SshClientBuilder;

/**
 * Unit tests for {@link SshClient.SshClientBuilder}.
 * <p>
 * Tests cover builder configuration without requiring a network connection.
 * Private fields are inspected via reflection.
 */
public class SshClientBuilderTest {

    // ------------------------------------------------------------------
    // Constant tests
    // ------------------------------------------------------------------

    @Test
    void guestUsername_isNotNull() {
        assertNotNull(SshClient.GUEST_USERNAME);
    }

    @Test
    void guestUsername_defaultIsGuest() {
        String expected = System.getProperty("maverick.guestUsername", "guest");
        assertEquals(expected, SshClient.GUEST_USERNAME);
    }

    @Test
    void defaultConnectTimeout_isPositive() {
        assertTrue(SshClient.DEFAULT_CONNECT_TIMEOUT > 0,
                "Default connect timeout must be positive");
    }

    // ------------------------------------------------------------------
    // Builder factory test
    // ------------------------------------------------------------------

    @Test
    void create_returnsNonNull() {
        assertNotNull(SshClientBuilder.create());
    }

    // ------------------------------------------------------------------
    // Builder method chaining – each setter returns the same builder
    // ------------------------------------------------------------------

    @Test
    void withHostname_returnsSameBuilder() {
        SshClientBuilder b = SshClientBuilder.create();
        assertSame(b, b.withHostname("host"));
    }

    @Test
    void withPort_returnsSameBuilder() {
        SshClientBuilder b = SshClientBuilder.create();
        assertSame(b, b.withPort(22));
    }

    @Test
    void withUsername_returnsSameBuilder() {
        SshClientBuilder b = SshClientBuilder.create();
        assertSame(b, b.withUsername("alice"));
    }

    @Test
    void withConnectTimeout_millis_returnsSameBuilder() {
        SshClientBuilder b = SshClientBuilder.create();
        assertSame(b, b.withConnectTimeout(5000L));
    }

    @Test
    void withConnectTimeout_duration_returnsSameBuilder() {
        SshClientBuilder b = SshClientBuilder.create();
        assertSame(b, b.withConnectTimeout(Duration.ofSeconds(5)));
    }

    @Test
    void withPassword_returnsSameBuilder() {
        SshClientBuilder b = SshClientBuilder.create();
        assertSame(b, b.withPassword("s3cr3t"));
    }

    // ------------------------------------------------------------------
    // Field-level assertions via reflection
    // ------------------------------------------------------------------

    @Test
    void withHostname_storesHostname() throws Exception {
        SshClientBuilder b = SshClientBuilder.create().withHostname("example.com");
        assertEquals(Optional.of("example.com"), getField(b, "hostname"));
    }

    @Test
    void withPort_storesPort() throws Exception {
        SshClientBuilder b = SshClientBuilder.create().withPort(2222);
        assertEquals(Optional.of(2222), getField(b, "port"));
    }

    @Test
    void withUsername_storesUsername() throws Exception {
        SshClientBuilder b = SshClientBuilder.create().withUsername("bob");
        assertEquals(Optional.of("bob"), getField(b, "username"));
    }

    @Test
    void withUsername_emptyString_clearsUsername() throws Exception {
        SshClientBuilder b = SshClientBuilder.create().withUsername("");
        assertEquals(Optional.empty(), getField(b, "username"));
    }

    @Test
    void withConnectTimeout_millis_storesDuration() throws Exception {
        SshClientBuilder b = SshClientBuilder.create().withConnectTimeout(3000L);
        assertEquals(Optional.of(Duration.ofMillis(3000)), getField(b, "connectTimeout"));
    }

    @Test
    void withPassword_addsPasswordAuthenticator() throws Exception {
        SshClientBuilder b = SshClientBuilder.create().withPassword("pw");
        Set<?> auths = getField(b, "authenticators");
        assertEquals(1, auths.size(), "withPassword should add exactly one authenticator");
    }

    @Test
    void withPassword_calledTwice_addsTwoAuthenticators() throws Exception {
        SshClientBuilder b = SshClientBuilder.create()
                .withPassword("pw1")
                .withPassword("pw2");
        Set<?> auths = getField(b, "authenticators");
        assertEquals(2, auths.size(), "Two withPassword calls should add two authenticators");
    }

    @Test
    void withTarget_setsHostnameAndPort() throws Exception {
        SshClientBuilder b = SshClientBuilder.create().withTarget("srv.example.com", 22);
        assertEquals(Optional.of("srv.example.com"), getField(b, "hostname"));
        assertEquals(Optional.of(22), getField(b, "port"));
    }

    @Test
    void twoBuilders_areIndependent() throws Exception {
        SshClientBuilder b1 = SshClientBuilder.create().withHostname("a.example.com");
        SshClientBuilder b2 = SshClientBuilder.create().withHostname("b.example.com");
        assertNotSame(getField(b1, "hostname"), getField(b2, "hostname"),
                "Two separate builders must not share state");
    }

    // ------------------------------------------------------------------
    // helper
    // ------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private static <T> T getField(SshClientBuilder builder, String fieldName) throws Exception {
        Field f = SshClientBuilder.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        return (T) f.get(builder);
    }
}

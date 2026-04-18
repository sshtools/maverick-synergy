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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.sshtools.client.PasswordAuthenticator;

/**
 * Unit tests for {@link PasswordAuthenticator}.
 * <p>
 * These tests cover factory methods and query behaviour without requiring a
 * network connection.
 */
public class PasswordAuthenticatorTest {

    @Test
    public void forPassword_string_returnsCorrectPassword() {
        PasswordAuthenticator auth = PasswordAuthenticator.forPassword("s3cret");
        assertEquals("s3cret", auth.getPassword());
    }

    @Test
    public void forPassword_chars_returnsCorrectPassword() {
        char[] pw = {'p', 'a', 's', 's'};
        PasswordAuthenticator auth = PasswordAuthenticator.forPassword(pw);
        assertEquals("pass", auth.getPassword());
    }

    @Test
    public void of_withPrompt_returnsPromptValue() {
        PasswordAuthenticator auth = PasswordAuthenticator.of(() -> "dynPass");
        assertEquals("dynPass", auth.getPassword());
    }

    @Test
    public void getName_returnsPassword() {
        PasswordAuthenticator auth = PasswordAuthenticator.forPassword("x");
        assertEquals("password", auth.getName());
    }

    @Test
    public void promptCalledEachTime() {
        AtomicInteger callCount = new AtomicInteger(0);
        PasswordAuthenticator auth = PasswordAuthenticator.of(() -> {
            callCount.incrementAndGet();
            return "pw" + callCount.get();
        });

        String first  = auth.getPassword();
        String second = auth.getPassword();

        assertEquals(2, callCount.get(), "Prompt lambda should be invoked on each getPassword() call");
        assertNotEquals(first, second, "Successive calls should reflect fresh prompt invocations");
    }

    @Test
    public void forPassword_notNull() {
        PasswordAuthenticator auth = PasswordAuthenticator.forPassword("any");
        assertNotNull(auth);
    }

    @Test
    public void of_notNull() {
        PasswordAuthenticator auth = PasswordAuthenticator.of(() -> "pw");
        assertNotNull(auth);
    }
}

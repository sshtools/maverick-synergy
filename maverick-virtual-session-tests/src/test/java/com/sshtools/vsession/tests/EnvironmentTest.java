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
package com.sshtools.vsession.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.sshtools.server.vsession.Environment;

/**
 * Unit tests for {@link Environment}.
 */
public class EnvironmentTest {

    // ------------------------------------------------------------------
    // Constants
    // ------------------------------------------------------------------

    @Test
    void envHome_constantEqualsHome() {
        assertEquals("HOME", Environment.ENV_HOME);
    }

    // ------------------------------------------------------------------
    // Default constructor
    // ------------------------------------------------------------------

    @Test
    void defaultConstructor_isEmpty() {
        assertTrue(new Environment().isEmpty());
    }

    // ------------------------------------------------------------------
    // Map operations
    // ------------------------------------------------------------------

    @Test
    void put_then_get_returnsValue() {
        Environment env = new Environment();
        env.put("KEY", "value");
        assertEquals("value", env.get("KEY"));
    }

    @Test
    void get_missingKey_returnsNull() {
        assertNull(new Environment().get("MISSING"));
    }

    @Test
    void size_reflectsEntries() {
        Environment env = new Environment();
        env.put("A", "1");
        env.put("B", "2");
        assertEquals(2, env.size());
    }

    // ------------------------------------------------------------------
    // getOrDefault
    // ------------------------------------------------------------------

    @Test
    void getOrDefault_presentKey_returnsStoredValue() {
        Environment env = new Environment();
        env.put("SHELL", "/bin/bash");
        String result = env.getOrDefault("SHELL", "/bin/sh");
        assertEquals("/bin/bash", result);
    }

    @Test
    void getOrDefault_absentKey_returnsDefault() {
        Environment env = new Environment();
        String result = env.getOrDefault("SHELL", "/bin/sh");
        assertEquals("/bin/sh", result);
    }

    @Test
    void getOrDefault_integerValue_correctlyTyped() {
        Environment env = new Environment();
        env.put("COLUMNS", 80);
        int cols = env.getOrDefault("COLUMNS", 0);
        assertEquals(80, cols);
    }

    // ------------------------------------------------------------------
    // Copy constructor
    // ------------------------------------------------------------------

    @Test
    void copyConstructor_containsAllOriginalEntries() {
        Environment src = new Environment();
        src.put("HOME", "/home/user");
        src.put("PATH", "/usr/bin");

        Environment copy = new Environment(src);
        assertEquals("/home/user", copy.get("HOME"));
        assertEquals("/usr/bin",  copy.get("PATH"));
    }

    @Test
    void copyConstructor_isIndependentOfOriginal() {
        Environment src = new Environment();
        src.put("KEY", "original");

        Environment copy = new Environment(src);
        copy.put("KEY", "modified");

        // Original should be unchanged
        assertEquals("original", src.get("KEY"));
        assertEquals("modified", copy.get("KEY"));
    }

    @Test
    void copyConstructor_addingToOriginalDoesNotAffectCopy() {
        Environment src = new Environment();
        src.put("A", "1");
        Environment copy = new Environment(src);

        src.put("B", "2"); // add to original after copy

        assertTrue(copy.containsKey("A"));
        assertEquals(1, copy.size(), "Copy should not see new entry added to original");
    }
}

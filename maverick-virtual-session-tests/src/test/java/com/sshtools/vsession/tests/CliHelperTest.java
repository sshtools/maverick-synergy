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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.sshtools.server.vsession.CliHelper;
import com.sshtools.server.vsession.UsageException;

/**
 * Unit tests for {@link CliHelper} — a pure static argument-parsing utility.
 */
public class CliHelperTest {

    // -----------------------------------------------------------------------
    // hasShortOption
    // -----------------------------------------------------------------------

    @Test
    public void hasShortOption_presentSingleChar_returnsTrue() {
        assertTrue(CliHelper.hasShortOption(new String[]{"-v"}, 'v'));
    }

    @Test
    public void hasShortOption_absent_returnsFalse() {
        assertFalse(CliHelper.hasShortOption(new String[]{"-x"}, 'v'));
    }

    @Test
    public void hasShortOption_longOptionNotMatched() {
        assertFalse(CliHelper.hasShortOption(new String[]{"--verbose"}, 'v'));
    }

    // -----------------------------------------------------------------------
    // hasLongOption
    // -----------------------------------------------------------------------

    @Test
    public void hasLongOption_present_returnsTrue() {
        assertTrue(CliHelper.hasLongOption(new String[]{"--verbose"}, "verbose"));
    }

    @Test
    public void hasLongOption_absent_returnsFalse() {
        assertFalse(CliHelper.hasLongOption(new String[]{"--output"}, "verbose"));
    }

    @Test
    public void hasLongOption_normalisesMissingDashes() {
        // caller may pass "verbose" without the leading dashes
        assertTrue(CliHelper.hasLongOption(new String[]{"--verbose"}, "verbose"));
    }

    // -----------------------------------------------------------------------
    // hasOption (combined)
    // -----------------------------------------------------------------------

    @Test
    public void hasOption_shortMatches() {
        assertTrue(CliHelper.hasOption(new String[]{"-v"}, 'v', "verbose"));
    }

    @Test
    public void hasOption_longMatches() {
        assertTrue(CliHelper.hasOption(new String[]{"--verbose"}, 'v', "verbose"));
    }

    @Test
    public void hasOption_neitherMatches_returnsFalse() {
        assertFalse(CliHelper.hasOption(new String[]{"-x", "--other"}, 'v', "verbose"));
    }

    // -----------------------------------------------------------------------
    // getShortValue
    // -----------------------------------------------------------------------

    @Test
    public void getShortValue_returnsFollowingArg() throws UsageException {
        String val = CliHelper.getShortValue(new String[]{"-o", "output.txt"}, 'o');
        assertEquals("output.txt", val);
    }

    @Test
    public void getShortValue_noValue_throwsUsageException() {
        assertThrows(UsageException.class,
            () -> CliHelper.getShortValue(new String[]{"-o"}, 'o'));
    }

    @Test
    public void getShortValue_optionAbsent_throwsUsageException() {
        assertThrows(UsageException.class,
            () -> CliHelper.getShortValue(new String[]{"-x", "file"}, 'o'));
    }

    // -----------------------------------------------------------------------
    // getLongValue
    // -----------------------------------------------------------------------

    @Test
    public void getLongValue_returnsFollowingArg() throws UsageException {
        String val = CliHelper.getLongValue(new String[]{"--output", "result.txt"}, "output");
        assertEquals("result.txt", val);
    }

    @Test
    public void getLongValue_noValue_throwsUsageException() {
        assertThrows(UsageException.class,
            () -> CliHelper.getLongValue(new String[]{"--output"}, "output"));
    }

    // -----------------------------------------------------------------------
    // getValue with default
    // -----------------------------------------------------------------------

    @Test
    public void getValue_shortPresent_returnsShortValue() throws UsageException {
        String val = CliHelper.getValue(new String[]{"-n", "42"}, 'n', "number", "0");
        assertEquals("42", val);
    }

    @Test
    public void getValue_longPresent_returnsLongValue() throws UsageException {
        String val = CliHelper.getValue(new String[]{"--number", "99"}, 'n', "number", "0");
        assertEquals("99", val);
    }

    @Test
    public void getValue_neitherPresent_returnsDefault() throws UsageException {
        String val = CliHelper.getValue(new String[]{}, 'n', "number", "7");
        assertEquals("7", val);
    }

    @Test
    public void getValue_neitherPresent_nullDefault_throwsUsageException() {
        assertThrows(UsageException.class,
            () -> CliHelper.getValue(new String[]{}, 'n', "number"));
    }

    // -----------------------------------------------------------------------
    // isOption
    // -----------------------------------------------------------------------

    @Test
    public void isOption_shortPresent_returnsTrue() {
        assertTrue(CliHelper.isOption("-v", "verbose"));
    }

    @Test
    public void isOption_longPresent_returnsTrue() {
        assertTrue(CliHelper.isOption("--verbose", "verbose"));
    }

    @Test
    public void isOption_notPresent_returnsFalse() {
        assertFalse(CliHelper.isOption("-x", "verbose"));
    }
}

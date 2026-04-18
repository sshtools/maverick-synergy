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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.junit.jupiter.api.Test;

import com.sshtools.server.vsession.CommandArgumentsParser;

/**
 * Unit tests for {@link CommandArgumentsParser}.
 */
public class CommandArgumentsParserTest {

    // ------------------------------------------------------------------
    // Null / empty options
    // ------------------------------------------------------------------

    @Test
    void parse_nullOptions_emptyArgs_returnsCommandLine() {
        CommandLine cl = CommandArgumentsParser.parse(null, new String[0], "usage");
        assertTrue(cl.getArgList().isEmpty());
    }

    @Test
    void parse_emptyOptions_emptyArgs_returnsEmptyCommandLine() {
        CommandLine cl = CommandArgumentsParser.parse(new Options(), new String[0], "usage");
        assertTrue(cl.getArgList().isEmpty());
    }

    // ------------------------------------------------------------------
    // Recognized options
    // ------------------------------------------------------------------

    @Test
    void parse_recognizedFlag_commandLineHasOption() {
        Options opts = new Options();
        opts.addOption("v", "verbose", false, "verbose output");

        CommandLine cl = CommandArgumentsParser.parse(opts, new String[]{"-v"}, "usage");
        assertTrue(cl.hasOption("v"));
    }

    @Test
    void parse_recognizedLongFlag_commandLineHasOption() {
        Options opts = new Options();
        opts.addOption("v", "verbose", false, "verbose output");

        CommandLine cl = CommandArgumentsParser.parse(opts, new String[]{"--verbose"}, "usage");
        assertTrue(cl.hasOption("verbose"));
    }

    @Test
    void parse_optionWithArgument_valueAccessible() {
        Options opts = new Options();
        Option hostOpt = Option.builder("H")
                .longOpt("host")
                .hasArg()
                .argName("hostname")
                .build();
        opts.addOption(hostOpt);

        CommandLine cl = CommandArgumentsParser.parse(opts,
                new String[]{"--host", "example.com"}, "usage");
        assertEquals("example.com", cl.getOptionValue("host"));
    }

    @Test
    void parse_extraPositionalArgs_presentInArgList() {
        CommandLine cl = CommandArgumentsParser.parse(
                new Options(), new String[]{"arg1", "arg2"}, "usage");
        assertEquals(2, cl.getArgList().size());
        assertEquals("arg1", cl.getArgList().get(0));
        assertEquals("arg2", cl.getArgList().get(1));
    }

    // ------------------------------------------------------------------
    // Unrecognized options → IllegalArgumentException
    // ------------------------------------------------------------------

    @Test
    void parse_unknownOption_throwsIllegalArgument() {
        Options opts = new Options();
        assertThrows(IllegalArgumentException.class,
                () -> CommandArgumentsParser.parse(opts, new String[]{"--unknown"}, "usage: cmd"),
                "Unknown option should throw IllegalArgumentException");
    }

    @Test
    void parse_unknownOption_exceptionMessageContainsUsage() {
        Options opts = new Options();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> CommandArgumentsParser.parse(opts, new String[]{"--bad"}, "my-usage"));
        assertEquals("my-usage", ex.getMessage());
    }

    // ------------------------------------------------------------------
    // Multiple flags
    // ------------------------------------------------------------------

    @Test
    void parse_multipleFlags_allPresent() {
        Options opts = new Options();
        opts.addOption("a", false, "flag a");
        opts.addOption("b", false, "flag b");

        CommandLine cl = CommandArgumentsParser.parse(opts, new String[]{"-a", "-b"}, "usage");
        assertTrue(cl.hasOption("a"));
        assertTrue(cl.hasOption("b"));
    }
}

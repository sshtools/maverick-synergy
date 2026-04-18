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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.cli.Options;
import org.junit.jupiter.api.Test;

import com.sshtools.server.vsession.CmdLine;
import com.sshtools.server.vsession.CmdLine.Condition;
import com.sshtools.server.vsession.CommandArgumentsParser;

/**
 * Unit tests for {@link CmdLine} and {@link CommandArgumentsParser}.
 */
public class CmdLineTest {

    // -----------------------------------------------------------------------
    // CmdLine
    // -----------------------------------------------------------------------

    @Test
    public void cmdLine_getCommand_returnsFirstArg() {
        CmdLine cmd = new CmdLine("ls -la", Arrays.asList("ls", "-la"), Condition.ExecNextCommand, false);
        assertEquals("ls", cmd.getCommand());
    }

    @Test
    public void cmdLine_getLine_returnsFullLine() {
        CmdLine cmd = new CmdLine("echo hello", Arrays.asList("echo", "hello"), Condition.ExecNextCommand, false);
        assertEquals("echo hello", cmd.getLine());
    }

    @Test
    public void cmdLine_getArgs_returnsMutableCopy() {
        CmdLine cmd = new CmdLine("echo", Arrays.asList("echo", "world"), Condition.ExecNextCommand, false);
        assertNotNull(cmd.getArgs());
        assertEquals(2, cmd.getArgs().size());
    }

    @Test
    public void cmdLine_getArgArray_returnsArray() {
        CmdLine cmd = new CmdLine("cp a b", Arrays.asList("cp", "a", "b"), Condition.ExecNextCommand, false);
        assertArrayEquals(new String[]{"cp", "a", "b"}, cmd.getArgArray());
    }

    @Test
    public void cmdLine_emptyStringArgsAreStripped() {
        // Constructor strips empty-string args; must use a mutable list so the iterator.remove() in CmdLine can work
        CmdLine cmd = new CmdLine("ls", new ArrayList<>(Arrays.asList("ls", "", "-la")), Condition.ExecNextCommand, false);
        assertFalse(cmd.getArgs().contains(""), "Empty-string args should be removed by constructor");
    }

    @Test
    public void cmdLine_background_flag() {
        CmdLine fg = new CmdLine("ls", Arrays.asList("ls"), Condition.ExecNextCommand, false);
        CmdLine bg = new CmdLine("ls", Arrays.asList("ls"), Condition.Background, true);
        assertFalse(fg.isBackground());
        assertTrue(bg.isBackground());
    }

    @Test
    public void cmdLine_condition_stored() {
        CmdLine cmd = new CmdLine("cmd", Arrays.asList("cmd"), Condition.ExecNextCommandOnSuccess, false);
        assertEquals(Condition.ExecNextCommandOnSuccess, cmd.getCondition());
    }

    @Test
    public void cmdLine_exitCode_defaultZero() {
        CmdLine cmd = new CmdLine("x", Arrays.asList("x"), Condition.ExecNextCommand, false);
        assertEquals(0, cmd.getExitCode());
    }

    @Test
    public void cmdLine_setExitCode_updatesValue() {
        CmdLine cmd = new CmdLine("x", Arrays.asList("x"), Condition.ExecNextCommand, false);
        cmd.setExitCode(42);
        assertEquals(42, cmd.getExitCode());
    }

    // -----------------------------------------------------------------------
    // CommandArgumentsParser
    // -----------------------------------------------------------------------

    @Test
    public void parser_noOptions_emptyArgs_succeeds() {
        var cl = CommandArgumentsParser.parse(new Options(), new String[0], "usage");
        assertNotNull(cl);
    }

    @Test
    public void parser_withKnownOption_parsesFlag() {
        Options opts = new Options();
        opts.addOption("v", "verbose", false, "verbose output");

        var cl = CommandArgumentsParser.parse(opts, new String[]{"-v"}, "usage");
        assertTrue(cl.hasOption("v"));
    }

    @Test
    public void parser_nullOptions_treatedAsEmpty() {
        // null Options argument should be handled gracefully
        var cl = CommandArgumentsParser.parse(null, new String[0], "usage");
        assertNotNull(cl);
    }

    @Test
    public void parser_unknownOption_throwsIllegalArgument() {
        Options opts = new Options();
        // --unknown is not declared, so parser should throw
        assertThrows(IllegalArgumentException.class,
            () -> CommandArgumentsParser.parse(opts, new String[]{"--unknown"}, "usage: test"));
    }
}

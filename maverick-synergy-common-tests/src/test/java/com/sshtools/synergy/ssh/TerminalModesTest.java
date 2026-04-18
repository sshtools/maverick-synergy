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
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.sshtools.synergy.ssh.TerminalModes.Mode;
import com.sshtools.synergy.ssh.TerminalModes.TerminalModesBuilder;

/**
 * Unit tests for {@link TerminalModes} and {@link TerminalModesBuilder}.
 */
public class TerminalModesTest {

    // ------------------------------------------------------------------
    // Builder creation
    // ------------------------------------------------------------------

    @Test
    void create_returnsNonNull() {
        assertNotNull(TerminalModesBuilder.create());
    }

    @Test
    void build_emptyBuilder_producesEmptyModes() {
        TerminalModes modes = TerminalModesBuilder.create().build();
        assertTrue(modes.modes().isEmpty(), "Empty builder should produce no modes");
    }

    // ------------------------------------------------------------------
    // withMode(Mode, boolean)
    // ------------------------------------------------------------------

    @Test
    void withMode_booleanTrue_isReflectedInIs() {
        TerminalModes modes = TerminalModesBuilder.create()
                .withMode(Mode.ECHO, true)
                .build();
        assertTrue(modes.is(Mode.ECHO));
    }

    @Test
    void withMode_booleanFalse_isReturnsFalse() {
        TerminalModes modes = TerminalModesBuilder.create()
                .withMode(Mode.ECHO, false)
                .build();
        assertFalse(modes.is(Mode.ECHO));
    }

    @Test
    void withMode_booleanFalse_modeIsPresentButFalse() {
        TerminalModes modes = TerminalModesBuilder.create()
                .withMode(Mode.ECHO, false)
                .build();
        assertTrue(modes.present(Mode.ECHO), "Mode set to false should still be present");
        assertFalse(modes.is(Mode.ECHO));
    }

    // ------------------------------------------------------------------
    // withMode(Mode, int)
    // ------------------------------------------------------------------

    @Test
    void withMode_intValue_getReturnsValue() {
        TerminalModes modes = TerminalModesBuilder.create()
                .withMode(Mode.TTY_OP_OSPEED, 38400)
                .build();
        assertEquals(38400, modes.get(Mode.TTY_OP_OSPEED));
    }

    @Test
    void get_defaultForAbsentMode_returnsZero() {
        TerminalModes modes = TerminalModesBuilder.create().build();
        assertEquals(0, modes.get(Mode.TTY_OP_OSPEED));
    }

    @Test
    void get_defaultOverride_returnsSuppliedDefault() {
        TerminalModes modes = TerminalModesBuilder.create().build();
        assertEquals(9600, modes.get(Mode.TTY_OP_ISPEED, 9600));
    }

    // ------------------------------------------------------------------
    // withModes / withoutModes
    // ------------------------------------------------------------------

    @Test
    void withModes_setsMultipleModesToTrue() {
        TerminalModes modes = TerminalModesBuilder.create()
                .withModes(Mode.ECHO, Mode.INLCR)
                .build();
        assertTrue(modes.is(Mode.ECHO));
        assertTrue(modes.is(Mode.INLCR));
    }

    @Test
    void withoutModes_setsMultipleModesToFalse() {
        TerminalModes modes = TerminalModesBuilder.create()
                .withModes(Mode.ECHO, Mode.INLCR)
                .withoutModes(Mode.ECHO, Mode.INLCR)
                .build();
        assertFalse(modes.is(Mode.ECHO));
        assertFalse(modes.is(Mode.INLCR));
        // modes are still present (just set to 0)
        assertTrue(modes.present(Mode.ECHO));
    }

    // ------------------------------------------------------------------
    // present() vs is()
    // ------------------------------------------------------------------

    @Test
    void present_absentMode_returnsFalse() {
        TerminalModes modes = TerminalModesBuilder.create().build();
        assertFalse(modes.present(Mode.ECHO));
    }

    @Test
    void is_absentMode_returnsDefaultFalse() {
        TerminalModes modes = TerminalModesBuilder.create().build();
        assertFalse(modes.is(Mode.ECHO));
    }

    @Test
    void is_absentMode_withDefaultTrue_returnsTrue() {
        TerminalModes modes = TerminalModesBuilder.create().build();
        assertTrue(modes.is(Mode.ECHO, true));
    }

    // ------------------------------------------------------------------
    // reset()
    // ------------------------------------------------------------------

    @Test
    void reset_clearsAllModes() {
        TerminalModesBuilder builder = TerminalModesBuilder.create()
                .withModes(Mode.ECHO, Mode.INLCR);
        builder.reset();
        TerminalModes modes = builder.build();
        assertTrue(modes.modes().isEmpty(), "After reset, modes map should be empty");
    }

    // ------------------------------------------------------------------
    // toByteArray() / fromBytes() round-trip
    // ------------------------------------------------------------------

    @Test
    void toByteArray_fromBytes_roundTrip() {
        TerminalModes original = TerminalModesBuilder.create()
                .withMode(Mode.ECHO, true)
                .withMode(Mode.TTY_OP_OSPEED, 115200)
                .withMode(Mode.INLCR, false)
                .build();

        byte[] bytes = original.toByteArray();
        TerminalModes restored = TerminalModesBuilder.create().fromBytes(bytes).build();

        assertEquals(original.get(Mode.ECHO),           restored.get(Mode.ECHO));
        assertEquals(original.get(Mode.TTY_OP_OSPEED),  restored.get(Mode.TTY_OP_OSPEED));
        assertEquals(original.get(Mode.INLCR),          restored.get(Mode.INLCR));
    }

    @Test
    void toByteArray_notEmpty_forNonEmptyModes() {
        byte[] bytes = TerminalModesBuilder.create()
                .withMode(Mode.ECHO, true)
                .build()
                .toByteArray();
        assertTrue(bytes.length > 0);
    }

    // ------------------------------------------------------------------
    // modes() map is unmodifiable
    // ------------------------------------------------------------------

    @Test
    void modes_mapIsUnmodifiable() {
        TerminalModes modes = TerminalModesBuilder.create()
                .withMode(Mode.ECHO, true)
                .build();
        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class,
                () -> modes.modes().put(Mode.INLCR, 1));
    }
}

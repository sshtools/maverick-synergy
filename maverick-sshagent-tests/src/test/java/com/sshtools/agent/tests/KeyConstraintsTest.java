package com.sshtools.agent.tests;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.sshtools.agent.KeyConstraints;

/**
 * Unit tests for {@link KeyConstraints}.
 */
public class KeyConstraintsTest {

    // ---------------------------------------------------------------
    // Default constraints
    // ---------------------------------------------------------------

    @Test
    void defaultConstraints_canUse_returnsTrue() {
        KeyConstraints cs = new KeyConstraints();
        assertTrue(cs.canUse(), "Default constraints should allow usage");
    }

    @Test
    void defaultConstraints_hasTimedOut_returnsFalse() {
        KeyConstraints cs = new KeyConstraints();
        assertFalse(cs.hasTimedOut(), "Default constraints should not be timed out");
    }

    @Test
    void defaultConstraints_forwardingPath_isEmpty() {
        KeyConstraints cs = new KeyConstraints();
        assertNotNull(cs.getForwardingPath());
        assertTrue(cs.getForwardingPath().isEmpty(),
                "Default forwarding path should be empty");
    }

    // ---------------------------------------------------------------
    // use() decrements available count
    // ---------------------------------------------------------------

    @Test
    void useLimit_afterExhaustingLimit_canUseReturnsFalse() {
        KeyConstraints cs = new KeyConstraints();
        // Set use limit to 1 via the setter
        cs.setKeyUseLimit(1);
        assertTrue(cs.canUse(), "Should be usable before first use");
        cs.use();
        assertFalse(cs.canUse(), "Should not be usable after exhausting use limit");
    }

    @Test
    void useLimit_zeroMeansUnlimited() {
        // A use limit of 0 is the NO_LIMIT sentinel: canUse() must return true.
        KeyConstraints cs = new KeyConstraints();
        cs.setKeyUseLimit(0); // NO_LIMIT
        for (int i = 0; i < 1000; i++) {
            cs.use();
        }
        assertTrue(cs.canUse(), "Use limit of 0 (NO_LIMIT) should never block usage");
    }

    // ---------------------------------------------------------------
    // Constants
    // ---------------------------------------------------------------

    @Test
    void constants_noTimeout_isZero() {
        assertEquals(0L, KeyConstraints.NO_TIMEOUT);
    }

    @Test
    void constants_noLimit_isUnsignedIntMax() {
        assertEquals(0xffffffffL, KeyConstraints.NO_LIMIT);
    }
}

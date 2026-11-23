package com.sshtools.common.publickey;

/*-
 * #%L
 * Base API Tests
 * %%
 * Copyright (C) 2002 - 2026 JADAPTIVE Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.Test;

public class OpenSSHPrivateKeyFileBoundsTest {

    @Test
    public void acceptsReasonableBcryptRounds() throws Exception {
        assertEquals(16, OpenSSHPrivateKeyFile.validateBcryptKdfRounds(16));
        assertEquals(OpenSSHPrivateKeyFile.MAX_BCRYPT_KDF_ROUNDS,
                OpenSSHPrivateKeyFile.validateBcryptKdfRounds(
                        OpenSSHPrivateKeyFile.MAX_BCRYPT_KDF_ROUNDS));
    }

    @Test
    public void rejectsUnboundedBcryptRounds() throws Exception {
        assertRejected(0);
        assertRejected(OpenSSHPrivateKeyFile.MAX_BCRYPT_KDF_ROUNDS + 1L);
        assertRejected(Integer.MAX_VALUE);
        assertRejected(0xffffffffL);
    }

    private static void assertRejected(long rounds) throws Exception {
        try {
            OpenSSHPrivateKeyFile.validateBcryptKdfRounds(rounds);
            fail("Expected bcrypt KDF rounds to be rejected: " + rounds);
        } catch (IOException expected) {
            assertEquals(true, expected.getMessage().contains("rounds"));
        }
    }
}

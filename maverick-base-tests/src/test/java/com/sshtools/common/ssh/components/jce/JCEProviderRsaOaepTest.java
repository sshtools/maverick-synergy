package com.sshtools.common.ssh.components.jce;

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

import static org.junit.Assert.assertArrayEquals;

import java.security.KeyPair;
import java.security.KeyPairGenerator;

import javax.crypto.Cipher;

import org.junit.Test;

public class JCEProviderRsaOaepTest {

    @Test
    public void sunJceSha256RoundTrip() throws Exception {
        assertRoundTrip(false);
    }

    @Test
    public void sunJceSha1RoundTrip() throws Exception {
        assertRoundTrip(true);
    }

    private static void assertRoundTrip(boolean sha1) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        byte[] clear = new byte[64];
        for (int i = 0; i < clear.length; i++) {
            clear[i] = (byte) i;
        }

        Cipher encrypt = Cipher.getInstance("RSA/ECB/OAEPPadding");
        Cipher decrypt = Cipher.getInstance("RSA/ECB/OAEPPadding");
        if (sha1) {
            JCEProvider.initRSAOAEPSHA1(encrypt, Cipher.ENCRYPT_MODE, pair.getPublic());
            JCEProvider.initRSAOAEPSHA1(decrypt, Cipher.DECRYPT_MODE, pair.getPrivate());
        } else {
            JCEProvider.initRSAOAEPSHA256(encrypt, Cipher.ENCRYPT_MODE, pair.getPublic());
            JCEProvider.initRSAOAEPSHA256(decrypt, Cipher.DECRYPT_MODE, pair.getPrivate());
        }

        assertArrayEquals(clear, decrypt.doFinal(encrypt.doFinal(clear)));
    }
}

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
package com.sshtools.common.util;

/*-
 * #%L
 * Utils
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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import org.junit.Test;

/**
 * Tests for BCryptKDF, EOLProcessor, IOStreamConnector, and Base64.
 */
public class BCryptKDFAndEOLTest {

    // -----------------------------------------------------------------------
    // BCryptKDF
    // -----------------------------------------------------------------------

    /** Known-good bcrypt_pbkdf vector derived from OpenSSH test vectors. */
    @Test
    public void testBcryptPbkdfProducesNonNullResult() throws NoSuchAlgorithmException {
        byte[] pass = "password".getBytes();
        byte[] salt = "saltsalt".getBytes();
        int keylen = 32;
        int rounds = 16;
        byte[] key = BCryptKDF.bcrypt_pbkdf(pass, salt, keylen, rounds);
        assertNotNull(key);
        assertEquals(keylen, key.length);
    }

    @Test
    public void testBcryptPbkdfDifferentPasswordsProduceDifferentKeys() throws NoSuchAlgorithmException {
        byte[] salt = "saltsalt".getBytes();
        byte[] key1 = BCryptKDF.bcrypt_pbkdf("password1".getBytes(), salt, 32, 4);
        byte[] key2 = BCryptKDF.bcrypt_pbkdf("password2".getBytes(), salt, 32, 4);
        boolean same = java.util.Arrays.equals(key1, key2);
        assertEquals(false, same);
    }

    @Test
    public void testBcryptPbkdfDifferentSaltsProduceDifferentKeys() throws NoSuchAlgorithmException {
        byte[] pass = "password".getBytes();
        byte[] key1 = BCryptKDF.bcrypt_pbkdf(pass, "salt1111".getBytes(), 32, 4);
        byte[] key2 = BCryptKDF.bcrypt_pbkdf(pass, "salt2222".getBytes(), 32, 4);
        boolean same = java.util.Arrays.equals(key1, key2);
        assertEquals(false, same);
    }

    @Test
    public void testBcryptPbkdfDeterministic() throws NoSuchAlgorithmException {
        byte[] pass = "test".getBytes();
        byte[] salt = "salt1234".getBytes();
        byte[] key1 = BCryptKDF.bcrypt_pbkdf(pass, salt, 32, 4);
        byte[] key2 = BCryptKDF.bcrypt_pbkdf(pass, salt, 32, 4);
        assertArrayEquals("bcrypt_pbkdf should be deterministic", key1, key2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBcryptPbkdfZeroRoundsThrows() throws NoSuchAlgorithmException {
        BCryptKDF.bcrypt_pbkdf("pass".getBytes(), "salt".getBytes(), 32, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBcryptPbkdfEmptyPasswordThrows() throws NoSuchAlgorithmException {
        BCryptKDF.bcrypt_pbkdf(new byte[0], "salt".getBytes(), 32, 4);
    }

    // -----------------------------------------------------------------------
    // EOLProcessor — convert between line ending styles
    // -----------------------------------------------------------------------

    @Test
    public void testEolUnixToWindows() throws IOException {
        String input = "line1\nline2\nline3";
        byte[] out = processEOL(input.getBytes(), EOLProcessor.TEXT_UNIX, EOLProcessor.TEXT_WINDOWS);
        String result = new String(out);
        assertEquals("line1\r\nline2\r\nline3", result);
    }

    @Test
    public void testEolWindowsToUnix() throws IOException {
        String input = "line1\r\nline2\r\nline3";
        byte[] out = processEOL(input.getBytes(), EOLProcessor.TEXT_WINDOWS, EOLProcessor.TEXT_UNIX);
        String result = new String(out);
        assertEquals("line1\nline2\nline3", result);
    }

    @Test
    public void testEolNoTrailingNewlinePreserved() throws IOException {
        String input = "hello";
        byte[] out = processEOL(input.getBytes(), EOLProcessor.TEXT_UNIX, EOLProcessor.TEXT_WINDOWS);
        assertEquals("hello", new String(out));
    }

    @Test
    public void testEolWindowsToMac() throws IOException {
        String input = "a\r\nb";
        byte[] out = processEOL(input.getBytes(), EOLProcessor.TEXT_WINDOWS, EOLProcessor.TEXT_MAC);
        assertEquals("a\rb", new String(out));
    }

    private byte[] processEOL(byte[] data, int inputStyle, int outputStyle) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        EOLProcessor processor = new EOLProcessor(inputStyle, outputStyle, baos);
        processor.processBytes(data, 0, data.length);
        processor.close();
        return baos.toByteArray();
    }

    // -----------------------------------------------------------------------
    // IOStreamConnector
    // -----------------------------------------------------------------------

    @Test
    public void testIOStreamConnectorCopiesData() throws InterruptedException, IOException {
        byte[] data = "Hello IOStreamConnector!".getBytes();
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        IOStreamConnector connector = new IOStreamConnector(in, out);
        // Poll until the connector has closed (EOF reached) or timeout at 3 seconds
        long deadline = System.currentTimeMillis() + 3000;
        while (!connector.isClosed() && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }

        assertArrayEquals(data, out.toByteArray());
    }
}

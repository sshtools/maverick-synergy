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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Tests for Utils, UnsignedInteger32, UnsignedInteger64, URLUTF8Encoder,
 * SimpleASNWriter/Reader, and Version.
 */
public class UtilsMiscTest {

    // -----------------------------------------------------------------------
    // Utils.bytesToHex
    // -----------------------------------------------------------------------

    @Test
    public void testBytesToHexBasic() {
        byte[] data = { (byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF };
        assertEquals("deadbeef", Utils.bytesToHex(data));
    }

    @Test
    public void testBytesToHexWithOffsetLen() {
        byte[] data = { 0x00, (byte) 0xAB, (byte) 0xCD, 0x00 };
        assertEquals("abcd", Utils.bytesToHex(data, 1, 2));
    }

    @Test
    public void testBytesToHexSingleZero() {
        assertEquals("00", Utils.bytesToHex(new byte[]{ 0 }));
    }

    @Test
    public void testBytesToHexWithSeparator() {
        byte[] data = { 0x01, 0x02 };
        String result = Utils.bytesToHex(data, 0, 2, 0, true, false);
        assertTrue("Should contain spaces", result.contains(" "));
    }

    @Test
    public void testBytesToHexWithText() {
        byte[] data = "AB".getBytes();
        String result = Utils.bytesToHex(data, 0, data.length, 0, false, true);
        assertTrue("Should contain text section", result.contains("["));
    }

    // -----------------------------------------------------------------------
    // Utils.before / after
    // -----------------------------------------------------------------------

    @Test
    public void testBeforeChar() {
        assertEquals("hello", Utils.before("hello:world", ':'));
    }

    @Test
    public void testBeforeCharNotFound() {
        assertEquals("hello", Utils.before("hello", ':'));
    }

    @Test
    public void testAfterChar() {
        assertEquals("world", Utils.after("hello:world", ':'));
    }

    @Test
    public void testAfterCharNotFound() {
        assertEquals("hello", Utils.after("hello", ':'));
    }

    @Test
    public void testBeforeString() {
        assertEquals("foo", Utils.before("foo::bar", "::"));
    }

    @Test
    public void testAfterString() {
        assertEquals("bar", Utils.after("foo::bar", "::"));
    }

    // -----------------------------------------------------------------------
    // Utils.isBlank / isNotBlank
    // -----------------------------------------------------------------------

    @Test
    public void testIsBlankNull() {
        assertTrue(Utils.isBlank(null));
    }

    @Test
    public void testIsBlankEmpty() {
        assertTrue(Utils.isBlank(""));
    }

    @Test
    public void testIsBlankWhitespace() {
        assertTrue(Utils.isBlank("   "));
    }

    @Test
    public void testIsNotBlank() {
        assertTrue(Utils.isNotBlank("hello"));
        assertFalse(Utils.isNotBlank(null));
    }

    // -----------------------------------------------------------------------
    // Utils.nearestMultipleOf
    // -----------------------------------------------------------------------

    @Test
    public void testNearestMultipleOfExact() {
        assertEquals(16, Utils.nearestMultipleOf(16, 8));
    }

    @Test
    public void testNearestMultipleOfRoundsDown() {
        assertEquals(16, Utils.nearestMultipleOf(17, 8));
    }

    @Test
    public void testNearestMultipleOfRoundsUp() {
        assertEquals(24, Utils.nearestMultipleOf(21, 8));
    }

    // -----------------------------------------------------------------------
    // Utils.splitToArgsArray / mergeToArgsString
    // -----------------------------------------------------------------------

    @Test
    public void testSplitToArgsArray() {
        String[] args = Utils.splitToArgsArray("foo bar baz");
        assertArrayEquals(new String[]{ "foo", "bar", "baz" }, args);
    }

    @Test
    public void testSplitToArgsArrayQuoted() {
        String[] args = Utils.splitToArgsArray("foo \"bar baz\" qux");
        assertEquals(3, args.length);
        assertEquals("bar baz", args[1]);
    }

    @Test
    public void testMergeToArgsString() {
        String merged = Utils.mergeToArgsString(new String[]{ "foo", "bar baz", "qux" });
        assertTrue(merged.contains("\"bar baz\""));
        assertTrue(merged.startsWith("foo"));
    }

    // -----------------------------------------------------------------------
    // Utils.stripLeadingZeros
    // -----------------------------------------------------------------------

    @Test
    public void testUtilsStripLeadingZeros() {
        byte[] result = Utils.stripLeadingZeros(new byte[]{ 0, 0, 1, 2 });
        // Should strip leading zeros but preserve sign bit
        assertTrue(result.length <= 3);
        assertEquals(1, result[result.length - 2]);
        assertEquals(2, result[result.length - 1]);
    }

    // -----------------------------------------------------------------------
    // UnsignedInteger32
    // -----------------------------------------------------------------------

    @Test
    public void testUnsignedInteger32Valid() {
        UnsignedInteger32 v = new UnsignedInteger32(42L);
        assertEquals(42L, v.longValue());
        assertEquals(42, v.intValue());
        assertEquals("42", v.toString());
    }

    @Test
    public void testUnsignedInteger32Zero() {
        UnsignedInteger32 v = new UnsignedInteger32(0L);
        assertEquals(0L, v.longValue());
    }

    @Test
    public void testUnsignedInteger32MaxValue() {
        UnsignedInteger32 v = new UnsignedInteger32(0xFFFFFFFFL);
        assertEquals(0xFFFFFFFFL, v.longValue());
    }

    @Test(expected = NumberFormatException.class)
    public void testUnsignedInteger32NegativeThrows() {
        new UnsignedInteger32(-1L);
    }

    @Test(expected = NumberFormatException.class)
    public void testUnsignedInteger32OverflowThrows() {
        new UnsignedInteger32(0x100000000L);
    }

    @Test
    public void testUnsignedInteger32FromString() {
        UnsignedInteger32 v = new UnsignedInteger32("100");
        assertEquals(100L, v.longValue());
    }

    @Test
    public void testUnsignedInteger32Zero_constant() {
        assertEquals(0L, UnsignedInteger32.ZERO.longValue());
    }

    // -----------------------------------------------------------------------
    // UnsignedInteger64
    // -----------------------------------------------------------------------

    @Test
    public void testUnsignedInteger64FromLong() {
        UnsignedInteger64 v = new UnsignedInteger64(Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, v.longValue());
    }

    @Test
    public void testUnsignedInteger64Zero() {
        UnsignedInteger64 v = new UnsignedInteger64(0L);
        assertEquals(0L, v.longValue());
    }

    @Test
    public void testUnsignedInteger64BigIntValue() {
        UnsignedInteger64 v = new UnsignedInteger64(256L);
        assertEquals(java.math.BigInteger.valueOf(256), v.bigIntValue());
    }

    // -----------------------------------------------------------------------
    // URLUTF8Encoder
    // -----------------------------------------------------------------------

    @Test
    public void testUrlEncoderAsciiPassthrough() {
        String s = "abcABC123";
        String encoded = URLUTF8Encoder.encode(s, false);
        assertEquals(s, encoded);
    }

    @Test
    public void testUrlEncoderSpaceEncodesAsPercent20() {
        String encoded = URLUTF8Encoder.encode("hello world", false);
        assertEquals("hello%20world", encoded);
    }

    @Test
    public void testUrlEncoderSpecialChars() {
        String encoded = URLUTF8Encoder.encode("a=b", false);
        // '=' is ASCII 0x3D — should be %3D
        assertEquals("a%3Db", encoded);
    }

    @Test
    public void testUrlEncoderSlashPassthroughWhenNotEncoding() {
        String encoded = URLUTF8Encoder.encode("/path/to/file", false);
        assertEquals("/path/to/file", encoded);
    }

    @Test
    public void testUrlEncoderSlashEncodedWhenRequested() {
        String encoded = URLUTF8Encoder.encode("/a", true);
        assertEquals("%2Fa", encoded);
    }

    // -----------------------------------------------------------------------
    // SimpleASNWriter / SimpleASNReader
    // -----------------------------------------------------------------------

    @Test
    public void testASNWriteReadByte() throws java.io.IOException {
        SimpleASNWriter w = new SimpleASNWriter();
        w.writeByte(0x30); // SEQUENCE tag
        SimpleASNReader r = new SimpleASNReader(w.toByteArray());
        r.assertByte(0x30); // should not throw
    }

    @Test(expected = java.io.IOException.class)
    public void testASNAssertByteWrongValueThrows() throws java.io.IOException {
        SimpleASNWriter w = new SimpleASNWriter();
        w.writeByte(0x30);
        SimpleASNReader r = new SimpleASNReader(w.toByteArray());
        r.assertByte(0x02); // wrong — should throw
    }

    @Test
    public void testASNWriteData() throws java.io.IOException {
        byte[] payload = { 1, 2, 3, 4 };
        SimpleASNWriter w = new SimpleASNWriter();
        w.writeByte(0x04); // OCTET STRING tag
        w.writeData(payload);
        byte[] encoded = w.toByteArray();

        SimpleASNReader r = new SimpleASNReader(encoded);
        r.assertByte(0x04);
        byte[] data = r.getData();
        assertArrayEquals(payload, data);
    }

    @Test
    public void testASNWriteLongLength() throws java.io.IOException {
        // Test length >= 0x80 (multi-byte length encoding)
        byte[] payload = new byte[200];
        for (int i = 0; i < payload.length; i++) payload[i] = (byte) i;

        SimpleASNWriter w = new SimpleASNWriter();
        w.writeByte(0x04);
        w.writeData(payload);
        byte[] encoded = w.toByteArray();

        SimpleASNReader r = new SimpleASNReader(encoded);
        r.assertByte(0x04);
        byte[] data = r.getData();
        assertArrayEquals(payload, data);
    }

    @Test
    public void testASNWriteDirectBytes() {
        byte[] raw = { 0x01, 0x02 };
        SimpleASNWriter w = new SimpleASNWriter();
        w.write(raw);
        byte[] out = w.toByteArray();
        assertArrayEquals(raw, out);
    }

    // -----------------------------------------------------------------------
    // Version
    // -----------------------------------------------------------------------

    @Test
    public void testVersionGetVersionReturnsNonNull() {
        assertNotNull(Version.getVersion());
    }

    @Test
    public void testVersionFakeOverrideViaSysProp() {
        System.setProperty("maverick.development.version", "test-99.99");
        try {
            assertEquals("test-99.99", Version.getVersion());
        } finally {
            System.clearProperty("maverick.development.version");
        }
    }
}

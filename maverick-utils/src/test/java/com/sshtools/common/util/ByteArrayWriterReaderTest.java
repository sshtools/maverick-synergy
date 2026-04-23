package com.sshtools.common.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.math.BigInteger;

import org.junit.Test;

/**
 * Tests for ByteArrayWriter and ByteArrayReader covering all major wire-format types.
 */
public class ByteArrayWriterReaderTest {

    // -----------------------------------------------------------------------
    // Integer encoding
    // -----------------------------------------------------------------------

    @Test
    public void testWriteReadInt() throws IOException {
        int[] values = { 0, 1, 127, 128, 255, 256, 65535, 65536, Integer.MAX_VALUE };
        for (int v : values) {
            ByteArrayWriter w = new ByteArrayWriter();
            w.writeInt(v);
            ByteArrayReader r = new ByteArrayReader(w.toByteArray());
            assertEquals("roundtrip int " + v, v & 0xFFFFFFFFL, r.readInt());
        }
    }

    @Test
    public void testWriteReadIntAsLong() throws IOException {
        long[] values = { 0L, 1L, 0xFFFFFFFFL };
        for (long v : values) {
            ByteArrayWriter w = new ByteArrayWriter();
            w.writeInt(v);
            ByteArrayReader r = new ByteArrayReader(w.toByteArray());
            assertEquals("roundtrip long " + v, v, r.readInt());
        }
    }

    @Test
    public void testEncodeIntStatic() {
        byte[] encoded = ByteArrayWriter.encodeInt(0x01020304);
        assertEquals((byte) 0x01, encoded[0]);
        assertEquals((byte) 0x02, encoded[1]);
        assertEquals((byte) 0x03, encoded[2]);
        assertEquals((byte) 0x04, encoded[3]);
    }

    @Test
    public void testEncodeIntStaticLong() {
        byte[] encoded = ByteArrayWriter.encodeInt(0x01020304L);
        assertEquals((byte) 0x01, encoded[0]);
        assertEquals((byte) 0x04, encoded[3]);
    }

    @Test
    public void testEncodeIntIntoBuffer() {
        byte[] buf = new byte[6];
        ByteArrayWriter.encodeInt(buf, 1, 0xDEADBEEF);
        assertEquals((byte) 0xDE, buf[1]);
        assertEquals((byte) 0xAD, buf[2]);
        assertEquals((byte) 0xBE, buf[3]);
        assertEquals((byte) 0xEF, buf[4]);
    }

    @Test
    public void testReadIntStaticHelper() {
        byte[] data = { 0x00, 0x00, 0x01, 0x00 };
        long v = ByteArrayReader.readInt(data, 0);
        assertEquals(256L, v);
    }

    // -----------------------------------------------------------------------
    // Short encoding
    // -----------------------------------------------------------------------

    @Test
    public void testWriteReadShort() throws IOException {
        short[] values = { 0, 1, 127, 128, (short) 0xFFFF };
        for (short v : values) {
            ByteArrayWriter w = new ByteArrayWriter();
            w.writeShort(v);
            ByteArrayReader r = new ByteArrayReader(w.toByteArray());
            assertEquals("roundtrip short " + v, v, r.readShort());
        }
    }

    @Test
    public void testReadShortStaticHelper() {
        byte[] data = { 0x01, 0x02 };
        short s = ByteArrayReader.readShort(data, 0);
        assertEquals((short) 0x0102, s);
    }

    // -----------------------------------------------------------------------
    // Boolean encoding
    // -----------------------------------------------------------------------

    @Test
    public void testWriteReadBoolean() throws IOException {
        for (boolean b : new boolean[]{ true, false }) {
            ByteArrayWriter w = new ByteArrayWriter();
            w.writeBoolean(b);
            ByteArrayReader r = new ByteArrayReader(w.toByteArray());
            assertEquals("roundtrip boolean " + b, b, r.readBoolean());
        }
    }

    // -----------------------------------------------------------------------
    // String encoding
    // -----------------------------------------------------------------------

    @Test
    public void testWriteReadString() throws IOException {
        String[] values = { "", "hello", "UTF-8 café", "a".repeat(1000) };
        for (String s : values) {
            ByteArrayWriter w = new ByteArrayWriter();
            w.writeString(s);
            ByteArrayReader r = new ByteArrayReader(w.toByteArray());
            assertEquals("roundtrip string", s, r.readString());
        }
    }

    @Test
    public void testWriteNullStringWritesZeroLength() throws IOException {
        ByteArrayWriter w = new ByteArrayWriter();
        w.writeString(null);
        ByteArrayReader r = new ByteArrayReader(w.toByteArray());
        assertEquals("", r.readString());
    }

    @Test
    public void testEncodeDecodeStringStatic() throws IOException {
        String original = "static encode test";
        byte[] encoded = ByteArrayWriter.encodeString(original);
        String decoded = ByteArrayReader.decodeString(encoded);
        assertEquals(original, decoded);
    }

    // -----------------------------------------------------------------------
    // Binary string encoding
    // -----------------------------------------------------------------------

    @Test
    public void testWriteReadBinaryString() throws IOException {
        byte[] data = { 0x00, 0x01, (byte) 0xFF, 0x7F };
        ByteArrayWriter w = new ByteArrayWriter();
        w.writeBinaryString(data);
        ByteArrayReader r = new ByteArrayReader(w.toByteArray());
        assertArrayEquals(data, r.readBinaryString());
    }

    @Test
    public void testWriteReadBinaryStringWithOffsetLen() throws IOException {
        byte[] data = { 0x00, 0x01, (byte) 0xAB, 0x7F, 0x00 };
        ByteArrayWriter w = new ByteArrayWriter();
        w.writeBinaryString(data, 1, 3);
        ByteArrayReader r = new ByteArrayReader(w.toByteArray());
        byte[] result = r.readBinaryString();
        assertEquals(3, result.length);
        assertEquals(data[1], result[0]);
        assertEquals(data[2], result[1]);
        assertEquals(data[3], result[2]);
    }

    @Test
    public void testWriteNullBinaryStringWritesZeroLength() throws IOException {
        ByteArrayWriter w = new ByteArrayWriter();
        w.writeBinaryString((byte[]) null);
        ByteArrayReader r = new ByteArrayReader(w.toByteArray());
        byte[] result = r.readBinaryString();
        assertEquals(0, result.length);
    }

    // -----------------------------------------------------------------------
    // BigInteger encoding
    // -----------------------------------------------------------------------

    @Test
    public void testWriteReadBigInteger() throws IOException {
        BigInteger[] values = {
            BigInteger.ZERO,
            BigInteger.ONE,
            BigInteger.TWO,
            BigInteger.valueOf(Long.MAX_VALUE),
            new BigInteger("12345678901234567890123456789")
        };
        for (BigInteger bi : values) {
            ByteArrayWriter w = new ByteArrayWriter();
            w.writeBigInteger(bi);
            ByteArrayReader r = new ByteArrayReader(w.toByteArray());
            assertEquals("roundtrip BigInteger " + bi, bi, r.readBigInteger());
        }
    }

    // -----------------------------------------------------------------------
    // UINT32 / UINT64 encoding
    // -----------------------------------------------------------------------

    @Test
    public void testWriteReadUINT32() throws IOException {
        UnsignedInteger32 v = new UnsignedInteger32(0xFFFFFFFEL);
        ByteArrayWriter w = new ByteArrayWriter();
        w.writeUINT32(v);
        ByteArrayReader r = new ByteArrayReader(w.toByteArray());
        assertEquals(0xFFFFFFFEL, r.readUINT32().longValue());
    }

    @Test
    public void testWriteReadUINT64() throws IOException {
        UnsignedInteger64 v = new UnsignedInteger64(Long.MAX_VALUE);
        ByteArrayWriter w = new ByteArrayWriter();
        w.writeUINT64(v);
        ByteArrayReader r = new ByteArrayReader(w.toByteArray());
        assertEquals(Long.MAX_VALUE, r.readUINT64().longValue());
    }

    @Test
    public void testWriteReadUINT64FromLong() throws IOException {
        long value = 123456789L;
        ByteArrayWriter w = new ByteArrayWriter();
        w.writeUINT64(value);
        ByteArrayReader r = new ByteArrayReader(w.toByteArray());
        assertEquals(value, r.readUINT64().longValue());
    }

    // -----------------------------------------------------------------------
    // MPINT (SSH1 16-bit bit-length prefix) encoding
    // -----------------------------------------------------------------------

    @Test
    public void testWriteReadMPINT() throws IOException {
        BigInteger original = BigInteger.valueOf(12345678);
        ByteArrayWriter w = new ByteArrayWriter();
        w.writeMPINT(original);
        ByteArrayReader r = new ByteArrayReader(w.toByteArray());
        assertEquals(original, r.readMPINT());
    }

    @Test
    public void testWriteReadMPINT32() throws IOException {
        BigInteger original = BigInteger.valueOf(987654321L);
        ByteArrayWriter w = new ByteArrayWriter();
        // MPINT32 uses writeInt for bit length, then raw bytes
        int bits = original.bitLength();
        w.writeInt(bits);
        byte[] raw = original.toByteArray();
        int bytes = (bits + 7) / 8;
        // write only the needed bytes (no leading zero)
        if (raw[0] == 0) {
            w.write(raw, 1, bytes);
        } else {
            w.write(raw, 0, bytes);
        }
        ByteArrayReader r = new ByteArrayReader(w.toByteArray());
        assertEquals(original, r.readMPINT32());
    }

    // -----------------------------------------------------------------------
    // Sequential writes
    // -----------------------------------------------------------------------

    @Test
    public void testSequentialWritesAndReads() throws IOException {
        ByteArrayWriter w = new ByteArrayWriter();
        w.writeInt(42);
        w.writeBoolean(true);
        w.writeString("hello");
        w.writeBinaryString(new byte[]{ 1, 2, 3 });

        ByteArrayReader r = new ByteArrayReader(w.toByteArray());
        assertEquals(42L, r.readInt());
        assertTrue(r.readBoolean());
        assertEquals("hello", r.readString());
        assertArrayEquals(new byte[]{ 1, 2, 3 }, r.readBinaryString());
    }

    // -----------------------------------------------------------------------
    // ByteArrayWriter utilities
    // -----------------------------------------------------------------------

    @Test
    public void testLengthConstructorAndArray() {
        ByteArrayWriter w = new ByteArrayWriter(16);
        w.write(0xAB);
        assertEquals((byte) 0xAB, w.array()[0]);
    }

    @Test
    public void testMoveAndSize() throws IOException {
        ByteArrayWriter w = new ByteArrayWriter(4);
        w.writeInt(0x01020304);
        assertEquals(4, w.size());
    }

    @Test
    public void testStripLeadingZeros() {
        byte[] input = { 0, 0, 1, 2 };
        byte[] result = ByteArrayWriter.stripLeadingZeros(input);
        assertArrayEquals(new byte[]{ 1, 2 }, result);
    }

    @Test
    public void testStripLeadingZerosNoneToStrip() {
        byte[] input = { 1, 0, 0 };
        byte[] result = ByteArrayWriter.stripLeadingZeros(input);
        assertArrayEquals(input, result);
    }

    @Test
    public void testSilentClose() throws IOException {
        ByteArrayWriter w = new ByteArrayWriter();
        w.writeInt(1);
        w.silentClose(); // should not throw
    }

    @Test
    public void testDispose() throws IOException {
        ByteArrayWriter w = new ByteArrayWriter();
        w.writeInt(1);
        w.dispose();
        // After dispose, buf is null — no further assertions needed, just no NPE
    }

    // -----------------------------------------------------------------------
    // ByteArrayReader utilities
    // -----------------------------------------------------------------------

    @Test
    public void testReaderArray() {
        byte[] data = { 1, 2, 3 };
        ByteArrayReader r = new ByteArrayReader(data);
        assertArrayEquals(data, r.array());
    }

    @Test
    public void testReaderGetPosition() throws IOException {
        ByteArrayWriter w = new ByteArrayWriter();
        w.writeInt(1);
        ByteArrayReader r = new ByteArrayReader(w.toByteArray());
        assertEquals(0, r.getPosition());
        r.readInt();
        assertEquals(4, r.getPosition());
    }

    @Test
    public void testReaderOffsetConstructor() throws IOException {
        byte[] data = { 0, 0, 0, 0, 0x00, 0x00, 0x00, 0x05 };
        ByteArrayReader r = new ByteArrayReader(data, 4, 4);
        assertEquals(5L, r.readInt());
    }

    @Test(expected = IOException.class)
    public void testReaderThrowsOnTooShortBuffer() throws IOException {
        ByteArrayReader r = new ByteArrayReader(new byte[]{ 0x00, 0x00, 0x10 }); // only 3 bytes
        r.readInt(); // needs 4
    }

    @Test
    public void testReaderReadFullyExact() throws IOException {
        byte[] source = { 1, 2, 3, 4, 5 };
        ByteArrayReader r = new ByteArrayReader(source);
        byte[] dest = new byte[5];
        r.readFully(dest);
        assertArrayEquals(source, dest);
    }

    @Test
    public void testReaderReadFullyWithOffset() throws IOException {
        byte[] source = { 10, 20, 30 };
        ByteArrayReader r = new ByteArrayReader(source);
        byte[] dest = new byte[5];
        r.readFully(dest, 1, 3);
        assertEquals(10, dest[1]);
        assertEquals(20, dest[2]);
        assertEquals(30, dest[3]);
    }

    @Test
    public void testReaderSetCharsetEncoding() {
        ByteArrayReader.setCharsetEncoding("UTF-8");
        assertEquals("UTF-8", ByteArrayReader.getCharsetEncoding());
    }

    @Test
    public void testReaderSilentClose() {
        ByteArrayReader r = new ByteArrayReader(new byte[]{ 1 });
        r.silentClose(); // should not throw
    }

    @Test
    public void testReaderDispose() {
        ByteArrayReader r = new ByteArrayReader(new byte[]{ 1 });
        r.dispose(); // should not throw
    }
}

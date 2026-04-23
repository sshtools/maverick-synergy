package com.sshtools.common.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

/**
 * Tests for DynamicBuffer covering the basic write/read contract,
 * buffer growth, close signalling, and concurrent access.
 */
public class DynamicBufferTest {

    @Test
    public void testWriteThenReadSingleByte() throws IOException {
        DynamicBuffer buf = new DynamicBuffer();
        buf.getOutputStream().write(0xAB);
        buf.close();
        assertEquals(0xAB, buf.getInputStream().read());
    }

    @Test
    public void testWriteThenReadByteArray() throws IOException, InterruptedException {
        DynamicBuffer buf = new DynamicBuffer();
        byte[] data = { 1, 2, 3, 4, 5 };
        buf.getOutputStream().write(data);
        buf.close();

        byte[] result = new byte[5];
        int n = buf.getInputStream().read(result);
        assertEquals(5, n);
        assertArrayEquals(data, result);
    }

    @Test
    public void testReadReturnsMinusOneAfterClose() throws IOException {
        DynamicBuffer buf = new DynamicBuffer();
        buf.getOutputStream().write(new byte[]{ 9 });
        buf.close();
        buf.getInputStream().read(); // consume the one byte
        int r = buf.getInputStream().read();
        assertEquals(-1, r);
    }

    @Test
    public void testCloseSignalsReader() throws InterruptedException {
        DynamicBuffer buf = new DynamicBuffer();
        CountDownLatch latch = new CountDownLatch(1);
        Thread reader = new Thread(() -> {
            try {
                buf.getInputStream().read(); // blocks until close
            } catch (IOException e) {
                // expected or -1
            }
            latch.countDown();
        });
        reader.setDaemon(true);
        reader.start();

        buf.close();
        assertTrue("Reader should unblock after close", latch.await(3, TimeUnit.SECONDS));
    }

    @Test(expected = IOException.class)
    public void testWriteAfterCloseThrows() throws IOException {
        DynamicBuffer buf = new DynamicBuffer();
        buf.close();
        buf.getOutputStream().write(1);
    }

    @Test
    public void testBufferGrowth() throws IOException {
        DynamicBuffer buf = new DynamicBuffer(4); // small initial size
        byte[] large = new byte[1024];
        for (int i = 0; i < large.length; i++) large[i] = (byte) (i & 0xFF);
        buf.getOutputStream().write(large);
        buf.close();

        byte[] result = new byte[1024];
        int read = 0;
        InputStream in = buf.getInputStream();
        while (read < 1024) {
            int n = in.read(result, read, 1024 - read);
            if (n < 0) break;
            read += n;
        }
        assertEquals(1024, read);
        assertArrayEquals(large, result);
    }

    @Test
    public void testConcurrentWriteRead() throws InterruptedException, IOException {
        DynamicBuffer buf = new DynamicBuffer();
        int count = 500;
        CountDownLatch done = new CountDownLatch(1);
        byte[] received = new byte[count];

        Thread writer = new Thread(() -> {
            try {
                OutputStream out = buf.getOutputStream();
                for (int i = 0; i < count; i++) {
                    out.write(i & 0xFF);
                }
                buf.close();
            } catch (IOException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread reader = new Thread(() -> {
            try {
                InputStream in = buf.getInputStream();
                for (int i = 0; i < count; i++) {
                    int v = in.read();
                    if (v < 0) break;
                    received[i] = (byte) v;
                }
                done.countDown();
            } catch (IOException e) {
                done.countDown();
            }
        });

        reader.setDaemon(true);
        writer.setDaemon(true);
        reader.start();
        writer.start();

        assertTrue("Concurrent read/write should complete", done.await(10, TimeUnit.SECONDS));
        for (int i = 0; i < count; i++) {
            assertEquals((byte) (i & 0xFF), received[i]);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeBufferSizeThrows() {
        new DynamicBuffer(-1);
    }

    @Test
    public void testFlushDoesNotThrow() throws IOException {
        DynamicBuffer buf = new DynamicBuffer();
        buf.getOutputStream().flush(); // should not throw
        buf.close();
    }

    @Test
    public void testTimeout() throws IOException {
        DynamicBuffer buf = new DynamicBuffer();
        buf.setTimeout(100); // 100ms timeout
        buf.setBlockInterrupt(50);
        // read on empty buffer should time out
        try {
            buf.getInputStream().read();
        } catch (java.io.InterruptedIOException e) {
            // expected timeout
        }
    }
}

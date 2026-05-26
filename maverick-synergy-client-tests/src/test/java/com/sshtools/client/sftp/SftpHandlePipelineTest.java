package com.sshtools.client.sftp;

/*-
 * #%L
 * Client API Tests
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import com.sshtools.client.SessionChannelNG;
import com.sshtools.client.tasks.FileTransferProgress;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.util.UnsignedInteger32;
import com.sshtools.common.util.UnsignedInteger64;

@DisplayName("SftpHandle optimized transfer pipeline")
class SftpHandlePipelineTest {

    private static final int BLOCK_SIZE = 4096;

    @AfterEach
    void clearOptimizationTelemetry() {
        System.clearProperty("maverick.read.optimizedBlock");
        System.clearProperty("maverick.read.asyncRequests");
        System.clearProperty("maverick.write.optimizedBlock");
        System.clearProperty("maverick.write.asyncRequestsMax");
    }

    @Test
    @DisplayName("performOptimizedWrite posts asynchronously before waiting when request window allows")
    void performOptimizedWritePostsRequestsBeforeWaiting() throws Exception {
        SftpChannel sftp = mockSftpChannel();
        SftpFile file = mock(SftpFile.class);
        when(file.getAbsolutePath()).thenReturn("/tmp/remote.bin");
        when(file.getFilename()).thenReturn("remote.bin");

        SftpHandle handle = new SftpHandle(new byte[] {1}, sftp, file);
        SftpHandle spyHandle = org.mockito.Mockito.spy(handle);

        doReturn(new UnsignedInteger32(99))
            .doReturn(new UnsignedInteger32(100))
                .doReturn(new UnsignedInteger32(101))
                .doReturn(new UnsignedInteger32(102))
                .doReturn(new UnsignedInteger32(103))
                .doReturn(new UnsignedInteger32(104))
                .when(spyHandle).postWriteRequest(anyLong(), any(byte[].class), eq(0), any(int.class));

        byte[] payload = new byte[6 * BLOCK_SIZE];
        spyHandle.performOptimizedWrite("/tmp/remote.bin", BLOCK_SIZE, 2,
                new ByteArrayInputStream(payload), BLOCK_SIZE, null, 0);

        verify(spyHandle, times(6)).postWriteRequest(anyLong(), any(byte[].class), eq(0), any(int.class));
        verify(sftp, times(1)).getOKRequestStatus(any(UnsignedInteger32.class), eq(file));
        verify(sftp, times(5)).getOKRequestStatus(any(UnsignedInteger32.class), eq("/tmp/remote.bin"));

        assertEquals(String.valueOf(BLOCK_SIZE), System.getProperty("maverick.write.optimizedBlock"));
        assertEquals("2", System.getProperty("maverick.write.asyncRequestsMax"));
    }

    @Test
    @DisplayName("performOptimizedRead posts up to outstanding window before first response wait")
    void performOptimizedReadPostsWindowBeforeWaiting() throws Exception {
        SftpChannel sftp = mockSftpChannel();
        SftpFile file = mock(SftpFile.class);
        when(file.getFilename()).thenReturn("remote.bin");
        when(file.getAbsolutePath()).thenReturn("/tmp/remote.bin");

        SftpMessage dataMessage = mock(SftpMessage.class);
        when(dataMessage.getType()).thenReturn(SftpChannel.SSH_FXP_DATA);
        when(dataMessage.readInt()).thenReturn((long) BLOCK_SIZE);
        when(dataMessage.array()).thenReturn(new byte[BLOCK_SIZE]);
        when(dataMessage.getPosition()).thenReturn(0);

        SftpMessage eofMessage = mock(SftpMessage.class);
        when(eofMessage.getType()).thenReturn(SftpChannel.SSH_FXP_STATUS);
        when(eofMessage.readInt()).thenReturn((long) SftpStatusException.SSH_FX_EOF);

        when(sftp.getResponse(any(UnsignedInteger32.class)))
            .thenReturn(dataMessage)
            .thenReturn(dataMessage)
            .thenReturn(dataMessage)
            .thenReturn(dataMessage)
            .thenReturn(dataMessage)
            .thenReturn(eofMessage);

        SftpHandle handle = new SftpHandle(new byte[] {1}, sftp, file);
        SftpHandle spyHandle = org.mockito.Mockito.spy(handle);

        doReturn(BLOCK_SIZE).when(spyHandle).readFile(any(UnsignedInteger64.class), any(byte[].class), eq(0), eq(BLOCK_SIZE));
        doReturn(new UnsignedInteger32(200))
                .doReturn(new UnsignedInteger32(201))
                .doReturn(new UnsignedInteger32(202))
                .doReturn(new UnsignedInteger32(203))
                .doReturn(new UnsignedInteger32(204))
                .when(spyHandle).postReadRequest(anyLong(), eq(BLOCK_SIZE));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        spyHandle.performOptimizedRead(6L * BLOCK_SIZE, BLOCK_SIZE, out, 2, null, 0);

        verify(spyHandle, atLeast(6)).postReadRequest(anyLong(), eq(BLOCK_SIZE));
        verify(sftp, times(6)).getResponse(any(UnsignedInteger32.class));

        InOrder order = inOrder(spyHandle, sftp);
        order.verify(spyHandle).postReadRequest(anyLong(), eq(BLOCK_SIZE));
        order.verify(spyHandle).postReadRequest(anyLong(), eq(BLOCK_SIZE));
        order.verify(sftp).getResponse(any(UnsignedInteger32.class));

        assertEquals(6L * BLOCK_SIZE, out.size());
        assertEquals(String.valueOf(BLOCK_SIZE), System.getProperty("maverick.read.optimizedBlock"));
        assertEquals("2", System.getProperty("maverick.read.asyncRequests"));
    }

    @Test
    @DisplayName("performOptimizedRead honours cancellation during async posting")
    void performOptimizedReadCancelsDuringPosting() throws Exception {
        SftpChannel sftp = mockSftpChannel();
        SftpFile file = mock(SftpFile.class);
        when(file.getFilename()).thenReturn("remote.bin");
        when(file.getAbsolutePath()).thenReturn("/tmp/remote.bin");

        SftpHandle handle = new SftpHandle(new byte[] {1}, sftp, file);
        SftpHandle spyHandle = org.mockito.Mockito.spy(handle);

        doReturn(BLOCK_SIZE).when(spyHandle).readFile(any(UnsignedInteger64.class), any(byte[].class), eq(0), eq(BLOCK_SIZE));
        doReturn(new UnsignedInteger32(300)).when(spyHandle).postReadRequest(anyLong(), eq(BLOCK_SIZE));

        FileTransferProgress cancellingProgress = new FileTransferProgress() {
            @Override
            public boolean isCancelled() {
                return true;
            }
        };

        assertThrows(TransferCancelledException.class,
                () -> spyHandle.performOptimizedRead(6L * BLOCK_SIZE, BLOCK_SIZE,
                        new ByteArrayOutputStream(), 4, cancellingProgress, 0));

        verify(spyHandle, times(1)).postReadRequest(anyLong(), eq(BLOCK_SIZE));
        verify(sftp, never()).getResponse(any(UnsignedInteger32.class));
    }

    @Test
    @DisplayName("performOptimizedRead defers posting when remote window is too small")
    void performOptimizedReadDefersWhenRemoteWindowLow() throws Exception {
        SftpChannel sftp = mockSftpChannel();
        SessionChannelNG session = sftp.getSession();
        when(session.getRemoteWindow())
                .thenReturn(new UnsignedInteger32(65536))
                .thenReturn(new UnsignedInteger32(0));

        SftpFile file = mock(SftpFile.class);
        when(file.getFilename()).thenReturn("remote.bin");
        when(file.getAbsolutePath()).thenReturn("/tmp/remote.bin");

        SftpMessage eofMessage = mock(SftpMessage.class);
        when(eofMessage.getType()).thenReturn(SftpChannel.SSH_FXP_STATUS);
        when(eofMessage.readInt()).thenReturn((long) SftpStatusException.SSH_FX_EOF);
        when(sftp.getResponse(any(UnsignedInteger32.class))).thenReturn(eofMessage);

        SftpHandle handle = new SftpHandle(new byte[] {1}, sftp, file);
        SftpHandle spyHandle = org.mockito.Mockito.spy(handle);

        doReturn(BLOCK_SIZE).when(spyHandle).readFile(any(UnsignedInteger64.class), any(byte[].class), eq(0), eq(BLOCK_SIZE));
        doReturn(new UnsignedInteger32(400)).when(spyHandle).postReadRequest(anyLong(), eq(BLOCK_SIZE));

        spyHandle.performOptimizedRead(3L * BLOCK_SIZE, BLOCK_SIZE, new ByteArrayOutputStream(), 3, null, 0);

        verify(spyHandle, times(1)).postReadRequest(anyLong(), eq(BLOCK_SIZE));
        verify(sftp, times(1)).getResponse(any(UnsignedInteger32.class));
    }

    // ------------------------------------------------------------------ //
    //  Write-path gaps                                                   //
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("performOptimizedWrite throws TransferCancelledException when progress cancels during pipeline")
    void performOptimizedWriteCancelsDuringPipeline() throws Exception {
        SftpChannel sftp = mockSftpChannel();
        SftpFile file = mock(SftpFile.class);
        when(file.getAbsolutePath()).thenReturn("/tmp/cancel-write.bin");
        when(file.getFilename()).thenReturn("cancel-write.bin");

        SftpHandle handle = new SftpHandle(new byte[] {1}, sftp, file);
        SftpHandle spyHandle = org.mockito.Mockito.spy(handle);

        doReturn(new UnsignedInteger32(500))
            .doReturn(new UnsignedInteger32(501))
            .doReturn(new UnsignedInteger32(502))
            .when(spyHandle).postWriteRequest(anyLong(), any(byte[].class), eq(0), any(int.class));

        // First isCancelled() call (after sync writeFile) returns false; second returns true.
        final int[] callCount = {0};
        FileTransferProgress cancelAfterFirstAsync = new FileTransferProgress() {
            @Override
            public boolean isCancelled() {
                return ++callCount[0] > 1;
            }
        };

        byte[] payload = new byte[3 * BLOCK_SIZE];
        assertThrows(TransferCancelledException.class,
                () -> spyHandle.performOptimizedWrite("/tmp/cancel-write.bin", BLOCK_SIZE, 2,
                        new ByteArrayInputStream(payload), BLOCK_SIZE, cancelAfterFirstAsync, 0));

        // 1 postWriteRequest from sync writeFile + 1 from async loop before cancel
        verify(spyHandle, times(2)).postWriteRequest(anyLong(), any(byte[].class), eq(0), any(int.class));
        // Sync writeFile drains via getOKRequestStatus(id, SftpFile); async drain loop never reached
        verify(sftp, times(1)).getOKRequestStatus(any(UnsignedInteger32.class), eq(file));
        verify(sftp, never()).getOKRequestStatus(any(UnsignedInteger32.class), eq("/tmp/cancel-write.bin"));
    }

    @Test
    @DisplayName("performOptimizedWrite with empty InputStream posts no requests and completes normally")
    void performOptimizedWriteEmptyInputStreamSkipsPipeline() throws Exception {
        SftpChannel sftp = mockSftpChannel();
        SftpFile file = mock(SftpFile.class);
        when(file.getAbsolutePath()).thenReturn("/tmp/empty.bin");
        when(file.getFilename()).thenReturn("empty.bin");

        SftpHandle handle = new SftpHandle(new byte[] {1}, sftp, file);
        SftpHandle spyHandle = org.mockito.Mockito.spy(handle);

        // Empty payload — should complete without error and without any async posts
        spyHandle.performOptimizedWrite("/tmp/empty.bin", BLOCK_SIZE, 2,
                new ByteArrayInputStream(new byte[0]), BLOCK_SIZE, null, 0);

        verify(spyHandle, never()).postWriteRequest(anyLong(), any(byte[].class), anyInt(), anyInt());
        verify(sftp, never()).getOKRequestStatus(any(UnsignedInteger32.class), any(String.class));
    }

    // ------------------------------------------------------------------ //
    //  Read-path gaps                                                     //
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("performOptimizedRead returns immediately and posts no requests when first block is EOF")
    void performOptimizedReadFirstBlockEofSkipsPipeline() throws Exception {
        SftpChannel sftp = mockSftpChannel();
        SftpFile file = mock(SftpFile.class);
        when(file.getFilename()).thenReturn("remote.bin");
        when(file.getAbsolutePath()).thenReturn("/tmp/remote.bin");

        SftpHandle handle = new SftpHandle(new byte[] {1}, sftp, file);
        SftpHandle spyHandle = org.mockito.Mockito.spy(handle);

        // Synchronous first-block read returns -1 (EOF before any data)
        doReturn(-1).when(spyHandle).readFile(any(UnsignedInteger64.class), any(byte[].class), eq(0), eq(BLOCK_SIZE));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        spyHandle.performOptimizedRead(3L * BLOCK_SIZE, BLOCK_SIZE, out, 2, null, 0);

        verify(spyHandle, never()).postReadRequest(anyLong(), anyInt());
        verify(sftp, never()).getResponse(any(UnsignedInteger32.class));
        assertEquals(0, out.size(), "no bytes should be written when first block is EOF");
    }

    @Test
    @DisplayName("performOptimizedRead propagates SftpStatusException when server returns non-EOF error status")
    void performOptimizedReadNonEofStatusThrowsSftpStatusException() throws Exception {
        SftpChannel sftp = mockSftpChannel();
        SftpFile file = mock(SftpFile.class);
        when(file.getFilename()).thenReturn("remote.bin");
        when(file.getAbsolutePath()).thenReturn("/tmp/remote.bin");

        SftpMessage errorMessage = mock(SftpMessage.class);
        when(errorMessage.getType()).thenReturn(SftpChannel.SSH_FXP_STATUS);
        // SSH_FX_FAILURE = 4, distinct from SSH_FX_EOF = 1
        when(errorMessage.readInt()).thenReturn((long) SftpStatusException.SSH_FX_FAILURE);
        when(sftp.getResponse(any(UnsignedInteger32.class))).thenReturn(errorMessage);

        SftpHandle handle = new SftpHandle(new byte[] {1}, sftp, file);
        SftpHandle spyHandle = org.mockito.Mockito.spy(handle);

        doReturn(BLOCK_SIZE).when(spyHandle).readFile(any(UnsignedInteger64.class), any(byte[].class), eq(0), eq(BLOCK_SIZE));
        doReturn(new UnsignedInteger32(600))
            .doReturn(new UnsignedInteger32(601))
            .when(spyHandle).postReadRequest(anyLong(), eq(BLOCK_SIZE));

        assertThrows(SftpStatusException.class,
                () -> spyHandle.performOptimizedRead(3L * BLOCK_SIZE, BLOCK_SIZE,
                        new ByteArrayOutputStream(), 2, null, 0));
    }

    // ------------------------------------------------------------------ //
    //  Validation guards                                                  //
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("performOptimizedRead throws SshException when blocksize is below minimum (4096)")
    void performOptimizedReadInvalidBlocksizeThrows() {
        SftpChannel sftp = mockSftpChannel();
        SftpFile file = mock(SftpFile.class);
        when(file.getFilename()).thenReturn("remote.bin");

        SftpHandle handle = new SftpHandle(new byte[]{1}, sftp, file);

        assertThrows(SshException.class,
            () -> handle.performOptimizedRead(3L * BLOCK_SIZE, 100 /* 0 < 100 < 4096 */,
                new ByteArrayOutputStream(), 2, null, 0));
    }

    @Test
    @DisplayName("performOptimizedWrite throws SshException when blocksize is below minimum (4096)")
    void performOptimizedWriteInvalidBlocksizeThrows() {
        SftpChannel sftp = mockSftpChannel();
        SftpFile file = mock(SftpFile.class);
        when(file.getFilename()).thenReturn("remote.bin");

        SftpHandle handle = new SftpHandle(new byte[]{1}, sftp, file);

        assertThrows(SshException.class,
            () -> handle.performOptimizedWrite("/tmp/remote.bin", 100 /* 0 < 100 < 4096 */, 2,
                new ByteArrayInputStream(new byte[BLOCK_SIZE]), BLOCK_SIZE, null, 0));
    }

    @Test
    @DisplayName("performOptimizedRead throws SshException for negative position")
    void performOptimizedReadNegativePositionThrows() {
        SftpChannel sftp = mockSftpChannel();
        SftpFile file = mock(SftpFile.class);
        when(file.getFilename()).thenReturn("remote.bin");

        SftpHandle handle = new SftpHandle(new byte[]{1}, sftp, file);

        assertThrows(SshException.class,
            () -> handle.performOptimizedRead(3L * BLOCK_SIZE, BLOCK_SIZE,
                new ByteArrayOutputStream(), 2, null, -1L));
    }

    @Test
    @DisplayName("performOptimizedWrite throws SshException for negative position")
    void performOptimizedWriteNegativePositionThrows() {
        SftpChannel sftp = mockSftpChannel();
        SftpFile file = mock(SftpFile.class);
        when(file.getFilename()).thenReturn("remote.bin");

        SftpHandle handle = new SftpHandle(new byte[]{1}, sftp, file);

        assertThrows(SshException.class,
            () -> handle.performOptimizedWrite("/tmp/remote.bin", BLOCK_SIZE, 2,
                new ByteArrayInputStream(new byte[BLOCK_SIZE]), BLOCK_SIZE, null, -1L));
    }

    // ------------------------------------------------------------------ //
    //  Pipeline edge cases                                                //
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("performOptimizedRead returns immediately when the first block fills the full requested length")
    void performOptimizedReadFirstBlockIsEntireFileReturnsImmediately() throws Exception {
        SftpChannel sftp = mockSftpChannel();
        SftpFile file = mock(SftpFile.class);
        when(file.getFilename()).thenReturn("remote.bin");
        when(file.getAbsolutePath()).thenReturn("/tmp/remote.bin");

        SftpHandle handle = new SftpHandle(new byte[]{1}, sftp, file);
        SftpHandle spyHandle = org.mockito.Mockito.spy(handle);

        doReturn(BLOCK_SIZE).when(spyHandle)
            .readFile(any(UnsignedInteger64.class), any(byte[].class), eq(0), eq(BLOCK_SIZE));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        spyHandle.performOptimizedRead(BLOCK_SIZE /* length == one block */, BLOCK_SIZE, out, 2, null, 0);

        verify(spyHandle, never()).postReadRequest(anyLong(), anyInt());
        verify(sftp, never()).getResponse(any(UnsignedInteger32.class));
        assertEquals(BLOCK_SIZE, out.size(), "output must contain exactly the first block of bytes");
    }

    @Test
    @DisplayName("performOptimizedWrite drains in-flight requests in-loop when maxAsyncRequests is reached")
    void performOptimizedWriteInLoopDrainFiresAtWindowLimit() throws Exception {
        SftpChannel sftp = mockSftpChannel();
        SftpFile file = mock(SftpFile.class);
        when(file.getAbsolutePath()).thenReturn("/tmp/drain.bin");
        when(file.getFilename()).thenReturn("drain.bin");

        SftpHandle handle = new SftpHandle(new byte[]{1}, sftp, file);
        SftpHandle spyHandle = org.mockito.Mockito.spy(handle);

        doReturn(new UnsignedInteger32(700))
            .doReturn(new UnsignedInteger32(701))
            .doReturn(new UnsignedInteger32(702))
            .doReturn(new UnsignedInteger32(703))
            .when(spyHandle).postWriteRequest(anyLong(), any(byte[].class), eq(0), any(int.class));

        // maxAsyncRequests=1 forces in-loop drain whenever requests.size() > 1
        byte[] payload = new byte[4 * BLOCK_SIZE];
        spyHandle.performOptimizedWrite("/tmp/drain.bin", BLOCK_SIZE, 1,
                new ByteArrayInputStream(payload), BLOCK_SIZE, null, 0);

        // 1 sync (writeFile) + 3 async loop iterations = 4 total postWriteRequest calls
        verify(spyHandle, times(4)).postWriteRequest(anyLong(), any(byte[].class), eq(0), any(int.class));
        // 1 drain from writeFile (SftpFile overload)
        verify(sftp, times(1)).getOKRequestStatus(any(UnsignedInteger32.class), eq(file));
        // 2 in-loop drains + 1 final drain = 3 drains via the path-String overload
        verify(sftp, times(3)).getOKRequestStatus(any(UnsignedInteger32.class), eq("/tmp/drain.bin"));
    }

    @Test
    @DisplayName("performOptimizedRead converts OutputStream write IOException into TransferCancelledException")
    void performOptimizedReadOutputStreamWriteExceptionBecomesTransferCancelled() throws Exception {
        SftpChannel sftp = mockSftpChannel();
        SftpFile file = mock(SftpFile.class);
        when(file.getFilename()).thenReturn("remote.bin");
        when(file.getAbsolutePath()).thenReturn("/tmp/remote.bin");

        SftpHandle handle = new SftpHandle(new byte[]{1}, sftp, file);
        SftpHandle spyHandle = org.mockito.Mockito.spy(handle);

        doReturn(BLOCK_SIZE).when(spyHandle)
            .readFile(any(UnsignedInteger64.class), any(byte[].class), eq(0), eq(BLOCK_SIZE));

        OutputStream throwingOut = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                throw new IOException("simulated write failure");
            }
            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                throw new IOException("simulated write failure");
            }
        };

        assertThrows(TransferCancelledException.class,
            () -> spyHandle.performOptimizedRead(3L * BLOCK_SIZE, BLOCK_SIZE, throwingOut, 2, null, 0));
    }

    @Test
    @DisplayName("performOptimizedWrite reports resume offset to progress before first write when position > 0")
    void performOptimizedWriteWithResumePositionCallsProgressWithOffset() throws Exception {
        SftpChannel sftp = mockSftpChannel();
        SftpFile file = mock(SftpFile.class);
        when(file.getAbsolutePath()).thenReturn("/tmp/resume.bin");
        when(file.getFilename()).thenReturn("resume.bin");

        SftpHandle handle = new SftpHandle(new byte[]{1}, sftp, file);
        SftpHandle spyHandle = org.mockito.Mockito.spy(handle);

        doReturn(new UnsignedInteger32(800))
            .doReturn(new UnsignedInteger32(801))
            .when(spyHandle).postWriteRequest(anyLong(), any(byte[].class), eq(0), any(int.class));

        FileTransferProgress progress = mock(FileTransferProgress.class);
        when(progress.isCancelled()).thenReturn(false);

        long resumePosition = BLOCK_SIZE;
        spyHandle.performOptimizedWrite("/tmp/resume.bin", BLOCK_SIZE, 2,
                new ByteArrayInputStream(new byte[2 * BLOCK_SIZE]), BLOCK_SIZE, progress, resumePosition);

        // progress.progressed(BLOCK_SIZE) must be called once for the resume-offset notification
        verify(progress).progressed(resumePosition);
    }

    // ------------------------------------------------------------------ //
    //  SftpHandle contract                                               //
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("SftpHandle equals and hashCode identity is determined by the handle byte array only")
    void handleEqualsAndHashCodeContractBasedOnHandleBytes() {
        SftpChannel sftp = mockSftpChannel();
        SftpFile fileA = mock(SftpFile.class);
        SftpFile fileB = mock(SftpFile.class);

        SftpHandle h1 = new SftpHandle(new byte[]{1, 2, 3}, sftp, fileA);
        SftpHandle h2 = new SftpHandle(new byte[]{1, 2, 3}, sftp, fileB); // same bytes, different file
        SftpHandle h3 = new SftpHandle(new byte[]{9, 8, 7}, sftp, fileA); // different bytes

        assertTrue(h1.equals(h1), "handle must equal itself");
        assertTrue(h1.equals(h2), "handles with identical bytes must be equal regardless of file");
        assertFalse(h1.equals(h3), "handles with different bytes must not be equal");
        assertFalse(h1.equals(null), "handle must not equal null");
        assertEquals(h1.hashCode(), h2.hashCode(), "equal handles must have identical hashCodes");
    }

    private static SftpChannel mockSftpChannel() {
        SftpChannel sftp = mock(SftpChannel.class);
        SessionChannelNG session = mock(SessionChannelNG.class);

        when(sftp.getSession()).thenReturn(session);
        when(sftp.getVersion()).thenReturn(3);

        when(session.getMaximumRemotePacketLength()).thenReturn(65536);
        when(session.getMaximumLocalPacketLength()).thenReturn(65536);
        when(session.getMaxiumRemotePacketSize()).thenReturn(65536);
        when(session.getMaximumWindowSpace()).thenReturn(new UnsignedInteger32(65536));
        when(session.getRemoteWindow()).thenReturn(new UnsignedInteger32(65536));

        return sftp;
    }

}

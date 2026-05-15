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
package com.sshtools.client.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.sshtools.client.sftp.DirectoryOperation;

/**
 * Unit tests for {@link DirectoryOperation}.
 * <p>
 * Tests cover the state management of directory-operation tracking objects
 * without requiring a network connection or SSH session.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class SftpClientUnitTest {

    // ------------------------------------------------------------------
    // Initial state
    // ------------------------------------------------------------------

    @Test
    void defaultConstruction_fileCountIsZero() {
        assertEquals(0, new DirectoryOperation().getFileCount());
    }

    @Test
    void defaultConstruction_newFilesEmpty() {
        assertNotNull(new DirectoryOperation().getNewFiles());
        assertTrue(new DirectoryOperation().getNewFiles().isEmpty());
    }

    @Test
    void defaultConstruction_updatedFilesEmpty() {
        assertTrue(new DirectoryOperation().getUpdatedFiles().isEmpty());
    }

    @Test
    void defaultConstruction_unchangedFilesEmpty() {
        assertTrue(new DirectoryOperation().getUnchangedFiles().isEmpty());
    }

    @Test
    void defaultConstruction_deletedFilesEmpty() {
        assertTrue(new DirectoryOperation().getDeletedFiles().isEmpty());
    }

    @Test
    void defaultConstruction_failedTransfersEmpty() {
        assertTrue(new DirectoryOperation().getFailedTransfers().isEmpty());
    }

    // ------------------------------------------------------------------
    // Mutable Vector returned directly – add via public getter
    // ------------------------------------------------------------------

    @Test
    void getFileCount_countsNewAndUpdated() {
        DirectoryOperation op = new DirectoryOperation();
        op.getNewFiles().add("file1.txt");
        op.getNewFiles().add("file2.txt");
        op.getUpdatedFiles().add("file3.txt");
        assertEquals(3, op.getFileCount());
    }

    @Test
    void getFileCount_unchangedAndDeletedNotCounted() {
        DirectoryOperation op = new DirectoryOperation();
        op.getUnchangedFiles().add("unchanged.txt");
        op.getDeletedFiles().add("deleted.txt");
        assertEquals(0, op.getFileCount(),
                "Unchanged and deleted files must not affect getFileCount()");
    }

    // ------------------------------------------------------------------
    // addDirectoryOperation(DirectoryOperation, String)
    // ------------------------------------------------------------------

    @Test
    void addDirectoryOperation_mergesNewFiles() {
        DirectoryOperation src = new DirectoryOperation();
        src.getNewFiles().add("a.txt");

        DirectoryOperation dst = new DirectoryOperation();
        dst.addDirectoryOperation(src, "subdir");

        assertTrue(dst.getNewFiles().contains("a.txt"));
    }

    @Test
    void addDirectoryOperation_mergesUpdatedFiles() {
        DirectoryOperation src = new DirectoryOperation();
        src.getUpdatedFiles().add("b.txt");

        DirectoryOperation dst = new DirectoryOperation();
        dst.addDirectoryOperation(src, "subdir");

        assertTrue(dst.getUpdatedFiles().contains("b.txt"));
    }

    @Test
    void addDirectoryOperation_mergesDeletedFiles() {
        DirectoryOperation src = new DirectoryOperation();
        src.getDeletedFiles().add("gone.txt");

        DirectoryOperation dst = new DirectoryOperation();
        dst.addDirectoryOperation(src, "subdir");

        assertTrue(dst.getDeletedFiles().contains("gone.txt"));
    }

    @Test
    void addDirectoryOperation_mergesFailedTransfers() {
        DirectoryOperation src = new DirectoryOperation();
        src.getFailedTransfers().put("bad.txt", "error");

        DirectoryOperation dst = new DirectoryOperation();
        dst.addDirectoryOperation(src, "subdir");

        assertTrue(dst.getFailedTransfers().containsKey("bad.txt"));
    }

    @Test
    void addDirectoryOperation_accumulatesFileCount() {
        DirectoryOperation src1 = new DirectoryOperation();
        src1.getNewFiles().add("n1.txt");

        DirectoryOperation src2 = new DirectoryOperation();
        src2.getNewFiles().add("n2.txt");
        src2.getUpdatedFiles().add("u1.txt");

        DirectoryOperation dst = new DirectoryOperation();
        dst.addDirectoryOperation(src1, "dir1");
        dst.addDirectoryOperation(src2, "dir2");

        assertEquals(3, dst.getFileCount());
    }

    // ------------------------------------------------------------------
    // getNewFiles / getUpdatedFiles mutation affects fileCount
    // ------------------------------------------------------------------

    @Test
    void getFileCount_afterMerge_reflectsSourceCounts() {
        DirectoryOperation src = new DirectoryOperation();
        src.getNewFiles().add("x.txt");
        src.getUpdatedFiles().add("y.txt");

        DirectoryOperation dst = new DirectoryOperation();
        assertEquals(0, dst.getFileCount(), "Destination starts at zero");
        dst.addDirectoryOperation(src, "somedir");
        assertEquals(2, dst.getFileCount(), "Merged fileCount should be 2");
    }
}

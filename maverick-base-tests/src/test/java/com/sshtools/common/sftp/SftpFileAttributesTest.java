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
package com.sshtools.common.sftp;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import org.junit.Test;

import com.sshtools.common.sftp.SftpFileAttributes.SftpFileAttributesBuilder;
import com.sshtools.common.util.ByteArrayReader;

/**
 * Tests for {@link SftpFileAttributes} and {@link SftpFileAttributesBuilder}.
 */
public class SftpFileAttributesTest {

    // -----------------------------------------------------------------------
    // File type detection
    // -----------------------------------------------------------------------

    @Test
    public void testRegularFileType() {
        SftpFileAttributes attrs = SftpFileAttributesBuilder.create()
                .withType(SftpFileAttributes.SSH_FILEXFER_TYPE_REGULAR)
                .build();
        assertTrue("Should be regular file", attrs.isFile());
        assertFalse("Should not be directory", attrs.isDirectory());
        assertFalse("Should not be link", attrs.isLink());
        assertFalse("Should not be socket", attrs.isSocket());
        assertFalse("Should not be fifo", attrs.isFifo());
    }

    @Test
    public void testDirectoryType() {
        SftpFileAttributes attrs = SftpFileAttributesBuilder.create()
                .withType(SftpFileAttributes.SSH_FILEXFER_TYPE_DIRECTORY)
                .build();
        assertTrue("Should be directory", attrs.isDirectory());
        assertFalse("Should not be regular file", attrs.isFile());
        assertFalse("Should not be link", attrs.isLink());
    }

    @Test
    public void testSymlinkType() {
        SftpFileAttributes attrs = SftpFileAttributesBuilder.create()
                .withType(SftpFileAttributes.SSH_FILEXFER_TYPE_SYMLINK)
                .build();
        assertTrue("Should be link", attrs.isLink());
        assertFalse("Should not be regular file", attrs.isFile());
        assertFalse("Should not be directory", attrs.isDirectory());
    }

    @Test
    public void testSocketType() {
        SftpFileAttributes attrs = SftpFileAttributesBuilder.create()
                .withType(SftpFileAttributes.SSH_FILEXFER_TYPE_SOCKET)
                .build();
        assertTrue("Should be socket", attrs.isSocket());
        assertFalse("Should not be regular file", attrs.isFile());
    }

    @Test
    public void testFifoType() {
        SftpFileAttributes attrs = SftpFileAttributesBuilder.create()
                .withType(SftpFileAttributes.SSH_FILEXFER_TYPE_FIFO)
                .build();
        assertTrue("Should be fifo", attrs.isFifo());
        assertFalse("Should not be regular file", attrs.isFile());
    }

    @Test
    public void testBlockDeviceType() {
        SftpFileAttributes attrs = SftpFileAttributesBuilder.create()
                .withType(SftpFileAttributes.SSH_FILEXFER_TYPE_BLOCK_DEVICE)
                .build();
        assertTrue("Should be block device", attrs.isBlock());
        assertFalse("Should not be regular file", attrs.isFile());
    }

    @Test
    public void testCharDeviceType() {
        SftpFileAttributes attrs = SftpFileAttributesBuilder.create()
                .withType(SftpFileAttributes.SSH_FILEXFER_TYPE_CHAR_DEVICE)
                .build();
        assertTrue("Should be character device", attrs.isCharacter());
        assertFalse("Should not be regular file", attrs.isFile());
    }

    @Test
    public void testSpecialType() {
        SftpFileAttributes attrs = SftpFileAttributesBuilder.create()
                .withType(SftpFileAttributes.SSH_FILEXFER_TYPE_SPECIAL)
                .build();
        assertTrue("Should be special", attrs.isSpecial());
    }

    // -----------------------------------------------------------------------
    // Size
    // -----------------------------------------------------------------------

    @Test
    public void testSize() {
        SftpFileAttributes attrs = SftpFileAttributesBuilder.create()
                .withType(SftpFileAttributes.SSH_FILEXFER_TYPE_REGULAR)
                .withSize(12345L)
                .build();
        assertEquals(12345L, attrs.size().longValue());
        assertTrue(attrs.sizeOr().isPresent());
        assertEquals(12345L, attrs.sizeOr().get().longValue());
    }

    @Test
    public void testNoSize() {
        SftpFileAttributes attrs = SftpFileAttributesBuilder.create()
                .withType(SftpFileAttributes.SSH_FILEXFER_TYPE_REGULAR)
                .build();
        assertFalse("Size should be absent", attrs.sizeOr().isPresent());
    }

    @Test
    public void testSizeZero() {
        SftpFileAttributes attrs = SftpFileAttributesBuilder.create()
                .withType(SftpFileAttributes.SSH_FILEXFER_TYPE_REGULAR)
                .withSize(0L)
                .build();
        assertEquals(0L, attrs.size().longValue());
    }

    // -----------------------------------------------------------------------
    // Permissions
    // -----------------------------------------------------------------------

    @Test
    public void testPermissionsSet() {
        SftpFileAttributes attrs = SftpFileAttributesBuilder.create()
                .withType(SftpFileAttributes.SSH_FILEXFER_TYPE_REGULAR)
                .withPermissions(
                        PosixFilePermission.OWNER_READ,
                        PosixFilePermission.OWNER_WRITE,
                        PosixFilePermission.GROUP_READ,
                        PosixFilePermission.OTHERS_READ)
                .build();
        assertNotNull(attrs.permissions());
        assertTrue(attrs.permissionsOr().isPresent());
    }

    @Test
    public void testNoPermissions() {
        SftpFileAttributes attrs = SftpFileAttributesBuilder.create()
                .withType(SftpFileAttributes.SSH_FILEXFER_TYPE_REGULAR)
                .build();
        assertFalse("Permissions should be absent", attrs.permissionsOr().isPresent());
    }

    @Test
    public void testPermissionsStringForRegularFile() {
        SftpFileAttributes attrs = SftpFileAttributesBuilder.create()
                .withType(SftpFileAttributes.SSH_FILEXFER_TYPE_REGULAR)
                .withPermissions(
                        PosixFilePermission.OWNER_READ,
                        PosixFilePermission.OWNER_WRITE,
                        PosixFilePermission.GROUP_READ,
                        PosixFilePermission.OTHERS_READ)
                .build();
        String permStr = attrs.toPermissionsString();
        assertNotNull(permStr);
        assertEquals(10, permStr.length());
        assertEquals('-', permStr.charAt(0)); // regular file
    }

    @Test
    public void testPermissionsStringForDirectory() {
        SftpFileAttributes attrs = SftpFileAttributesBuilder.create()
                .withType(SftpFileAttributes.SSH_FILEXFER_TYPE_DIRECTORY)
                .withPermissions(
                        PosixFilePermission.OWNER_READ,
                        PosixFilePermission.OWNER_WRITE,
                        PosixFilePermission.OWNER_EXECUTE,
                        PosixFilePermission.GROUP_READ,
                        PosixFilePermission.GROUP_EXECUTE,
                        PosixFilePermission.OTHERS_READ,
                        PosixFilePermission.OTHERS_EXECUTE)
                .build();
        String permStr = attrs.toPermissionsString();
        assertEquals('d', permStr.charAt(0)); // directory prefix
        assertEquals(10, permStr.length());
    }

    @Test
    public void testPermissionsStringForSymlink() {
        SftpFileAttributes attrs = SftpFileAttributesBuilder.create()
                .withType(SftpFileAttributes.SSH_FILEXFER_TYPE_SYMLINK)
                .build();
        String permStr = attrs.toPermissionsString();
        assertEquals('l', permStr.charAt(0));
    }

    @Test
    public void testPermissionsStringNoPermissions() {
        SftpFileAttributes attrs = SftpFileAttributesBuilder.create()
                .withType(SftpFileAttributes.SSH_FILEXFER_TYPE_REGULAR)
                .build();
        String permStr = attrs.toPermissionsString();
        // no permissions set - should still be 10 chars
        assertEquals(10, permStr.length());
        assertEquals('-', permStr.charAt(0));
    }

    @Test
    public void testMaskStringWithNoPermissions() {
        SftpFileAttributes attrs = SftpFileAttributesBuilder.create()
                .withType(SftpFileAttributes.SSH_FILEXFER_TYPE_REGULAR)
                .build();
        // no permissions => fallback "----"
        assertEquals("----", attrs.toMaskString());
    }

    // -----------------------------------------------------------------------
    // UID / GID / Username / Group
    // -----------------------------------------------------------------------

    @Test
    public void testUidAndGid() {
        SftpFileAttributes attrs = SftpFileAttributesBuilder.create()
                .withType(SftpFileAttributes.SSH_FILEXFER_TYPE_REGULAR)
                .withUid(1000)
                .withGid(1001)
                .build();
        assertEquals(1000, attrs.uid());
        assertEquals(1001, attrs.gid());
        assertTrue(attrs.uidOr().isPresent());
        assertTrue(attrs.gidOr().isPresent());
    }

    @Test
    public void testNoUidGid() {
        SftpFileAttributes attrs = SftpFileAttributesBuilder.create()
                .withType(SftpFileAttributes.SSH_FILEXFER_TYPE_REGULAR)
                .build();
        assertFalse("uid should be absent", attrs.uidOr().isPresent());
        assertFalse("gid should be absent", attrs.gidOr().isPresent());
        assertEquals(0, attrs.uid()); // default
        assertEquals(0, attrs.gid()); // default
    }

    @Test
    public void testUsernameAndGroup() {
        SftpFileAttributes attrs = SftpFileAttributesBuilder.create()
                .withType(SftpFileAttributes.SSH_FILEXFER_TYPE_REGULAR)
                .withUsername("alice")
                .withGroup("staff")
                .build();
        assertEquals("alice", attrs.username());
        assertEquals("staff", attrs.group());
        assertTrue(attrs.usernameOr().isPresent());
        assertTrue(attrs.groupOr().isPresent());
    }

    @Test
    public void testBestUsername() {
        // With username set
        SftpFileAttributes withName = SftpFileAttributesBuilder.create()
                .withUsername("bob")
                .build();
        assertEquals("bob", withName.bestUsername());

        // Without username but with uid
        SftpFileAttributes withUid = SftpFileAttributesBuilder.create()
                .withUid(42)
                .build();
        assertEquals("42", withUid.bestUsername());

        // Neither
        SftpFileAttributes neither = SftpFileAttributesBuilder.create().build();
        assertEquals("nouser", neither.bestUsername());
    }

    @Test
    public void testBestGroup() {
        SftpFileAttributes withGroup = SftpFileAttributesBuilder.create()
                .withGroup("admins")
                .build();
        assertEquals("admins", withGroup.bestGroup());

        SftpFileAttributes withGid = SftpFileAttributesBuilder.create()
                .withGid(100)
                .build();
        assertEquals("100", withGid.bestGroup());

        SftpFileAttributes neither = SftpFileAttributesBuilder.create().build();
        assertEquals("nogroup", neither.bestGroup());
    }

    // -----------------------------------------------------------------------
    // Timestamps
    // -----------------------------------------------------------------------

    @Test
    public void testLastModifiedTime() {
        long timeMs = 1_700_000_000_000L;
        SftpFileAttributes attrs = SftpFileAttributesBuilder.create()
                .withType(SftpFileAttributes.SSH_FILEXFER_TYPE_REGULAR)
                .withLastModifiedTime(timeMs)
                .build();
        assertTrue(attrs.lastModifiedTimeOr().isPresent());
        assertEquals(FileTime.fromMillis(timeMs), attrs.lastModifiedTime());
    }

    @Test
    public void testLastAccessTime() {
        long timeMs = 1_600_000_000_000L;
        SftpFileAttributes attrs = SftpFileAttributesBuilder.create()
                .withType(SftpFileAttributes.SSH_FILEXFER_TYPE_REGULAR)
                .withLastAccessTime(timeMs)
                .build();
        assertTrue(attrs.lastAccessTimeOr().isPresent());
        assertEquals(FileTime.fromMillis(timeMs), attrs.lastAccessTime());
    }

    @Test
    public void testNoTimestamps() {
        SftpFileAttributes attrs = SftpFileAttributesBuilder.create()
                .withType(SftpFileAttributes.SSH_FILEXFER_TYPE_REGULAR)
                .build();
        assertFalse(attrs.lastModifiedTimeOr().isPresent());
        assertFalse(attrs.lastAccessTimeOr().isPresent());
        assertFalse(attrs.lastAttributesModifiedTimeOr().isPresent());
    }

    // -----------------------------------------------------------------------
    // Extended attributes
    // -----------------------------------------------------------------------

    @Test
    public void testExtendedAttributes() {
        byte[] value = new byte[]{0x01, 0x02, 0x03};
        SftpFileAttributes attrs = SftpFileAttributesBuilder.create()
                .withType(SftpFileAttributes.SSH_FILEXFER_TYPE_REGULAR)
                .addExtendedAttribute("custom@example.com", value)
                .build();
        assertTrue(attrs.hasExtendedAttribute("custom@example.com"));
        assertArrayEquals(value, attrs.extendedAttribute("custom@example.com"));
    }

    @Test
    public void testNoExtendedAttributes() {
        SftpFileAttributes attrs = SftpFileAttributesBuilder.create()
                .withType(SftpFileAttributes.SSH_FILEXFER_TYPE_REGULAR)
                .build();
        assertFalse(attrs.hasExtendedAttribute("anything"));
        assertTrue(attrs.extendedAttributes().isEmpty());
    }

    @Test
    public void testMultipleExtendedAttributes() {
        SftpFileAttributes attrs = SftpFileAttributesBuilder.create()
                .withType(SftpFileAttributes.SSH_FILEXFER_TYPE_REGULAR)
                .addExtendedAttribute("key1@example.com", new byte[]{1})
                .addExtendedAttribute("key2@example.com", new byte[]{2})
                .build();
        assertEquals(2, attrs.extendedAttributes().size());
        assertTrue(attrs.hasExtendedAttribute("key1@example.com"));
        assertTrue(attrs.hasExtendedAttribute("key2@example.com"));
    }

    // -----------------------------------------------------------------------
    // Attribute flags (readonly, hidden, etc.)
    // -----------------------------------------------------------------------

    @Test
    public void testReadOnlyFlag() {
        SftpFileAttributes attrs = SftpFileAttributesBuilder.create()
                .withType(SftpFileAttributes.SSH_FILEXFER_TYPE_REGULAR)
                .withReadOnly(true)
                .build();
        assertTrue(attrs.isReadOnly());
    }

    @Test
    public void testHiddenFlag() {
        SftpFileAttributes attrs = SftpFileAttributesBuilder.create()
                .withType(SftpFileAttributes.SSH_FILEXFER_TYPE_REGULAR)
                .withHidden(true)
                .build();
        assertTrue(attrs.isHidden());
    }

    @Test
    public void testArchiveFlag() {
        SftpFileAttributes attrs = SftpFileAttributesBuilder.create()
                .withType(SftpFileAttributes.SSH_FILEXFER_TYPE_REGULAR)
                .withArchive(true)
                .build();
        assertTrue(attrs.isArchive());
    }

    @Test
    public void testCompressedFlag() {
        SftpFileAttributes attrs = SftpFileAttributesBuilder.create()
                .withType(SftpFileAttributes.SSH_FILEXFER_TYPE_REGULAR)
                .withCompressed(true)
                .build();
        assertTrue(attrs.isCompressed());
    }

    @Test
    public void testEncryptedFlag() {
        SftpFileAttributes attrs = SftpFileAttributesBuilder.create()
                .withType(SftpFileAttributes.SSH_FILEXFER_TYPE_REGULAR)
                .withEncrypted(true)
                .build();
        assertTrue(attrs.isEncrypted());
    }

    @Test
    public void testImmutableFlag() {
        SftpFileAttributes attrs = SftpFileAttributesBuilder.create()
                .withType(SftpFileAttributes.SSH_FILEXFER_TYPE_REGULAR)
                .withImmutable(true)
                .build();
        assertTrue(attrs.isImmutable());
    }

    @Test
    public void testAppendOnlyFlag() {
        SftpFileAttributes attrs = SftpFileAttributesBuilder.create()
                .withType(SftpFileAttributes.SSH_FILEXFER_TYPE_REGULAR)
                .withAppendOnly(true)
                .build();
        assertTrue(attrs.isAppendOnly());
    }

    @Test
    public void testSparseFlag() {
        SftpFileAttributes attrs = SftpFileAttributesBuilder.create()
                .withType(SftpFileAttributes.SSH_FILEXFER_TYPE_REGULAR)
                .withSparse(true)
                .build();
        assertTrue(attrs.isSparse());
    }

    @Test
    public void testSyncFlag() {
        SftpFileAttributes attrs = SftpFileAttributesBuilder.create()
                .withType(SftpFileAttributes.SSH_FILEXFER_TYPE_REGULAR)
                .withSync(true)
                .build();
        assertTrue(attrs.isSync());
    }

    @Test
    public void testFlagsDefaultToFalse() {
        SftpFileAttributes attrs = SftpFileAttributesBuilder.create()
                .withType(SftpFileAttributes.SSH_FILEXFER_TYPE_REGULAR)
                .build();
        assertFalse(attrs.isReadOnly());
        assertFalse(attrs.isHidden());
        assertFalse(attrs.isArchive());
        assertFalse(attrs.isCompressed());
        assertFalse(attrs.isEncrypted());
        assertFalse(attrs.isImmutable());
        assertFalse(attrs.isAppendOnly());
        assertFalse(attrs.isSparse());
        assertFalse(attrs.isSync());
    }

    // -----------------------------------------------------------------------
    // toModeType
    // -----------------------------------------------------------------------

    @Test
    public void testToModeTypeForRegularFile() {
        SftpFileAttributes attrs = SftpFileAttributesBuilder.create()
                .withType(SftpFileAttributes.SSH_FILEXFER_TYPE_REGULAR)
                .build();
        assertEquals(SftpFileAttributes.S_IFREG, attrs.toModeType());
    }

    @Test
    public void testToModeTypeForDirectory() {
        SftpFileAttributes attrs = SftpFileAttributesBuilder.create()
                .withType(SftpFileAttributes.SSH_FILEXFER_TYPE_DIRECTORY)
                .build();
        assertEquals(SftpFileAttributes.S_IFDIR, attrs.toModeType());
    }

    @Test
    public void testToModeTypeForSymlink() {
        SftpFileAttributes attrs = SftpFileAttributesBuilder.create()
                .withType(SftpFileAttributes.SSH_FILEXFER_TYPE_SYMLINK)
                .build();
        assertEquals(SftpFileAttributes.S_IFLNK, attrs.toModeType());
    }

    @Test
    public void testToModeTypeForSocket() {
        SftpFileAttributes attrs = SftpFileAttributesBuilder.create()
                .withType(SftpFileAttributes.SSH_FILEXFER_TYPE_SOCKET)
                .build();
        assertEquals(SftpFileAttributes.S_IFSOCK, attrs.toModeType());
    }

    @Test
    public void testToModeTypeUnknown() {
        SftpFileAttributes attrs = SftpFileAttributesBuilder.create()
                .withType(SftpFileAttributes.SSH_FILEXFER_TYPE_UNKNOWN)
                .build();
        assertEquals(0, attrs.toModeType());
    }

    // -----------------------------------------------------------------------
    // type() accessor
    // -----------------------------------------------------------------------

    @Test
    public void testTypeAccessor() {
        SftpFileAttributes attrs = SftpFileAttributesBuilder.create()
                .withType(SftpFileAttributes.SSH_FILEXFER_TYPE_DIRECTORY)
                .build();
        assertEquals(SftpFileAttributes.SSH_FILEXFER_TYPE_DIRECTORY, attrs.type());
    }

    // -----------------------------------------------------------------------
    // createWith (copy)
    // -----------------------------------------------------------------------

    @Test
    public void testCreateWithCopy() {
        SftpFileAttributes original = SftpFileAttributesBuilder.create()
                .withType(SftpFileAttributes.SSH_FILEXFER_TYPE_REGULAR)
                .withSize(999L)
                .withUsername("copyuser")
                .build();
        SftpFileAttributes copy = SftpFileAttributesBuilder.createWith(original).build();
        assertEquals(original.type(), copy.type());
        assertEquals(999L, copy.size().longValue());
        assertEquals("copyuser", copy.username());
    }

    // -----------------------------------------------------------------------
    // Serialization round-trip (toByteArray / fromPacket)
    // -----------------------------------------------------------------------

    @Test
    public void testByteArrayRoundTripV4() throws IOException {
        SftpFileAttributes original = SftpFileAttributesBuilder.ofType(
                        SftpFileAttributes.SSH_FILEXFER_TYPE_REGULAR, "UTF-8")
                .withSize(4096L)
                .withUsername("testuser")
                .withGroup("testgroup")
                .withPermissions(
                        PosixFilePermission.OWNER_READ,
                        PosixFilePermission.OWNER_WRITE,
                        PosixFilePermission.GROUP_READ)
                .withLastModifiedTime(1_700_000_000_000L)
                .withLastAccessTime(1_600_000_000_000L)
                .build();

        byte[] bytes = original.toByteArray(4);
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);

        ByteArrayReader bar = new ByteArrayReader(bytes);
        SftpFileAttributes restored = SftpFileAttributesBuilder.of(bar, 4, "UTF-8").build();

        assertEquals(original.type(), restored.type());
        assertEquals(4096L, restored.size().longValue());
        assertTrue(restored.isFile());
    }

    @Test
    public void testByteArrayRoundTripV3() throws IOException {
        SftpFileAttributes original = SftpFileAttributesBuilder.create()
                .asVersion(3)
                .withSize(1024L)
                .withUid(500)
                .withGid(500)
                .withPermissions(
                        PosixFilePermission.OWNER_READ,
                        PosixFilePermission.OWNER_WRITE,
                        PosixFilePermission.OWNER_EXECUTE)
                .build();

        byte[] bytes = original.toByteArray(3);
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);

        ByteArrayReader bar = new ByteArrayReader(bytes);
        SftpFileAttributes restored = SftpFileAttributesBuilder.of(bar, 3, "UTF-8").build();

        assertEquals(1024L, restored.size().longValue());
        assertEquals(500, restored.uid());
        assertEquals(500, restored.gid());
    }

    // -----------------------------------------------------------------------
    // hasXxx predicates
    // -----------------------------------------------------------------------

    @Test
    public void testHasSize() {
        SftpFileAttributes withSize = SftpFileAttributesBuilder.create().withSize(100L).build();
        assertTrue(withSize.hasSize());

        SftpFileAttributes noSize = SftpFileAttributesBuilder.create().build();
        assertFalse(noSize.hasSize());
    }

    @Test
    public void testHasPermissions() {
        SftpFileAttributes withPerm = SftpFileAttributesBuilder.create()
                .withPermissions(PosixFilePermission.OWNER_READ).build();
        assertTrue(withPerm.hasPermissions());

        SftpFileAttributes noPerm = SftpFileAttributesBuilder.create().build();
        assertFalse(noPerm.hasPermissions());
    }

    @Test
    public void testHasUid() {
        SftpFileAttributes withUid = SftpFileAttributesBuilder.create().withUid(1).build();
        assertTrue(withUid.hasUid());

        SftpFileAttributes noUid = SftpFileAttributesBuilder.create().build();
        assertFalse(noUid.hasUid());
    }

    @Test
    public void testHasUsername() {
        SftpFileAttributes withName = SftpFileAttributesBuilder.create().withUsername("x").build();
        assertTrue(withName.hasUsername());

        SftpFileAttributes noName = SftpFileAttributesBuilder.create().build();
        assertFalse(noName.hasUsername());
    }
}

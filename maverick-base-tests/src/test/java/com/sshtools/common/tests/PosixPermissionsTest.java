package com.sshtools.common.tests;

/*-
 * #%L
 * Base API Tests
 * %%
 * Copyright (C) 2002 - 2024 JADAPTIVE Limited
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

import java.nio.file.attribute.PosixFilePermission;
import java.util.Arrays;
import java.util.Set;

import com.sshtools.common.sftp.SftpFileAttributes;
import com.sshtools.common.sftp.PosixPermissions.PosixPermissionsBuilder;
import com.sshtools.common.util.UnsignedInteger32;

import junit.framework.TestCase;

public class PosixPermissionsTest extends TestCase {

	public void testBadFileModeString() {
		try {
			PosixPermissionsBuilder.create().
				fromFileModeString("rw-").
				build();
			fail("Expected exception.");
		}
		catch(IllegalArgumentException iae) {
			assertEquals("Invalid mode", iae.getMessage());
		}
	}
	
	public void testMaskStringTooShort() {
		try {
			PosixPermissionsBuilder.create().
				fromMaskString("XXX").
				build();
			fail("Expected exception.");
		}
		catch(IllegalArgumentException iae) {
			assertEquals("Mask length must be 4", iae.getMessage());
		}
	}
	public void testUmaskStringTooShort() {
		try {
			PosixPermissionsBuilder.create().
				fromUmaskString("XXX").
				build();
			fail("Expected exception.");
		}
		catch(IllegalArgumentException iae) {
			assertEquals("Mask length must be 4", iae.getMessage());
		}
	}
	
	public void testBadMaskString() {
		try {
			PosixPermissionsBuilder.create().
				fromMaskString("9999").
				build();
			fail("Expected exception.");
		}
		catch(IllegalArgumentException iae) {
			assertEquals("Mask must be 4 digit octal number.", iae.getMessage());
		}
	}
	
	public void testBadUmaskString() {
		try {
			PosixPermissionsBuilder.create().
				fromUmaskString("9999").
				build();
			fail("Expected exception.");
		}
		catch(IllegalArgumentException iae) {
			assertEquals("Mask must be 4 digit octal number.", iae.getMessage());
		}
	}
	
	public void testFromFileModeString() {
		var perm = PosixPermissionsBuilder.create().
				fromFileModeString("rw-rw-rw-").
				build();
		
		assertEquals("rw-rw-rw-", perm.asFileModesString());
		assertEquals("0666", perm.asMaskString());
		assertEquals(438, perm.asInt());
		assertEquals(438, perm.asLong());
		assertEquals(new UnsignedInteger32(438), perm.asUInt32());
		assertEquals(Set.of(
				PosixFilePermission.OWNER_READ,
				PosixFilePermission.OWNER_WRITE,
				PosixFilePermission.GROUP_READ,
				PosixFilePermission.GROUP_WRITE,
				PosixFilePermission.OTHERS_READ,
				PosixFilePermission.OTHERS_WRITE
				), perm.asPermissions());
		assertEquals(true, perm.has(
				PosixFilePermission.OWNER_READ,
				PosixFilePermission.OWNER_WRITE,
				PosixFilePermission.GROUP_READ,
				PosixFilePermission.GROUP_WRITE,
				PosixFilePermission.OTHERS_READ,
				PosixFilePermission.OTHERS_WRITE
				));
		assertEquals(false, perm.has(
				PosixFilePermission.OWNER_EXECUTE,
				PosixFilePermission.GROUP_EXECUTE,
				PosixFilePermission.OTHERS_EXECUTE
				));
		
	}
	
	public void testAllRead() {
		var perm = PosixPermissionsBuilder.create().
				withAllRead().
				build();
		
		assertEquals("r--r--r--", perm.asFileModesString());
		assertEquals("0444", perm.asMaskString());
		assertEquals(292, perm.asInt());
		assertEquals(292, perm.asLong());
		assertEquals(new UnsignedInteger32(292), perm.asUInt32());
		assertEquals(Set.of(
				PosixFilePermission.OWNER_READ,
				PosixFilePermission.GROUP_READ,
				PosixFilePermission.OTHERS_READ
				), perm.asPermissions());
		assertEquals(true, perm.has(
				PosixFilePermission.OWNER_READ,
				PosixFilePermission.GROUP_READ,
				PosixFilePermission.OTHERS_READ
				));
		assertEquals(false, perm.has(
				PosixFilePermission.OWNER_EXECUTE,
				PosixFilePermission.OWNER_WRITE,
				PosixFilePermission.GROUP_EXECUTE,
				PosixFilePermission.GROUP_WRITE,
				PosixFilePermission.OTHERS_WRITE,
				PosixFilePermission.OTHERS_EXECUTE
				));
		
	}
	
	public void testAllWrite() {
		var perm = PosixPermissionsBuilder.create().
				withAllWrite().
				build();
		
		assertEquals("-w--w--w-", perm.asFileModesString());
		assertEquals("0222", perm.asMaskString());
		assertEquals(146, perm.asInt());
		assertEquals(146, perm.asLong());
		assertEquals(new UnsignedInteger32(146), perm.asUInt32());
		assertEquals(Set.of(
				PosixFilePermission.OWNER_WRITE,
				PosixFilePermission.GROUP_WRITE,
				PosixFilePermission.OTHERS_WRITE
				), perm.asPermissions());
		assertEquals(true, perm.has(
				PosixFilePermission.OWNER_WRITE,
				PosixFilePermission.GROUP_WRITE,
				PosixFilePermission.OTHERS_WRITE
				));
		assertEquals(false, perm.has(
				PosixFilePermission.OWNER_EXECUTE,
				PosixFilePermission.OWNER_READ,
				PosixFilePermission.GROUP_EXECUTE,
				PosixFilePermission.GROUP_READ,
				PosixFilePermission.OTHERS_READ,
				PosixFilePermission.OTHERS_EXECUTE
				));
		
	}
	
	public void testAllExecut() {
		var perm = PosixPermissionsBuilder.create().
				withAllExecute().
				build();
		
		assertEquals("--x--x--x", perm.asFileModesString());
		assertEquals("0111", perm.asMaskString());
		assertEquals(73, perm.asInt());
		assertEquals(73, perm.asLong());
		assertEquals(new UnsignedInteger32(73), perm.asUInt32());
		assertEquals(Set.of(
				PosixFilePermission.OWNER_EXECUTE,
				PosixFilePermission.GROUP_EXECUTE,
				PosixFilePermission.OTHERS_EXECUTE
				), perm.asPermissions());
		assertEquals(true, perm.has(
				PosixFilePermission.OWNER_EXECUTE,
				PosixFilePermission.GROUP_EXECUTE,
				PosixFilePermission.OTHERS_EXECUTE
				));
		assertEquals(false, perm.has(
				PosixFilePermission.OWNER_WRITE,
				PosixFilePermission.OWNER_READ,
				PosixFilePermission.GROUP_WRITE,
				PosixFilePermission.GROUP_READ,
				PosixFilePermission.OTHERS_READ,
				PosixFilePermission.OTHERS_WRITE
				));
		
	}
	
	public void testLaxFromFileModeString() {
		var perm = PosixPermissionsBuilder.create().
				fromLaxFileModeString("r").
				build();
		
		assertEquals("r--------", perm.asFileModesString());
		assertEquals("0400", perm.asMaskString());
		assertEquals(256, perm.asInt());
		assertEquals(256, perm.asLong());
		assertEquals(new UnsignedInteger32(256), perm.asUInt32());
		assertEquals(Set.of(
				PosixFilePermission.OWNER_READ
				), perm.asPermissions());
		assertEquals(true, perm.has(
				PosixFilePermission.OWNER_READ
				));
		assertEquals(false, perm.has(
				PosixFilePermission.OWNER_WRITE,
				PosixFilePermission.OWNER_EXECUTE,
				PosixFilePermission.GROUP_READ,
				PosixFilePermission.GROUP_WRITE,
				PosixFilePermission.GROUP_EXECUTE,
				PosixFilePermission.OTHERS_READ,
				PosixFilePermission.OTHERS_WRITE,
				PosixFilePermission.OTHERS_EXECUTE
				));
		
	}
	
	public void testWithoutOther() {
		var perm = PosixPermissionsBuilder.create().
				fromFileModeString("rw-rw-rw-").
				withoutOtherPermissions().
				build();
		
		assertEquals("rw-rw----", perm.asFileModesString());
		assertEquals("0660", perm.asMaskString());
		assertEquals(432, perm.asInt());
		assertEquals(432, perm.asLong());
		assertEquals(new UnsignedInteger32(432), perm.asUInt32());
		assertEquals(Set.of(
				PosixFilePermission.OWNER_READ,
				PosixFilePermission.OWNER_WRITE,
				PosixFilePermission.GROUP_READ,
				PosixFilePermission.GROUP_WRITE
				), perm.asPermissions());
		assertEquals(true, perm.has(
				PosixFilePermission.OWNER_READ,
				PosixFilePermission.OWNER_WRITE,
				PosixFilePermission.GROUP_READ,
				PosixFilePermission.GROUP_WRITE
				));
		assertEquals(false, perm.has(
				PosixFilePermission.OWNER_EXECUTE,
				PosixFilePermission.GROUP_EXECUTE,
				PosixFilePermission.OTHERS_READ,
				PosixFilePermission.OTHERS_WRITE,
				PosixFilePermission.OTHERS_EXECUTE
				));
		
	}
	
	public void testWithoutBitmaskFlags() {
		var perm = PosixPermissionsBuilder.create().
				fromFileModeString("rwxrw-rw-").
				withoutBitmaskFlags(
						SftpFileAttributes.S_IXUSR
				).
				build();
		
		assertEquals("rw-rw-rw-", perm.asFileModesString());
		assertEquals("0666", perm.asMaskString());
		assertEquals(438, perm.asInt());
		assertEquals(438, perm.asLong());
		assertEquals(new UnsignedInteger32(438), perm.asUInt32());
		assertEquals(Set.of(
				PosixFilePermission.OWNER_READ,
				PosixFilePermission.OWNER_WRITE,
				PosixFilePermission.GROUP_READ,
				PosixFilePermission.GROUP_WRITE,
				PosixFilePermission.OTHERS_READ,
				PosixFilePermission.OTHERS_WRITE
				), perm.asPermissions());
		assertEquals(true, perm.has(
				PosixFilePermission.OWNER_READ,
				PosixFilePermission.OWNER_WRITE,
				PosixFilePermission.GROUP_READ,
				PosixFilePermission.GROUP_WRITE,
				PosixFilePermission.OTHERS_READ,
				PosixFilePermission.OTHERS_WRITE
				));
		assertEquals(false, perm.has(
				PosixFilePermission.OWNER_EXECUTE,
				PosixFilePermission.GROUP_EXECUTE,
				PosixFilePermission.OTHERS_EXECUTE
				));
		
	}
	
	public void testPosixFilePermissionSet() {
		var perm = PosixPermissionsBuilder.create().
				fromPermissions(
					PosixFilePermission.OWNER_READ,
					PosixFilePermission.GROUP_READ,
					PosixFilePermission.OTHERS_READ
				).
				build();
		
		assertEquals("r--r--r--", perm.asFileModesString());
		assertEquals("0444", perm.asMaskString());
		assertEquals(292, perm.asInt());
		assertEquals(292, perm.asLong());
		assertEquals(new UnsignedInteger32(292), perm.asUInt32());
		assertEquals(Set.of(
				PosixFilePermission.OWNER_READ,
				PosixFilePermission.GROUP_READ,
				PosixFilePermission.OTHERS_READ
				), perm.asPermissions());
		assertEquals(true, perm.has(
				PosixFilePermission.OWNER_READ,
				PosixFilePermission.GROUP_READ,
				PosixFilePermission.OTHERS_READ
				));
		assertEquals(false, perm.has(
				PosixFilePermission.OWNER_WRITE,
				PosixFilePermission.GROUP_WRITE,
				PosixFilePermission.OTHERS_WRITE,
				PosixFilePermission.OWNER_EXECUTE,
				PosixFilePermission.GROUP_EXECUTE,
				PosixFilePermission.OTHERS_EXECUTE
				));
		
	}

	public void testFromMaskString() {
		var perm = PosixPermissionsBuilder.create().
				fromMaskString("0755").
				build();
		
		assertEquals("rwxr-xr-x", perm.asFileModesString());
		assertEquals("0755", perm.asMaskString());
		assertEquals(493, perm.asInt());
		assertEquals(493, perm.asLong());
		assertEquals(new UnsignedInteger32(493), perm.asUInt32());
		assertEquals(Set.of(
				PosixFilePermission.OWNER_READ,
				PosixFilePermission.OWNER_WRITE,
				PosixFilePermission.OWNER_EXECUTE,
				PosixFilePermission.GROUP_READ,
				PosixFilePermission.GROUP_EXECUTE,
				PosixFilePermission.OTHERS_READ,
				PosixFilePermission.OTHERS_EXECUTE
				), perm.asPermissions());
		assertEquals(true, perm.has(
				PosixFilePermission.OWNER_READ,
				PosixFilePermission.OWNER_WRITE,
				PosixFilePermission.OWNER_EXECUTE,
				PosixFilePermission.GROUP_READ,
				PosixFilePermission.GROUP_EXECUTE,
				PosixFilePermission.OTHERS_READ,
				PosixFilePermission.OTHERS_EXECUTE
				));
		assertEquals(false, perm.has(
				PosixFilePermission.GROUP_WRITE,
				PosixFilePermission.OTHERS_WRITE
				));
		
	}

	public void testFromUmaskString() {
		var perm = PosixPermissionsBuilder.create().
				fromUmaskString("0022").
				build();
		
		assertEquals("rwxr-xr-x", perm.asFileModesString());
		assertEquals("0755", perm.asMaskString());
		assertEquals(493, perm.asInt());
		assertEquals(493, perm.asLong());
		assertEquals(new UnsignedInteger32(493), perm.asUInt32());
		assertEquals(Set.of(
				PosixFilePermission.OWNER_READ,
				PosixFilePermission.OWNER_WRITE,
				PosixFilePermission.OWNER_EXECUTE,
				PosixFilePermission.GROUP_READ,
				PosixFilePermission.GROUP_EXECUTE,
				PosixFilePermission.OTHERS_READ,
				PosixFilePermission.OTHERS_EXECUTE
				), perm.asPermissions());
		assertEquals(true, perm.has(
				PosixFilePermission.OWNER_READ,
				PosixFilePermission.OWNER_WRITE,
				PosixFilePermission.OWNER_EXECUTE,
				PosixFilePermission.GROUP_READ,
				PosixFilePermission.GROUP_EXECUTE,
				PosixFilePermission.OTHERS_READ,
				PosixFilePermission.OTHERS_EXECUTE
				));
		assertEquals(false, perm.has(
				PosixFilePermission.GROUP_WRITE,
				PosixFilePermission.OTHERS_WRITE
				));
		
	}

	public void testPosixPermissions() {
		var sourcePerm = PosixPermissionsBuilder.create().
				fromMaskString("0700").
				build();

		var perm = PosixPermissionsBuilder.create().
				fromPosixPermissions(sourcePerm).
				build();
		
		assertEquals("rwx------", perm.asFileModesString());
		assertEquals("0700", perm.asMaskString());
		assertEquals(448, perm.asInt());
		assertEquals(448, perm.asLong());
		assertEquals(new UnsignedInteger32(448), perm.asUInt32());
		assertEquals(Set.of(
				PosixFilePermission.OWNER_READ,
				PosixFilePermission.OWNER_WRITE,
				PosixFilePermission.OWNER_EXECUTE
				), perm.asPermissions());
		assertEquals(true, perm.has(
				PosixFilePermission.OWNER_READ,
				PosixFilePermission.OWNER_WRITE,
				PosixFilePermission.OWNER_EXECUTE
				));
		assertEquals(false, perm.has(
				PosixFilePermission.GROUP_READ,
				PosixFilePermission.GROUP_EXECUTE,
				PosixFilePermission.GROUP_WRITE,
				PosixFilePermission.OTHERS_READ,
				PosixFilePermission.OTHERS_WRITE,
				PosixFilePermission.OTHERS_EXECUTE
				));
		
	}

	public void testFromBitmask() {
		var perm = PosixPermissionsBuilder.create().
				fromBitmask(
						SftpFileAttributes.S_IRUSR |
						SftpFileAttributes.S_IWUSR |
						SftpFileAttributes.S_IXUSR
				).
				build();
		
		assertEquals("rwx------", perm.asFileModesString());
		assertEquals("0700", perm.asMaskString());
		assertEquals(448, perm.asInt());
		assertEquals(448, perm.asLong());
		assertEquals(new UnsignedInteger32(448), perm.asUInt32());
		assertEquals(Set.of(
				PosixFilePermission.OWNER_READ,
				PosixFilePermission.OWNER_WRITE,
				PosixFilePermission.OWNER_EXECUTE
				), perm.asPermissions());
		assertEquals(true, perm.has(
				PosixFilePermission.OWNER_READ,
				PosixFilePermission.OWNER_WRITE,
				PosixFilePermission.OWNER_EXECUTE
				));
		assertEquals(false, perm.has(
				PosixFilePermission.GROUP_READ,
				PosixFilePermission.GROUP_EXECUTE,
				PosixFilePermission.GROUP_WRITE,
				PosixFilePermission.OTHERS_READ,
				PosixFilePermission.OTHERS_WRITE,
				PosixFilePermission.OTHERS_EXECUTE
				));
		
	}

	public void testWithoutWrite() {
		var perm = PosixPermissionsBuilder.create().
				fromMaskString("0777").
				withoutWritePermissions().
				build();
		
		assertEquals("r-xr-xr-x", perm.asFileModesString());
		assertEquals("0555", perm.asMaskString());
		assertEquals(365, perm.asInt());
		assertEquals(365, perm.asLong());
		assertEquals(new UnsignedInteger32(365), perm.asUInt32());
		assertEquals(Set.of(
				PosixFilePermission.OWNER_READ,
				PosixFilePermission.OWNER_EXECUTE,
				PosixFilePermission.GROUP_READ,
				PosixFilePermission.GROUP_EXECUTE,
				PosixFilePermission.OTHERS_READ,
				PosixFilePermission.OTHERS_EXECUTE
				), perm.asPermissions());
		assertEquals(true, perm.has(
				PosixFilePermission.OWNER_READ,
				PosixFilePermission.OWNER_EXECUTE,
				PosixFilePermission.GROUP_READ,
				PosixFilePermission.GROUP_EXECUTE,
				PosixFilePermission.OTHERS_READ,
				PosixFilePermission.OTHERS_EXECUTE
				));
		assertEquals(false, perm.has(
				PosixFilePermission.OWNER_WRITE,
				PosixFilePermission.GROUP_WRITE,
				PosixFilePermission.OTHERS_WRITE
				));
		
	}

	public void testWithoutExecute() {
		var perm = PosixPermissionsBuilder.create().
				fromMaskString("0777").
				withoutExecutePermissions().
				build();
		
		assertEquals("rw-rw-rw-", perm.asFileModesString());
		assertEquals("0666", perm.asMaskString());
		assertEquals(438, perm.asInt());
		assertEquals(438, perm.asLong());
		assertEquals(new UnsignedInteger32(438), perm.asUInt32());
		assertEquals(Set.of(
				PosixFilePermission.OWNER_READ,
				PosixFilePermission.OWNER_WRITE,
				PosixFilePermission.GROUP_READ,
				PosixFilePermission.GROUP_WRITE,
				PosixFilePermission.OTHERS_READ,
				PosixFilePermission.OTHERS_WRITE
				), perm.asPermissions());
		assertEquals(true, perm.has(
				PosixFilePermission.OWNER_READ,
				PosixFilePermission.OWNER_WRITE,
				PosixFilePermission.GROUP_READ,
				PosixFilePermission.GROUP_WRITE,
				PosixFilePermission.OTHERS_READ,
				PosixFilePermission.OTHERS_WRITE
				));
		assertEquals(false, perm.has(
				PosixFilePermission.OWNER_EXECUTE,
				PosixFilePermission.GROUP_EXECUTE,
				PosixFilePermission.OTHERS_EXECUTE
				));
		
	}

	public void testWithBitmask() {
		var perm = PosixPermissionsBuilder.create().
				fromFileModeString("rwx------").
				withBitmaskFlags(
						SftpFileAttributes.S_IRGRP |
						SftpFileAttributes.S_IWGRP |
						SftpFileAttributes.S_IXGRP
				).
				build();
		
		assertEquals("rwxrwx---", perm.asFileModesString());
		assertEquals("0770", perm.asMaskString());
		assertEquals(504, perm.asInt());
		assertEquals(504, perm.asLong());
		assertEquals(new UnsignedInteger32(504), perm.asUInt32());
		assertEquals(Set.of(
				PosixFilePermission.OWNER_READ,
				PosixFilePermission.OWNER_WRITE,
				PosixFilePermission.OWNER_EXECUTE,
				PosixFilePermission.GROUP_READ,
				PosixFilePermission.GROUP_WRITE,
				PosixFilePermission.GROUP_EXECUTE
				), perm.asPermissions());
		assertEquals(true, perm.has(
				PosixFilePermission.OWNER_READ,
				PosixFilePermission.OWNER_WRITE,
				PosixFilePermission.OWNER_EXECUTE,
				PosixFilePermission.GROUP_READ,
				PosixFilePermission.GROUP_WRITE,
				PosixFilePermission.GROUP_EXECUTE
				));
		assertEquals(false, perm.has(
				PosixFilePermission.OTHERS_READ,
				PosixFilePermission.OTHERS_WRITE,
				PosixFilePermission.OTHERS_EXECUTE
				));
		
	}

	public void testWithPermissions() {
		var perm = PosixPermissionsBuilder.create().
				fromFileModeString("r--------").
				withPermissions(PosixFilePermission.OWNER_WRITE).
				build();
		
		assertEquals("rw-------", perm.asFileModesString());
		assertEquals("0600", perm.asMaskString());
		assertEquals(384, perm.asInt());
		assertEquals(384, perm.asLong());
		assertEquals(new UnsignedInteger32(384), perm.asUInt32());
		assertEquals(Set.of(
				PosixFilePermission.OWNER_READ,
				PosixFilePermission.OWNER_WRITE
				), perm.asPermissions());
		assertEquals(true, perm.has(
				PosixFilePermission.OWNER_READ,
				PosixFilePermission.OWNER_WRITE
				));
		assertEquals(false, perm.has(
				PosixFilePermission.OWNER_EXECUTE,
				PosixFilePermission.GROUP_READ,
				PosixFilePermission.GROUP_WRITE,
				PosixFilePermission.GROUP_EXECUTE,
				PosixFilePermission.OTHERS_READ,
				PosixFilePermission.OTHERS_WRITE,
				PosixFilePermission.OTHERS_EXECUTE
				));
		
	}

	public void testWithChmodArgument() {
		var perm = PosixPermissionsBuilder.create().
				withChmodArgumentString("u=rwx,g=rwx,o=rwx").
				build();
		
		assertEquals("rwxrwxrwx", perm.asFileModesString());
		assertEquals("0777", perm.asMaskString());
		assertEquals(511, perm.asInt());
		assertEquals(511, perm.asLong());
		assertEquals(new UnsignedInteger32(511), perm.asUInt32());
		assertEquals(Set.of(
				PosixFilePermission.OWNER_READ,
				PosixFilePermission.OWNER_WRITE,
				PosixFilePermission.OWNER_EXECUTE,
				PosixFilePermission.GROUP_READ,
				PosixFilePermission.GROUP_WRITE,
				PosixFilePermission.GROUP_EXECUTE,
				PosixFilePermission.OTHERS_READ,
				PosixFilePermission.OTHERS_WRITE,
				PosixFilePermission.OTHERS_EXECUTE
				), perm.asPermissions());
		assertEquals(true, perm.has(
				PosixFilePermission.OWNER_READ,
				PosixFilePermission.OWNER_WRITE,
				PosixFilePermission.OWNER_EXECUTE,
				PosixFilePermission.GROUP_READ,
				PosixFilePermission.GROUP_WRITE,
				PosixFilePermission.GROUP_EXECUTE,
				PosixFilePermission.OTHERS_READ,
				PosixFilePermission.OTHERS_WRITE,
				PosixFilePermission.OTHERS_EXECUTE
				));
		
	}

	public void testWithChmodArgumentAdd() {
		var perm = PosixPermissionsBuilder.create().
				fromAllPermissions().
				withChmodArgumentString("u-rwx,g-rwx,o-rwx").
				build();
		
		assertEquals("---------", perm.asFileModesString());
		assertEquals("0000", perm.asMaskString());
		assertEquals(0, perm.asInt());
		assertEquals(0, perm.asLong());
		assertEquals(new UnsignedInteger32(0), perm.asUInt32());
		assertEquals(Set.of(), perm.asPermissions());
		assertEquals(false, perm.has(
				PosixFilePermission.OWNER_READ,
				PosixFilePermission.OWNER_WRITE,
				PosixFilePermission.GROUP_READ,
				PosixFilePermission.GROUP_WRITE,
				PosixFilePermission.OTHERS_READ,
				PosixFilePermission.OTHERS_WRITE,
				PosixFilePermission.OWNER_EXECUTE,
				PosixFilePermission.GROUP_EXECUTE,
				PosixFilePermission.OTHERS_EXECUTE
				));
		
	}

	public void testBadChmodScope() {
		try {
			PosixPermissionsBuilder.create().
				fromAllPermissions().
				withChmodArgumentString("ZZZZ").
				build();
			fail("Expected to fail");
		}
		catch(IllegalArgumentException iae) {
			assertEquals("Unknown scope 'ZZZZ'", iae.getMessage());
		}
	}

	public void testBadChmodUserMode() {
		try {
			PosixPermissionsBuilder.create().
				fromAllPermissions().
				withChmodArgumentString("u=QQQ").
				build();
			fail("Expected to fail");
		}
		catch(IllegalArgumentException iae) {
			assertEquals("Unknown user mode 'Q'", iae.getMessage());
		}
	}

	public void testBadChmodGroupMode() {
		try {
			PosixPermissionsBuilder.create().
				fromAllPermissions().
				withChmodArgumentString("g=QQQ").
				build();
			fail("Expected to fail");
		}
		catch(IllegalArgumentException iae) {
			assertEquals("Unknown group mode 'Q'", iae.getMessage());
		}
	}

	public void testBadChmodOtherMode() {
		try {
			PosixPermissionsBuilder.create().
				fromAllPermissions().
				withChmodArgumentString("o=QQQ").
				build();
			fail("Expected to fail");
		}
		catch(IllegalArgumentException iae) {
			assertEquals("Unknown others mode 'Q'", iae.getMessage());
		}
	}

	public void testWithPermissionsCollection() {
		var perm = PosixPermissionsBuilder.create().
				fromFileModeString("rw-------").
				withPermissions(Arrays.asList(
						PosixFilePermission.OWNER_EXECUTE,
						PosixFilePermission.GROUP_READ,
						PosixFilePermission.GROUP_WRITE,
						PosixFilePermission.GROUP_EXECUTE
					)).
				build();
		
		assertEquals("rwxrwx---", perm.asFileModesString());
		assertEquals("0770", perm.asMaskString());
		assertEquals(504, perm.asInt());
		assertEquals(504, perm.asLong());
		assertEquals(new UnsignedInteger32(504), perm.asUInt32());
		assertEquals(Set.of(
				PosixFilePermission.OWNER_READ,
				PosixFilePermission.OWNER_WRITE,
				PosixFilePermission.OWNER_EXECUTE,
				PosixFilePermission.GROUP_READ,
				PosixFilePermission.GROUP_WRITE,
				PosixFilePermission.GROUP_EXECUTE
				), perm.asPermissions());
		assertEquals(true, perm.has(
				PosixFilePermission.OWNER_READ,
				PosixFilePermission.OWNER_WRITE,
				PosixFilePermission.OWNER_EXECUTE,
				PosixFilePermission.GROUP_READ,
				PosixFilePermission.GROUP_WRITE,
				PosixFilePermission.GROUP_EXECUTE
				));
		assertEquals(false, perm.has(
				PosixFilePermission.OTHERS_READ,
				PosixFilePermission.OTHERS_WRITE,
				PosixFilePermission.OTHERS_EXECUTE
				));
		
	}

	public void testWithoutGroupOtherPermissions() {
		var perm = PosixPermissionsBuilder.create().
				fromFileModeString("r--r--r--").
				withoutGroupOtherPermissions().
				build();
		
		assertEquals("r--------", perm.asFileModesString());
		assertEquals("0400", perm.asMaskString());
		assertEquals(256, perm.asInt());
		assertEquals(256, perm.asLong());
		assertEquals(new UnsignedInteger32(256), perm.asUInt32());
		assertEquals(Set.of(
				PosixFilePermission.OWNER_READ
				), perm.asPermissions());
		assertEquals(true, perm.has(
				PosixFilePermission.OWNER_READ
				));
		assertEquals(false, perm.has(
				PosixFilePermission.OWNER_WRITE,
				PosixFilePermission.OWNER_EXECUTE,
				PosixFilePermission.GROUP_READ,
				PosixFilePermission.GROUP_WRITE,
				PosixFilePermission.GROUP_EXECUTE,
				PosixFilePermission.OTHERS_READ,
				PosixFilePermission.OTHERS_WRITE,
				PosixFilePermission.OTHERS_EXECUTE
				));
		
	}

	public void testWithoutPermissions() {
		var perm = PosixPermissionsBuilder.create().
				fromFileModeString("rw-rw-rw-").
				withoutPermissions(PosixFilePermission.GROUP_WRITE, PosixFilePermission.OTHERS_WRITE).
				build();
		
		assertEquals("rw-r--r--", perm.asFileModesString());
		assertEquals("0644", perm.asMaskString());
		assertEquals(420, perm.asInt());
		assertEquals(420, perm.asLong());
		assertEquals(new UnsignedInteger32(420), perm.asUInt32());
		assertEquals(Set.of(
				PosixFilePermission.OWNER_READ,
				PosixFilePermission.OWNER_WRITE,
				PosixFilePermission.GROUP_READ,
				PosixFilePermission.OTHERS_READ
				), perm.asPermissions());
		assertEquals(true, perm.has(
				PosixFilePermission.OWNER_READ,
				PosixFilePermission.OWNER_WRITE,
				PosixFilePermission.GROUP_READ,
				PosixFilePermission.OTHERS_READ
				));
		assertEquals(false, perm.has(
				PosixFilePermission.OWNER_EXECUTE,
				PosixFilePermission.GROUP_WRITE,
				PosixFilePermission.GROUP_EXECUTE,
				PosixFilePermission.OTHERS_WRITE,
				PosixFilePermission.OTHERS_EXECUTE
				));
		
	}

	public void testWithoutPermissionsCollection() {
		var perm = PosixPermissionsBuilder.create().
				fromFileModeString("rwxrwxrwx").
				withoutPermissions(Arrays.asList(
					PosixFilePermission.OWNER_EXECUTE,
					PosixFilePermission.GROUP_EXECUTE, 
					PosixFilePermission.OTHERS_EXECUTE
				)).
				build();
		
		assertEquals("rw-rw-rw-", perm.asFileModesString());
		assertEquals("0666", perm.asMaskString());
		assertEquals(438, perm.asInt());
		assertEquals(438, perm.asLong());
		assertEquals(new UnsignedInteger32(438), perm.asUInt32());
		assertEquals(Set.of(
				PosixFilePermission.OWNER_READ,
				PosixFilePermission.OWNER_WRITE,
				PosixFilePermission.GROUP_READ,
				PosixFilePermission.GROUP_WRITE,
				PosixFilePermission.OTHERS_READ,
				PosixFilePermission.OTHERS_WRITE
				), perm.asPermissions());
		assertEquals(true, perm.has(
				PosixFilePermission.OWNER_READ,
				PosixFilePermission.OWNER_WRITE,
				PosixFilePermission.GROUP_READ,
				PosixFilePermission.GROUP_WRITE,
				PosixFilePermission.OTHERS_READ,
				PosixFilePermission.OTHERS_WRITE
				));
		assertEquals(false, perm.has(
				PosixFilePermission.OWNER_EXECUTE,
				PosixFilePermission.GROUP_EXECUTE,
				PosixFilePermission.OTHERS_EXECUTE
				));
		
	}

	public void testNoPermissions() {
		var perm = PosixPermissionsBuilder.create().
				fromFileModeString("rwxrwxrwx").
				fromNoPermissions().
				build();
		
		assertEquals("---------", perm.asFileModesString());
		assertEquals("0000", perm.asMaskString());
		assertEquals(0, perm.asInt());
		assertEquals(0, perm.asLong());
		assertEquals(new UnsignedInteger32(0), perm.asUInt32());
		assertEquals(Set.of(), perm.asPermissions());
		assertEquals(true, perm.isEmpty());
		try {
			perm.has();
			fail("Expected to fail.");
		}
		catch(IllegalArgumentException iae) {
			assertEquals("Must provide at least one permission.", iae.getMessage());
		}
		assertEquals(false, perm.has(
				PosixFilePermission.OWNER_READ,
				PosixFilePermission.OWNER_EXECUTE,
				PosixFilePermission.OWNER_WRITE,
				PosixFilePermission.GROUP_READ,
				PosixFilePermission.GROUP_EXECUTE,
				PosixFilePermission.GROUP_WRITE,
				PosixFilePermission.OTHERS_READ,
				PosixFilePermission.OTHERS_WRITE,
				PosixFilePermission.OTHERS_EXECUTE
				));
		
	}

	public void testAllPermissions() {
		var perm = PosixPermissionsBuilder.create().
				fromAllPermissions().
				build();
		
		assertEquals("rwxrwxrwx", perm.asFileModesString());
		assertEquals("0777", perm.asMaskString());
		assertEquals(511, perm.asInt());
		assertEquals(511, perm.asLong());
		assertEquals(new UnsignedInteger32(511), perm.asUInt32());
		assertEquals(Set.of(
				PosixFilePermission.OWNER_READ,
				PosixFilePermission.OWNER_WRITE,
				PosixFilePermission.OWNER_EXECUTE,
				PosixFilePermission.GROUP_READ,
				PosixFilePermission.GROUP_WRITE,
				PosixFilePermission.GROUP_EXECUTE,
				PosixFilePermission.OTHERS_READ,
				PosixFilePermission.OTHERS_WRITE,
				PosixFilePermission.OTHERS_EXECUTE
				), perm.asPermissions());
		assertEquals(true, perm.has(
				PosixFilePermission.OWNER_READ,
				PosixFilePermission.OWNER_WRITE,
				PosixFilePermission.OWNER_EXECUTE,
				PosixFilePermission.GROUP_READ,
				PosixFilePermission.GROUP_WRITE,
				PosixFilePermission.GROUP_EXECUTE,
				PosixFilePermission.OTHERS_READ,
				PosixFilePermission.OTHERS_WRITE,
				PosixFilePermission.OTHERS_EXECUTE
				));
	}
}

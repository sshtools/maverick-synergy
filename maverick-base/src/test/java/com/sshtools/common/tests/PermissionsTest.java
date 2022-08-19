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
 * Copyright (C) 2002-2021 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
 */
package com.sshtools.common.tests;

import com.sshtools.common.permissions.Permissions;

import junit.framework.TestCase;

public class PermissionsTest extends TestCase {

	final int PERMISSION_ONE = 0x0000000F;
	final int PERMISSION_TWO = 0x000000F0;
	final int PERMISSION_THREE = 0x0000F00;
	final int PERMISSION_FOUR = 0x0000F000;
	
	final int ALL_PERMISSIONS = 0x0000FFFF;
	
	public void testEnablePermission() {
		Permissions perms=  new Permissions(0x00);
		
		assertFalse(perms.check(PERMISSION_FOUR));
		
		perms.add(PERMISSION_FOUR);
		
		assertEnable(perms);
		
	}
	
	private void assertEnable(Permissions perms) {
		
		assertFalse(perms.check(PERMISSION_ONE));
		assertFalse(perms.check(PERMISSION_TWO));
		assertFalse(perms.check(PERMISSION_THREE));
		
		assertTrue(perms.check(PERMISSION_FOUR));
	}

	public void testDisablePermission() {
		
		Permissions perms=  new Permissions(ALL_PERMISSIONS);
		
		assertTrue(perms.check(PERMISSION_FOUR));
		
		perms.remove(PERMISSION_FOUR);
		
		assertDisable(perms);
	}
	
	private void assertDisable(Permissions perms) {
		
		assertTrue(perms.check(PERMISSION_ONE));
		assertTrue(perms.check(PERMISSION_TWO));
		assertTrue(perms.check(PERMISSION_THREE));
		
		assertFalse(perms.check(PERMISSION_FOUR));
	}

	public void testEnableTwice() {
		
		Permissions perms=  new Permissions(0x00);
		perms.add(PERMISSION_FOUR);
		perms.add(PERMISSION_FOUR);
		
		assertEnable(perms);
	}
	
	public void testDisableTwice() {
		
		Permissions perms=  new Permissions(ALL_PERMISSIONS);
		perms.remove(PERMISSION_FOUR);
		perms.remove(PERMISSION_FOUR);
		
		assertDisable(perms);
		
	}
	
	public void testEnableDisableEnable() {
		Permissions perms=  new Permissions(0x00);
		perms.add(PERMISSION_FOUR);
		perms.remove(PERMISSION_FOUR);
		perms.add(PERMISSION_FOUR);
		
		assertEnable(perms);
		
	}
	
	public void testDisableEnableDisable() {
		
		Permissions perms=  new Permissions(ALL_PERMISSIONS);
		perms.remove(PERMISSION_FOUR);
		perms.add(PERMISSION_FOUR);
		perms.remove(PERMISSION_FOUR);
		
		assertDisable(perms);
		
	}
	
	
}

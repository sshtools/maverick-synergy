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

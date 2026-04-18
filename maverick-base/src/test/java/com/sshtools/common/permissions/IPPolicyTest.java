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
package com.sshtools.common.permissions;

/*-
 * #%L
 * Base API
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

/**
 * Test class for IPPolicy to verify IP address flagging, banning, 
 * whitelisting, and blacklisting functionality.
 */
public class IPPolicyTest {

	private IPPolicy policy;
	private InetAddress testAddress;
	private InetAddress localAddress;
	
	@Before
	public void setUp() throws UnknownHostException {
		policy = new IPPolicy();
		testAddress = new InetSocketAddress(InetAddress.getByName("192.168.1.100"), 22).getAddress();
		localAddress = new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 22).getAddress();
	}
	
	@Test
	public void testDefaultAllowConnection() {
		// By default, connections should be allowed
		assertTrue("Connection should be allowed by default", 
				policy.checkConnection(testAddress, localAddress));
	}
	
	@Test
	public void testStopAcceptingConnections() {
		// Stop accepting connections
		policy.stopAcceptingConnections();
		assertFalse("Connection should be denied after stopping connections", 
				policy.checkConnection(testAddress, localAddress));
	}
	
	@Test
	public void testStartAcceptingConnections() {
		// Stop and then start accepting connections
		policy.stopAcceptingConnections();
		policy.startAcceptingConnections();
		assertTrue("Connection should be allowed after starting connections", 
				policy.checkConnection(testAddress, localAddress));
	}
	
	@Test
	public void testFlagAddressUnderThreshold() throws UnknownHostException {
		// Flag the address multiple times but under the threshold
		String ipAddress = "192.168.1.100";
		
		for (int i = 0; i < 10; i++) {
			policy.flagAddress(ipAddress);
		}
		
		// Connection should still be allowed as we're under the default threshold of 15
		assertTrue("Connection should be allowed when flags are under threshold", 
				policy.assertAllowed(testAddress, localAddress));
	}
	
	@Test
	public void testFlagAddressExceedsThreshold() throws UnknownHostException {
		// Flag the address to exceed the default threshold of 15
		String ipAddress = "192.168.1.100";
		
		for (int i = 0; i <= 15; i++) {
			policy.flagAddress(ipAddress);
		}
		
		// Connection should now be denied due to temporary ban
		assertFalse("Connection should be denied after exceeding flag threshold", 
				policy.assertAllowed(testAddress, localAddress));
	}
	
	@Test
	public void testFlagAddressWithInetAddress() throws UnknownHostException {
		// Test flagging using InetAddress directly
		InetAddress addr = InetAddress.getByName("192.168.1.100");
		
		for (int i = 0; i <= 15; i++) {
			policy.flagAddress(addr);
		}
		
		assertFalse("Connection should be denied after exceeding flag threshold", 
				policy.assertAllowed(testAddress, localAddress));
	}
	
	@Test
	public void testCustomThreshold() throws UnknownHostException {
		// Set a custom threshold of 5
		policy.setFailedAuthenticationCountThreshold(5);
		String ipAddress = "192.168.1.100";
		
		// Flag 5 times (should not be banned yet)
		for (int i = 0; i < 5; i++) {
			policy.flagAddress(ipAddress);
		}
		assertTrue("Connection should be allowed at threshold", 
				policy.assertAllowed(testAddress, localAddress));
		
		// Flag one more time to exceed threshold
		policy.flagAddress(ipAddress);
		assertFalse("Connection should be denied after exceeding custom threshold", 
				policy.assertAllowed(testAddress, localAddress));
	}
	
	@Test
	public void testDisableBanning() throws UnknownHostException {
		// Disable temporary banning
		policy.disableTemporaryBanning();
		String ipAddress = "192.168.1.100";
		
		// Flag many times
		for (int i = 0; i <= 20; i++) {
			policy.flagAddress(ipAddress);
		}
		
		// Connection should still be allowed when banning is disabled
		assertTrue("Connection should be allowed when banning is disabled", 
				policy.assertAllowed(testAddress, localAddress));
	}
	
	@Test
	public void testEnableBanning() throws UnknownHostException {
		// Disable then re-enable banning
		policy.disableTemporaryBanning();
		policy.enableTemporaryBanning();
		String ipAddress = "192.168.1.100";
		
		// Flag to exceed threshold
		for (int i = 0; i <= 15; i++) {
			policy.flagAddress(ipAddress);
		}
		
		// Connection should be denied
		assertFalse("Connection should be denied when banning is re-enabled", 
				policy.assertAllowed(testAddress, localAddress));
	}
	
	@Test
	public void testBlacklist() throws UnknownHostException {
		// Blacklist the test address
		policy.blacklist("192.168.1.100");
		
		assertFalse("Connection should be denied for blacklisted IP", 
				policy.assertAllowed(testAddress, localAddress));
	}
	
	@Test
	public void testBlacklistCIDR() throws UnknownHostException {
		// Blacklist an entire subnet
		policy.blacklist("192.168.1.0/24");
		
		assertFalse("Connection should be denied for IP in blacklisted subnet", 
				policy.assertAllowed(testAddress, localAddress));
	}
	
	@Test
	public void testWhitelist() throws UnknownHostException {
		// Add to whitelist
		policy.whitelist("192.168.1.100");
		
		assertTrue("Connection should be allowed for whitelisted IP", 
				policy.assertAllowed(testAddress, localAddress));
	}
	
	@Test
	public void testWhitelistOverridesFlags() throws UnknownHostException {
		// Whitelist the address first
		policy.whitelist("192.168.1.100");
		
		// Flag many times to exceed threshold
		String ipAddress = "192.168.1.100";
		for (int i = 0; i <= 20; i++) {
			policy.flagAddress(ipAddress);
		}
		
		// Connection should still be denied even though whitelisted,
		// because temporary banning overrides whitelist
		assertFalse("Temporary ban should override whitelist", 
				policy.assertAllowed(testAddress, localAddress));
	}
	
	@Test
	public void testWhitelistWithBlacklist() throws UnknownHostException {
		// Add to whitelist and blacklist
		policy.whitelist("192.168.1.100");
		policy.blacklist("192.168.1.100");
		
		// Blacklist should take precedence
		assertFalse("Connection should be denied when IP is both whitelisted and blacklisted", 
				policy.assertAllowed(testAddress, localAddress));
	}
	
	@Test
	public void testWhitelistOnlyMode() throws UnknownHostException {
		// Whitelist only specific IPs
		policy.whitelist("192.168.1.100");
		
		// Test whitelisted address
		assertTrue("Whitelisted IP should be allowed", 
				policy.assertAllowed(testAddress, localAddress));
		
		// Test non-whitelisted address
		InetSocketAddress otherAddress = new InetSocketAddress(
				InetAddress.getByName("192.168.1.101"), 22);
		assertFalse("Non-whitelisted IP should be denied in whitelist-only mode", 
				policy.assertAllowed(otherAddress.getAddress(), localAddress));
	}
	
	@Test
	public void testMultipleIPsFlagging() throws UnknownHostException {
		// Flag different IPs independently
		String ip1 = "192.168.1.100";
		String ip2 = "192.168.1.101";
		
		// Flag ip1 to exceed threshold
		for (int i = 0; i <= 15; i++) {
			policy.flagAddress(ip1);
		}
		
		// Flag ip2 but not to threshold
		for (int i = 0; i < 10; i++) {
			policy.flagAddress(ip2);
		}
		
		// ip1 should be banned
		assertFalse("First IP should be banned", 
				policy.assertAllowed(testAddress, localAddress));
		
		// ip2 should still be allowed
		InetSocketAddress address2 = new InetSocketAddress(
				InetAddress.getByName(ip2), 22);
		assertTrue("Second IP should still be allowed", 
				policy.assertAllowed(address2.getAddress(), localAddress));
	}
	
	@Test
	public void testSetTemporaryBanTime() {
		// Set temporary ban time to 10 minutes
		policy.setTemporaryBanTime(10);
		
		assertEquals("Temporary ban time should be 10 minutes in milliseconds", 
				TimeUnit.MINUTES.toMillis(10), policy.getTemporaryBanTime());
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testSetTemporaryBanTimeInvalid() {
		// Setting ban time to 0 or less should throw exception
		policy.setTemporaryBanTime(0);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testSetTemporaryBanTimeNegative() {
		// Setting negative ban time should throw exception
		policy.setTemporaryBanTime(-5);
	}
	
	@Test
	public void testSetFailedAuthenticationThresholdPeriod() {
		// Set threshold period to 10 minutes
		policy.setFailedAuthenticationThresholdPeriod(10, TimeUnit.MINUTES);
		
		// This should reset the flagged address counts
		// We can verify this indirectly by flagging and checking behavior
		String ipAddress = "192.168.1.100";
		for (int i = 0; i <= 15; i++) {
			policy.flagAddress(ipAddress);
		}
		
		assertFalse("IP should be banned after exceeding threshold", 
				policy.assertAllowed(testAddress, localAddress));
	}
	
	@Test
	public void testGettersAndSetters() throws UnknownHostException {
		// Test blacklist getter/setter
		IPStore customBlacklist = new IPStore();
		customBlacklist.add("10.0.0.1");
		policy.setBlacklist(customBlacklist);
		assertEquals("Blacklist should match set value", 
				customBlacklist, policy.getBlacklist());
		
		// Test whitelist getter/setter
		IPStore customWhitelist = new IPStore();
		customWhitelist.add("10.0.0.2");
		policy.setWhitelist(customWhitelist);
		assertEquals("Whitelist should match set value", 
				customWhitelist, policy.getWhitelist());
	}
	
	@Test
	public void testFlagAddressStringWithInvalidIP() {
		try {
			policy.flagAddress("invalid.ip.address");
			fail("Should throw IllegalStateException for invalid IP address");
		} catch (IllegalStateException e) {
			// Expected exception
			assertNotNull("Exception should have a cause", e.getCause());
			assertTrue("Cause should be UnknownHostException", 
					e.getCause() instanceof UnknownHostException);
		}
	}
	
	@Test
	public void testCheckConnectionMethod() {
		// Verify checkConnection delegates to assertConnection
		assertTrue("checkConnection should return true for allowed connection", 
				policy.checkConnection(testAddress, localAddress));
		
		policy.stopAcceptingConnections();
		assertFalse("checkConnection should return false for denied connection", 
				policy.checkConnection(testAddress, localAddress));
	}
	
	@Test
	public void testFlaggedAddressCountExpiry() throws InterruptedException, UnknownHostException {
		// Set a very short threshold period (1 second for testing)
		policy.setFailedAuthenticationThresholdPeriod(1, TimeUnit.SECONDS);
		policy.setFailedAuthenticationCountThreshold(5);
		
		String ipAddress = "192.168.1.100";
		
		// Flag the address 4 times (under threshold)
		for (int i = 0; i < 4; i++) {
			policy.flagAddress(ipAddress);
		}
		
		// Connection should still be allowed
		assertTrue("Connection should be allowed with 4 flags under threshold of 5", 
				policy.assertAllowed(testAddress, localAddress));
		
		// Wait for the expiry period to pass (1 second + buffer)
		Thread.sleep(1200);
		
		// Flag the address one more time - this should be counted as 1 flag,
		// not 5, because the previous flags should have expired
		policy.flagAddress(ipAddress);
		
		// Connection should still be allowed because the count was reset
		assertTrue("Connection should be allowed after flag count expiry - count should have reset to 1", 
				policy.assertAllowed(testAddress, localAddress));
		
		// Now flag 5 more times to reach threshold
		for (int i = 0; i < 5; i++) {
			policy.flagAddress(ipAddress);
		}
		
		// Now it should be banned
		assertFalse("Connection should be denied after reaching threshold with fresh flags", 
				policy.assertAllowed(testAddress, localAddress));
	}
	
	@Test(timeout = 5000)
	public void testTemporaryBanExpiry() throws InterruptedException, UnknownHostException {
		// Use the new API with TimeUnit.SECONDS for a 2 second ban time
		policy.setTemporaryBanTime(2, TimeUnit.SECONDS);
		policy.setFailedAuthenticationCountThreshold(3);
		
		String ipAddress = "192.168.1.100";
		InetSocketAddress shortTestAddress = new InetSocketAddress(InetAddress.getByName(ipAddress), 22);
		
		// Flag to exceed threshold and trigger ban
		for (int i = 0; i <= 3; i++) {
			policy.flagAddress(ipAddress);
		}
		
		// Connection should be denied due to ban
		assertFalse("Connection should be denied due to temporary ban", 
				policy.assertAllowed(shortTestAddress.getAddress(), localAddress));
		
		// Wait for the temporary ban to expire (2 seconds + buffer)
		Thread.sleep(2200);
		
		// Connection should now be allowed again
		assertTrue("Connection should be allowed after temporary ban expires", 
				policy.assertAllowed(shortTestAddress.getAddress(), localAddress));
	}
	
	@Test
	public void testSetTemporaryBanTimeWithTimeUnit() {
		// Test setting ban time with seconds
		policy.setTemporaryBanTime(30, TimeUnit.SECONDS);
		assertEquals("Temporary ban time should be 30 seconds in milliseconds", 
				TimeUnit.SECONDS.toMillis(30), policy.getTemporaryBanTime());
		
		// Test setting ban time with hours
		policy.setTemporaryBanTime(2, TimeUnit.HOURS);
		assertEquals("Temporary ban time should be 2 hours in milliseconds", 
				TimeUnit.HOURS.toMillis(2), policy.getTemporaryBanTime());
		
		// Test setting ban time with milliseconds
		policy.setTemporaryBanTime(5000, TimeUnit.MILLISECONDS);
		assertEquals("Temporary ban time should be 5000 milliseconds", 
				5000L, policy.getTemporaryBanTime());
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testSetTemporaryBanTimeWithTimeUnitInvalid() {
		policy.setTemporaryBanTime(0, TimeUnit.SECONDS);
	}
	
	@Test
	public void testBackwardCompatibilityOfSetTemporaryBanTime() {
		// Test that the old API (minutes only) still works
		policy.setTemporaryBanTime(5);
		assertEquals("Temporary ban time should be 5 minutes in milliseconds", 
				TimeUnit.MINUTES.toMillis(5), policy.getTemporaryBanTime());
	}
}

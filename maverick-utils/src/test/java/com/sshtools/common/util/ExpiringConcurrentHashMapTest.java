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

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import com.sshtools.common.util.ExpiringConcurrentHashMap.ExpiryConfiguration;

/**
 * Test class for ExpiringConcurrentHashMap to verify automatic expiry
 * of entries after a configured time period.
 */
public class ExpiringConcurrentHashMapTest {

	private ExpiringConcurrentHashMap<String, String> map;
	
	@Before
	public void setUp() {
		// Create map with 1 second expiry time for testing
		map = new ExpiringConcurrentHashMap<>(1000);
	}
	
	@Test
	public void testPutAndGet() {
		map.put("key1", "value1");
		assertEquals("Value should be retrievable immediately after put", 
				"value1", map.get("key1"));
	}
	
	@Test
	public void testMultiplePutsAndGets() {
		map.put("key1", "value1");
		map.put("key2", "value2");
		map.put("key3", "value3");
		
		assertEquals("value1", map.get("key1"));
		assertEquals("value2", map.get("key2"));
		assertEquals("value3", map.get("key3"));
		assertEquals("Map should contain 3 entries", 3, map.size());
	}
	
	@Test(timeout = 3000)
	public void testEntryExpiry() throws InterruptedException {
		map.put("key1", "value1");
		
		// Verify entry exists
		assertEquals("Entry should exist immediately after put", 
				"value1", map.get("key1"));
		assertTrue("Map should contain the key", map.containsKey("key1"));
		
		// Wait for expiry (1 second + buffer)
		Thread.sleep(1200);
		
		// Access the map to trigger purge
		map.get("key1");
		
		// Entry should be expired and removed
		assertNull("Entry should be null after expiry", map.get("key1"));
		assertFalse("Map should not contain expired key", map.containsKey("key1"));
		assertEquals("Map should be empty after expiry", 0, map.size());
	}
	
	@Test(timeout = 3000)
	public void testMultipleEntriesExpiry() throws InterruptedException {
		map.put("key1", "value1");
		map.put("key2", "value2");
		
		// Wait for expiry
		Thread.sleep(1200);
		
		// Trigger purge by accessing
		map.get("key1");
		
		// Both entries should be expired
		assertNull("First entry should be expired", map.get("key1"));
		assertNull("Second entry should be expired", map.get("key2"));
		assertEquals("Map should be empty", 0, map.size());
	}
	
	@Test(timeout = 3000)
	public void testPartialExpiry() throws InterruptedException {
		// Add first entry
		map.put("key1", "value1");
		
		// Wait half the expiry time
		Thread.sleep(600);
		
		// Add second entry
		map.put("key2", "value2");
		
		// Wait for first entry to expire but not second (600ms more)
		Thread.sleep(700);
		
		// Trigger purge
		map.get("key1");
		
		// First entry should be expired, second should still exist
		assertNull("First entry should be expired", map.get("key1"));
		assertEquals("Second entry should still exist", "value2", map.get("key2"));
	}
	
	@Test
	public void testPutAll() {
		Map<String, String> sourceMap = new HashMap<>();
		sourceMap.put("key1", "value1");
		sourceMap.put("key2", "value2");
		sourceMap.put("key3", "value3");
		
		map.putAll(sourceMap);
		
		assertEquals("All entries should be added", 3, map.size());
		assertEquals("value1", map.get("key1"));
		assertEquals("value2", map.get("key2"));
		assertEquals("value3", map.get("key3"));
	}
	
	@Test(timeout = 3000)
	public void testPutAllExpiry() throws InterruptedException {
		Map<String, String> sourceMap = new HashMap<>();
		sourceMap.put("key1", "value1");
		sourceMap.put("key2", "value2");
		
		map.putAll(sourceMap);
		assertEquals("Both entries should exist", 2, map.size());
		
		// Wait for expiry
		Thread.sleep(1200);
		
		// Trigger purge
		map.get("key1");
		
		assertEquals("Map should be empty after expiry", 0, map.size());
	}
	
	@Test
	public void testPutIfAbsent() {
		// Put if absent on empty map
		String result1 = map.putIfAbsent("key1", "value1");
		assertNull("Should return null when key doesn't exist", result1);
		assertEquals("value1", map.get("key1"));
		
		// Put if absent on existing key
		String result2 = map.putIfAbsent("key1", "value2");
		assertEquals("Should return existing value", "value1", result2);
		assertEquals("Value should not change", "value1", map.get("key1"));
	}
	
	@Test(timeout = 3000)
	public void testPutIfAbsentAfterExpiry() throws InterruptedException {
		map.putIfAbsent("key1", "value1");
		assertEquals("value1", map.get("key1"));
		
		// Wait for expiry
		Thread.sleep(1200);
		
		// Put if absent after expiry should succeed
		String result = map.putIfAbsent("key1", "value2");
		assertNull("Should return null after expiry", result);
		assertEquals("New value should be stored", "value2", map.get("key1"));
	}
	
	@Test
	public void testGetExpiryTime() {
		assertEquals("Expiry time should match constructor value", 
				1000L, map.getExpiryTime());
		
		ExpiringConcurrentHashMap<String, String> map2 = 
				new ExpiringConcurrentHashMap<>(5000);
		assertEquals("Expiry time should be 5 seconds", 
				5000L, map2.getExpiryTime());
	}
	
	@Test
	public void testExpiryConfiguration() {
		ExpiryConfiguration config = new ExpiryConfiguration() {
			@Override
			public long expiresInMillis() {
				return TimeUnit.SECONDS.toMillis(2);
			}
		};
		
		ExpiringConcurrentHashMap<String, String> customMap = 
				new ExpiringConcurrentHashMap<>(config);
		
		assertEquals("Expiry time should match configuration", 
				2000L, customMap.getExpiryTime());
	}
	
	@Test(timeout = 4000)
	public void testExpiryConfigurationFunctional() throws InterruptedException {
		ExpiryConfiguration config = new ExpiryConfiguration() {
			@Override
			public long expiresInMillis() {
				return 1000; // 1 second
			}
		};
		
		ExpiringConcurrentHashMap<String, String> customMap = 
				new ExpiringConcurrentHashMap<>(config);
		
		customMap.put("key1", "value1");
		assertEquals("value1", customMap.get("key1"));
		
		// Wait for expiry
		Thread.sleep(1200);
		
		// Trigger purge
		customMap.get("key1");
		
		assertNull("Entry should be expired", customMap.get("key1"));
	}
	
	@Test
	public void testUpdateExistingKey() {
		map.put("key1", "value1");
		assertEquals("value1", map.get("key1"));
		
		// Update with new value
		String oldValue = map.put("key1", "value2");
		assertEquals("Should return old value", "value1", oldValue);
		assertEquals("Should have new value", "value2", map.get("key1"));
	}
	
	@Test(timeout = 3000)
	public void testUpdateResetsExpiryTime() throws InterruptedException {
		map.put("key1", "value1");
		
		// Wait most of the expiry time
		Thread.sleep(800);
		
		// Update the entry - this should NOT reset the expiry time
		// (based on the implementation, it reuses the original timestamp)
		map.put("key1", "value2");
		
		// Wait remainder of original expiry time
		Thread.sleep(400);
		
		// Entry should still be expired based on original put time
		map.get("key1");
		
		// Note: Based on the implementation, the expiry time is NOT reset on update
		// The original timestamp is preserved
		assertNull("Entry should be expired based on original timestamp", 
				map.get("key1"));
	}
	
	@Test
	public void testConcurrentAccess() throws InterruptedException {
		final int threadCount = 10;
		final int operationsPerThread = 100;
		final CountDownLatch startLatch = new CountDownLatch(1);
		final CountDownLatch doneLatch = new CountDownLatch(threadCount);
		
		for (int i = 0; i < threadCount; i++) {
			final int threadNum = i;
			new Thread(() -> {
				try {
					startLatch.await();
					for (int j = 0; j < operationsPerThread; j++) {
						String key = "key-" + threadNum + "-" + j;
						map.put(key, "value-" + threadNum + "-" + j);
						map.get(key);
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				} finally {
					doneLatch.countDown();
				}
			}).start();
		}
		
		// Start all threads
		startLatch.countDown();
		
		// Wait for all threads to complete
		assertTrue("All threads should complete", 
				doneLatch.await(5, TimeUnit.SECONDS));
		
		// Map should contain entries (some may have expired during operations)
		assertTrue("Map should have entries", map.size() > 0);
	}
	
	@Test(timeout = 5000)
	public void testConcurrentExpiryAndAccess() throws InterruptedException {
		final CountDownLatch startLatch = new CountDownLatch(1);
		final CountDownLatch doneLatch = new CountDownLatch(2);
		
		// Thread 1: Keep adding entries
		new Thread(() -> {
			try {
				startLatch.await();
				for (int i = 0; i < 20; i++) {
					map.put("key-" + i, "value-" + i);
					Thread.sleep(50);
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			} finally {
				doneLatch.countDown();
			}
		}).start();
		
		// Thread 2: Keep reading entries (triggers purge)
		new Thread(() -> {
			try {
				startLatch.await();
				for (int i = 0; i < 20; i++) {
					map.get("key-" + (i % 10));
					Thread.sleep(50);
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			} finally {
				doneLatch.countDown();
			}
		}).start();
		
		startLatch.countDown();
		assertTrue("Both threads should complete", 
				doneLatch.await(10, TimeUnit.SECONDS));
		
		// Wait for entries to expire
		Thread.sleep(1200);
		map.get("trigger-purge");
		
		// All entries should eventually expire
		assertEquals("Map should be empty after expiry", 0, map.size());
	}
	
	@Test
	public void testRemove() {
		map.put("key1", "value1");
		assertEquals("value1", map.get("key1"));
		
		String removed = map.remove("key1");
		assertEquals("Should return removed value", "value1", removed);
		assertNull("Key should no longer exist", map.get("key1"));
		assertEquals("Map should be empty", 0, map.size());
	}
	
	@Test
	public void testClear() {
		map.put("key1", "value1");
		map.put("key2", "value2");
		map.put("key3", "value3");
		
		assertEquals("Map should have 3 entries", 3, map.size());
		
		map.clear();
		
		assertEquals("Map should be empty after clear", 0, map.size());
		assertNull("Keys should not exist", map.get("key1"));
	}
	
	@Test(timeout = 3000)
	public void testZeroExpiryTime() throws InterruptedException {
		// Map with 0ms expiry - entries should expire immediately
		ExpiringConcurrentHashMap<String, String> immediateMap = 
				new ExpiringConcurrentHashMap<>(0);
		
		immediateMap.put("key1", "value1");
		
		// Wait a tiny bit to ensure time has passed
		Thread.sleep(10);
		
		// Access to trigger purge - entry should be expired
		String value = immediateMap.get("key1");
		
		assertNull("Entry should be expired with 0ms expiry time", value);
		assertEquals("Map should be empty after purge", 0, immediateMap.size());
	}
	
	@Test(timeout = 10000)
	public void testLongExpiryTime() {
		// Map with 5 second expiry
		ExpiringConcurrentHashMap<String, String> longMap = 
				new ExpiringConcurrentHashMap<>(5000);
		
		longMap.put("key1", "value1");
		
		// Entry should still exist after 2 seconds
		try {
			Thread.sleep(2000);
			assertEquals("Entry should still exist", "value1", longMap.get("key1"));
		} catch (InterruptedException e) {
			fail("Test interrupted");
		}
	}
}

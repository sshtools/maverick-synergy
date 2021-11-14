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

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ExpiringConcurrentHashMap<K,V> extends ConcurrentHashMap<K,V> {

	private static final long serialVersionUID = 4825825094828550762L;

	private Map<K, Long> entryTime = new ConcurrentHashMap<K, Long>();
	
    private ExpiryConfiguration expiryConfig;
    
    public ExpiringConcurrentHashMap(long expiryInMillis) {
    	this.expiryConfig = new ExpiryConfiguration() {
			
			@Override
			public long expiresInMillis() {
				return expiryInMillis;
			}
		};
    }
    
    public long getExpiryTime() {
    	return this.expiryConfig.expiresInMillis();
    }
    
    public ExpiringConcurrentHashMap(ExpiryConfiguration expiryConfig) {
    	this.expiryConfig = expiryConfig;
    }

    @Override
    public V put(K key, V value) {
        purgeEntries();
        return doPut(key, value);
    }

    private V doPut(K key, V value) {
    	Long date = entryTime.getOrDefault(key, new Long(System.currentTimeMillis()));
        entryTime.put(key, date);
        V returnVal = super.put(key, value);
        return returnVal;
	}

	@Override
    public void putAll(Map<? extends K, ? extends V> m) {
		purgeEntries();
        for (K key : m.keySet()) {
            doPut(key, m.get(key));
        }
    }

    @Override
    public V putIfAbsent(K key, V value) {
    	purgeEntries();
        if (!containsKey(key)) {
            return doPut(key, value);
        } else {
            return get(key);
        }
    }
    
    @Override
	public V get(Object key) {
    	purgeEntries();
		return super.get(key);
	}

	private void purgeEntries() {
        long currentTime = new Date().getTime();
        for (K key : entryTime.keySet()) {
            if (currentTime > (entryTime.get(key) + expiryConfig.expiresInMillis())) {
                remove(key);
                entryTime.remove(key);
            }
        }
    }
	
	public interface ExpiryConfiguration {
		long expiresInMillis();
	}
}

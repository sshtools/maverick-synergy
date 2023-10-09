package com.sshtools.common.util;

/*-
 * #%L
 * Utils
 * %%
 * Copyright (C) 2002 - 2023 JADAPTIVE Limited
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
    	Long date = entryTime.getOrDefault(key, Long.valueOf(System.currentTimeMillis()));
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

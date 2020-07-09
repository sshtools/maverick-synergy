package com.sshtools.common.util;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ExpiringConcurrentHashMap<K,V> extends ConcurrentHashMap<K,V> {

	private static final long serialVersionUID = 4825825094828550762L;

	private Map<K, Long> entryTime = new ConcurrentHashMap<K, Long>();
	
    private long expiryInMillis;
    
    public ExpiringConcurrentHashMap(long expiryInMillis) {
    	this.expiryInMillis = expiryInMillis;
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
            if (currentTime > (entryTime.get(key) + expiryInMillis)) {
                remove(key);
                entryTime.remove(key);
            }
        }
    }
}

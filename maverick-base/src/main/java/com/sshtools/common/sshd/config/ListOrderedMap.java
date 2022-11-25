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
package com.sshtools.common.sshd.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ListOrderedMap<K, V> extends HashMap<K, V>{

	private static final long serialVersionUID = 9106126973132147223L;

	Map<K,V> contents = new LinkedHashMap<>();
	List<K> index = new ArrayList<>();
	
	@Override
	public int size() {
		return contents.size();
	}

	@Override
	public boolean isEmpty() {
		return contents.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return contents.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return contents.containsValue(value);
	}

	@Override
	public V get(Object key) {
		return contents.get(key);
	}

	@Override
	public V put(K key, V value) {
		if(contents.containsKey(key)) {
			return contents.put(key, value);
		}
		index.add(key);
		return contents.put(key, value);
	}

	@Override
	public V remove(Object key) {
		index.remove(key);
		return contents.remove(key);
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		m.forEach((key,value)-> {
			put(key, value);
		});
	}

	@Override
	public void clear() {
		index.clear();
		contents.clear();
	}

	@Override
	public Set<K> keySet() {
		return contents.keySet();
	}

	@Override
	public Collection<V> values() {
		return contents.values();
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		return contents.entrySet();
	}

	@Override
	public boolean equals(Object o) {
		return false;
	}

	@Override
	public int hashCode() {
		return contents.hashCode();
	}

	public V getValue(int i) {
		return contents.get(index.get(i));
	}

	public int indexOf(String key) {
		return index.indexOf(key);
	}

	public void put(int i, K key, V value) {
		if(index.contains(key)) {
			index.remove(key);
			contents.remove(key);
		}
		index.add(i, key);
		contents.put(key, value);
	}

	public void removeIndex(int i) {
		contents.remove(index.remove(i));
	}

}

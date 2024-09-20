package com.sshtools.common.sshd.config;

/*-
 * #%L
 * Base API
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

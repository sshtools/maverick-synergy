/*
 * (c) 2002-2023 JADAPTIVE Limited. All Rights Reserved.
 *
 * This file is part of the Maverick Synergy Java SSH API.
 *
 * Maverick Synergy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Maverick Synergy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.fuse.fs;

import java.util.List;

import com.sshtools.common.util.ExpiringConcurrentHashMap;

public class CachingFileTree {

	static long TIMEOUT = 60000 * 60;
	
	ExpiringConcurrentHashMap<String,List<String>> parentFolders = new ExpiringConcurrentHashMap<>(TIMEOUT);
	
	public CachingFileTree() {
	}
	
	public void clearCache(String path) {
		parentFolders.remove(path);
	}
	
	public void cache(String parent, List<String> children) {
		parentFolders.put(parent, children);
	}
	
	public List<String> getCache(String parent) {
		return parentFolders.get(parent);
	}

}

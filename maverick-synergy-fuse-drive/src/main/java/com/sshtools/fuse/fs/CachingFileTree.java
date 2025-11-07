package com.sshtools.fuse.fs;

/*-
 * #%L
 * Fuse Drive
 * %%
 * Copyright (C) 2002 - 2025 JADAPTIVE Limited
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

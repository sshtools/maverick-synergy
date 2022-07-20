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

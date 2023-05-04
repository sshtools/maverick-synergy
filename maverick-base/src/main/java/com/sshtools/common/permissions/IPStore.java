package com.sshtools.common.permissions;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.sshtools.common.net.CIDRNetwork;

public class IPStore {

	ConcurrentLinkedQueue<CIDRNetwork> entries = new ConcurrentLinkedQueue<>();
	
	public boolean isEmpty() {
		return entries.isEmpty();
	}

	public Collection<CIDRNetwork> getIPs() {
		return entries;
	}

	public void add(String ip) throws UnknownHostException {
		entries.add(new CIDRNetwork(ip));
	}
	
	public void reset(Collection<String> ips) throws UnknownHostException {
		
		Collection<CIDRNetwork> tmp = new ArrayList<>();
		for(String ip : ips) {
			tmp.add(new CIDRNetwork(ip));
		}
		this.entries.clear();
		this.entries.addAll(tmp);
	}
}

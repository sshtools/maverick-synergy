/**
 * (c) 2002-2021 JADAPTIVE Limited. All Rights Reserved.
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

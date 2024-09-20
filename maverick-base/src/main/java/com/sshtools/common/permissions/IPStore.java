package com.sshtools.common.permissions;

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

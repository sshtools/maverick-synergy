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

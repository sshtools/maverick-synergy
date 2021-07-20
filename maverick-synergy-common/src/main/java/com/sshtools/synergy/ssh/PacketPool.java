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

package com.sshtools.synergy.ssh;

import java.io.IOException;
import java.util.Vector;

import com.sshtools.common.ssh.Packet;

public class PacketPool {

	Vector<Packet> packets = new Vector<Packet>();

	static PacketPool instance;

	public static PacketPool getInstance() {
		return (instance == null ? instance = new PacketPool() : instance);
	}

	public Packet getPacket() throws IOException {
		synchronized (packets) {
			if (packets.size() == 0)
				return new Packet();
			Packet p = (Packet) packets.remove(0);
			return p;
		}
	}
	
	public void putPacket(Packet p) {
		synchronized (packets) {
			p.reset();
			packets.add(p);
		}
	}
}

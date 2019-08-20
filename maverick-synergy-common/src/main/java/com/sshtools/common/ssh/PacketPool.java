/**
 * (c) 2002-2019 JADAPTIVE Limited. All Rights Reserved.
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
package com.sshtools.common.ssh;

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

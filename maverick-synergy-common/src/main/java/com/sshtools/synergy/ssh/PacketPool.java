
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

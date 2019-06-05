package com.sshtools.common.ssh;

import java.io.IOException;
import java.util.Vector;

public class ByteArrays{

	Vector<byte[]> packets = new Vector<byte[]>();

	static ByteArrays instance;

	public static ByteArrays getInstance() {
		return (instance == null ? instance = new ByteArrays() : instance);
	}

	public byte[] getByteArray() throws IOException {
		synchronized (packets) {
			if (packets.size() == 0)
				return new byte[131072];
			return packets.remove(0);
		}
	}
	
	public void releaseByteArray(byte[] p) {
		synchronized (packets) {
			packets.add(p);
		}
	}
}

package com.sshtools.synergy.ssh;

/*-
 * #%L
 * Common API
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

import java.io.IOException;
import java.util.Vector;

@Deprecated
public class ByteArrays{

	Vector<byte[]> packets = new Vector<byte[]>();
	final int size;
	static ByteArrays instance;

	public static ByteArrays getInstance(int size) {
		return (instance == null ? instance = new ByteArrays(size) : instance);
	}
	
	public ByteArrays(int size) {
		this.size = size;
	}

	public byte[] getByteArray() throws IOException {
		synchronized (packets) {
			if (packets.size() == 0)
				return new byte[size];
		return packets.remove(0);
		}
	}
	
	public void releaseByteArray(byte[] p) {
		synchronized (packets) {
			packets.add(p);
		}
	}

}

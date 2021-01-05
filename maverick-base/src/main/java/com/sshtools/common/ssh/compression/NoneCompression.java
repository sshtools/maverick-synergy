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
package com.sshtools.common.ssh.compression;

import java.io.IOException;


public class NoneCompression implements SshCompression {

	public void init(int type, int level) {
	}

	public byte[] compress(byte[] data, int start, int len) throws IOException {
		return uncompress(data, start, len);
	}

	public byte[] uncompress(byte[] data, int start, int len)
			throws IOException {
		if(len != data.length || start != 0) {
			byte[] arr = new byte[len];
			System.arraycopy(data, start, arr, 0, len);
			return arr;
		}
		else
			return data;
	}

	public String getAlgorithm() {
		return "none";
	}

}

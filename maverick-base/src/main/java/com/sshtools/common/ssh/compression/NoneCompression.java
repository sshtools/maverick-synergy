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
 * along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.common.ssh.compression;

import java.io.IOException;


public class NoneCompression implements SshCompression {

	public void init(int type, int level) {
		// TODO Auto-generated method stub

	}

	public byte[] compress(byte[] data, int start, int len) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	public byte[] uncompress(byte[] data, int start, int len)
			throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	public String getAlgorithm() {
		// TODO Auto-generated method stub
		return null;
	}

}

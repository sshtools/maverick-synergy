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
package com.sshtools.common.ssh.components;

import com.sshtools.common.ssh.SshException;

public class NoneHmac implements SshHmac {

	public int getMacSize() {
		return 0;
	}

	public int getMacLength() {
		return 0;
	}
	
	public void generate(long sequenceNo, byte[] data, int offset, int len,
			byte[] output, int start) {
	}

	public void init(byte[] keydata) throws SshException {
	}

	public boolean verify(long sequenceNo, byte[] data, int start, int len,
			byte[] mac, int offset) {
		return true;
	}

	public void update(byte[] b) {
	}

	public byte[] doFinal() {

		return new byte[0];
	}

	public String getAlgorithm() {
		return "none";
	}

	public boolean isETM() {
		return false;
	}
}

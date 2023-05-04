/*
 * (c) 2002-2023 JADAPTIVE Limited. All Rights Reserved.
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
package com.sshtools.common.ssh.components.jce;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import com.sshtools.common.ssh.components.SshHmacFactory;

/**
 * SHA-1 message authentication implementation.
 * @author Lee David Painter
 *
 */
public class HmacSha256_96 extends HmacSha256 {
	
	public static class HmacSha256_96Factory implements SshHmacFactory<HmacSha256_96> {
		@Override
		public HmacSha256_96 create() throws NoSuchAlgorithmException, IOException {
			return new HmacSha256_96();
		}

		@Override
		public String[] getKeys() {
			return new String[] {  "hmac-sha2-256-96" };
		}
	}

	public HmacSha256_96() {
		super(12);
	}
	
	public String getAlgorithm() {
		return "hmac-sha2-256-96";
	}
}

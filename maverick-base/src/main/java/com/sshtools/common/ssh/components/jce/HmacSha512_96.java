package com.sshtools.common.ssh.components.jce;

/*-
 * #%L
 * Base API
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
import java.security.NoSuchAlgorithmException;

import com.sshtools.common.ssh.components.SshHmacFactory;

/**
 * SHA-1 message authentication implementation.
 * @author Lee David Painter
 *
 */
public class HmacSha512_96 extends HmacSha512 {
	public static class HmacSha512_96Factory implements SshHmacFactory<HmacSha512_96> {
		@Override
		public HmacSha512_96 create() throws NoSuchAlgorithmException, IOException {
			return new HmacSha512_96();
		}

		@Override
		public String[] getKeys() {
			return new String[] { "hmac-sha2-512-96" };
		}
	}

	public HmacSha512_96() {
		super(12);
	}
	
	public String getAlgorithm() {
		return "hmac-sha2-512-96";
	}
}

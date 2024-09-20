package com.sshtools.common.ssh.compression;

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


public class NoneCompression implements SshCompression {

	private static final String NONE = "none";

	public static class NoneCompressionFactory implements SshCompressionFactory<NoneCompression> {

		@Override
		public NoneCompression create() throws NoSuchAlgorithmException, IOException {
			return new NoneCompression();
		}

		@Override
		public String[] getKeys() {
			return new String[] { NONE };
		}
	}
	
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
		return NONE;
	}

}

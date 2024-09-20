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

import com.sshtools.common.ssh.SecurityLevel;
import com.sshtools.common.ssh.components.SshHmacFactory;

/**
 * MD5 message authentication implementation.
 * @author Lee David Painter
 *
 */
public class HmacMD5ETM extends AbstractHmac {
	
	private static final String ALGORITHM = "hmac-md5-etm@openssh.com";

	public static class HmacMD5ETMFactory implements SshHmacFactory<HmacMD5ETM> {
		@Override
		public HmacMD5ETM create() throws NoSuchAlgorithmException, IOException {
			return new HmacMD5ETM();
		}

		@Override
		public String[] getKeys() {
			return new String[] { ALGORITHM };
		}
	}

	public HmacMD5ETM() {
		super(JCEAlgorithms.JCE_HMACMD5, 16, SecurityLevel.WEAK, 1);
	}

	
	public String getAlgorithm() {
		return ALGORITHM;
	}
	
	@Override
	public boolean isETM() {
		return true;
	}

}

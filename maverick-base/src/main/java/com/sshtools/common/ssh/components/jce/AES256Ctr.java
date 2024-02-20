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
import com.sshtools.common.ssh.components.SshCipherFactory;

public class AES256Ctr extends AbstractJCECipher {

	private static final String CIPHER = "aes256-ctr";

	public static class AES256CtrFactory implements SshCipherFactory<AES256Ctr> {

		@Override
		public AES256Ctr create() throws NoSuchAlgorithmException, IOException {
			return new AES256Ctr();
		}

		@Override
		public String[] getKeys() {
			return new String[] { CIPHER };
		}
	}

	public AES256Ctr() throws IOException {
		super(JCEAlgorithms.JCE_AESCTRNOPADDING, "AES", 32, CIPHER, SecurityLevel.STRONG, 2002);
	}

}

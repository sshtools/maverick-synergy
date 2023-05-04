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
package com.sshtools.bcfips.tests;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import com.sshtools.common.ssh.components.jce.JCEProvider;
import com.sshtools.common.tests.AbstractCipherTests;

public class BCFIPSCipherTest extends AbstractCipherTests {

	@Override
	protected void setUp() {
		JCEProvider.enableBouncyCastle(true);
	}

	@Override
	protected String getTestingJCE() {
		return "BCFIPS";
	}

	@Override
	public void test3DESCBC() throws NoSuchAlgorithmException, IOException {
		// Not processed due to limit on number of bytes 3DES can process
	}

	@Override
	public void test3DESCTR() throws NoSuchAlgorithmException, IOException {
		// Not processed due to limit on number of bytes 3DES can process
	}
	
	
}

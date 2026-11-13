package com.sshtools.synergy.virtual.tests;

/*-
 * #%L
 * Virtual Connection Tests
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

import com.sshtools.client.PasswordAuthenticator;
import com.sshtools.client.SshClientContext;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.SshException;
import com.sshtools.server.SshServerContext;
import com.sshtools.synergy.nio.ConnectRequestFuture;
import com.sshtools.synergy.ssh.SshContext;

public class MlkemNistpHybridKexVirtualConnectionTests extends AbstractVirtualConnectionTests {

	private String preferredKex;

	@Override
	protected void configureClientContext(SshClientContext ctx) throws IOException, SshException {
		ctx.setUsername("admin");
		ctx.addAuthenticator(PasswordAuthenticator.forPassword("admin"));
		ctx.setPreferredKeyExchange(preferredKex);
	}

	@Override
	protected void configureServerContext(SshServerContext ctx) throws IOException, SshException {
		ctx.setPreferredKeyExchange(preferredKex);
	}

	public void testMlkem768Nistp256Handshake() throws Exception {
		assertHandshakeAndAuthentication(SshContext.KEX_MLKEM768_NISTP256_SHA256);
	}

	public void testMlkem1024Nistp384Handshake() throws Exception {
		assertHandshakeAndAuthentication(SshContext.KEX_MLKEM1024_NISTP384_SHA384);
	}

	private void assertHandshakeAndAuthentication(String kex) throws Exception {
		preferredKex = kex;
		ConnectRequestFuture future = connectVirtual();
		assertTrue("Virtual connection did not succeed for kex " + kex, future.isSuccess());
		SshConnection con = future.getConnection();
		con.getAuthenticatedFuture().waitForever();
		assertTrue("Authentication did not succeed for kex " + kex,
				con.getAuthenticatedFuture().isSuccess());
	}
}

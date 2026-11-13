/*-
 * #%L
 * BouncyCastle JCE Support
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

module com.sshtools.common.publickey.bc {
	requires com.sshtools.maverick.base;
	requires com.sshtools.synergy.client;
	requires com.sshtools.synergy.server;
	requires org.bouncycastle.pkix;
	requires org.bouncycastle.provider;

	provides com.sshtools.client.SshKeyExchangeClientFactory with
			com.sshtools.client.kex.bc.Sntrup761X25519SHA512DH.Sntrup761X25519SHA512DHFactory,
			com.sshtools.client.kex.bc.Sntrup761X25519SHA512DHatOpenSSHdotCOM.Sntrup761X25519SHA512DHatOpenSSHdotCOMFactory,
			com.sshtools.client.kex.bc.Mlkem768X25519SHA256.Mlkem768X25519SHA256Factory,
			com.sshtools.client.kex.bc.Mlkem768Nistp256SHA256.Mlkem768Nistp256SHA256Factory,
			com.sshtools.client.kex.bc.Mlkem1024Nistp384SHA384.Mlkem1024Nistp384SHA384Factory;

	provides com.sshtools.server.components.SshKeyExchangeServerFactory with
			com.sshtools.server.kex.bc.Sntrup761X25519SHA512DH.Sntrup761X25519SHA512DHFactory,
			com.sshtools.server.kex.bc.Sntrup761X25519SHA512DHatOpenSSHdotCOM.Sntrup761X25519SHA512DHatOpenSSHdotCOMFactory,
			com.sshtools.server.kex.bc.Mlkem768X25519SHA256.Mlkem768X25519SHA256Factory,
			com.sshtools.server.kex.bc.Mlkem768Nistp256SHA256.Mlkem768Nistp256SHA256Factory,
			com.sshtools.server.kex.bc.Mlkem1024Nistp384SHA384.Mlkem1024Nistp384SHA384Factory;
}

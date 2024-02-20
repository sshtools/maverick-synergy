/*-
 * #%L
 * Server API
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

import com.sshtools.common.command.ExecutableCommand.ExecutableCommandFactory;
import com.sshtools.server.components.SshKeyExchangeServerFactory;
import com.sshtools.server.components.jce.Curve25519SHA256LibSshServer;
import com.sshtools.server.components.jce.Curve25519SHA256Server;
import com.sshtools.server.components.jce.DiffieHellmanEcdhNistp256;
import com.sshtools.server.components.jce.DiffieHellmanEcdhNistp384;
import com.sshtools.server.components.jce.DiffieHellmanEcdhNistp521;
import com.sshtools.server.components.jce.DiffieHellmanGroup14Sha1JCE;
import com.sshtools.server.components.jce.DiffieHellmanGroup14Sha256JCE;
import com.sshtools.server.components.jce.DiffieHellmanGroup15Sha512JCE;
import com.sshtools.server.components.jce.DiffieHellmanGroup16Sha512JCE;
import com.sshtools.server.components.jce.DiffieHellmanGroup17Sha512JCE;
import com.sshtools.server.components.jce.DiffieHellmanGroup18Sha512JCE;
import com.sshtools.server.components.jce.DiffieHellmanGroup1Sha1JCE;
import com.sshtools.server.components.jce.DiffieHellmanGroupExchangeSha1JCE;
import com.sshtools.server.components.jce.DiffieHellmanGroupExchangeSha256JCE;
import com.sshtools.server.components.jce.Rsa1024SHA1KeyExchange;
import com.sshtools.server.components.jce.Rsa2048SHA2KeyExchange;

@SuppressWarnings("rawtypes")
module com.sshtools.synergy.server {
	requires transitive com.sshtools.maverick.base;
	requires transitive com.sshtools.synergy.common;
	exports com.sshtools.server;
	
	uses ExecutableCommandFactory;
	uses SshKeyExchangeServerFactory;
	provides SshKeyExchangeServerFactory with
			Curve25519SHA256LibSshServer.Curve25519SHA256LibSshServerFactory,
			Curve25519SHA256Server.Curve25519SHA256ServerFactory,
			DiffieHellmanEcdhNistp256.DiffieHellmanEcdhNistp256Factory,
			DiffieHellmanEcdhNistp384.DiffieHellmanEcdhNistp384Factory,
			DiffieHellmanEcdhNistp521.DiffieHellmanEcdhNistp521Factory,
			DiffieHellmanGroup14Sha1JCE.DiffieHellmanGroup14Sha1JCEFactory,
			DiffieHellmanGroup14Sha256JCE.DiffieHellmanGroup14Sha256JCEFactory,
			DiffieHellmanGroup15Sha512JCE.DiffieHellmanGroup15Sha512JCEFactory,
			DiffieHellmanGroup16Sha512JCE.DiffieHellmanGroup16Sha512JCEFactory,
			DiffieHellmanGroup17Sha512JCE.DiffieHellmanGroup17Sha512JCEFactory,
			DiffieHellmanGroup18Sha512JCE.DiffieHellmanGroup18Sha512JCEFactory,
			DiffieHellmanGroup1Sha1JCE.DiffieHellmanGroup1Sha1JCEFactory,
			DiffieHellmanGroupExchangeSha1JCE.DiffieHellmanGroupExchangeSha1JCEFactory,
			DiffieHellmanGroupExchangeSha256JCE.DiffieHellmanGroupExchangeSha256JCEFactory,
			Rsa1024SHA1KeyExchange.Rsa1024SHA1KeyExchangeFactory,
			Rsa2048SHA2KeyExchange.Rsa2048SHA2KeyExchangeFactory;
}

/*-
 * #%L
 * X509 Certificate Support
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

import com.sshtools.common.ssh.components.SshPublicKeyFactory;
import com.sshtools.common.ssh.x509.SshX509DsaPublicKey;
import com.sshtools.common.ssh.x509.SshX509DsaPublicKeyRfc6187;
import com.sshtools.common.ssh.x509.SshX509EcdsaSha2Nist256Rfc6187;
import com.sshtools.common.ssh.x509.SshX509EcdsaSha2Nist384Rfc6187;
import com.sshtools.common.ssh.x509.SshX509EcdsaSha2Nist521Rfc6187;
import com.sshtools.common.ssh.x509.SshX509Rsa2048Sha256Rfc6187;
import com.sshtools.common.ssh.x509.SshX509RsaPublicKey;
import com.sshtools.common.ssh.x509.SshX509RsaPublicKeyRfc6187;
import com.sshtools.common.ssh.x509.SshX509RsaSha1PublicKey;

@SuppressWarnings("rawtypes")
open module com.sshtools.common.ssh.x509 {
	requires transitive com.sshtools.maverick.base;
	requires com.sshtools.common.logger;
	requires com.sshtools.common.util;
	exports com.sshtools.common.ssh.x509;
	
	provides SshPublicKeyFactory with 
			SshX509EcdsaSha2Nist256Rfc6187.SshX509EcdsaSha2Nist256Rfc6187Factory,
			SshX509EcdsaSha2Nist384Rfc6187.SshX509EcdsaSha2Nist384Rfc6187Factory,
			SshX509EcdsaSha2Nist521Rfc6187.SshX509EcdsaSha2Nist521Rfc6187Factory,
			SshX509DsaPublicKey.SshX509DsaPublicKeyFactory,
			SshX509Rsa2048Sha256Rfc6187.SshX509Rsa2048Sha256Rfc6187Factory,
			SshX509RsaPublicKey.SshX509RsaPublicKeyFactory,
			SshX509RsaPublicKeyRfc6187.SshX509RsaPublicKeyRfc6187Factory,
			SshX509RsaSha1PublicKey.SshX509RsaSha1PublicKeyFactory,
			SshX509DsaPublicKeyRfc6187.SshX509DsaPublicKeyRfc6187Factory;
}

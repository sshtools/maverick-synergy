/*
 *    _           _             _   _
 *   (_) __ _  __| | __ _ _ __ | |_(_)_   _____
 *   | |/ _` |/ _` |/ _` | '_ \| __| \ \ / / _ \
 *   | | (_| | (_| | (_| | |_) | |_| |\ V /  __/
 *  _/ |\__,_|\__,_|\__,_| .__/ \__|_| \_/ \___|
 * |__/                  |_|
 *
 * This file is part of the Maverick Synergy Hotfixes Java SSH API
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Copyright (C) 2002-2021 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
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
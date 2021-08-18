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

package com.sshtools.common.publickey;

import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshCertificate;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.jce.OpenSshEcdsaSha2Nist256Certificate;
import com.sshtools.common.ssh.components.jce.OpenSshEcdsaSha2Nist384Certificate;
import com.sshtools.common.ssh.components.jce.OpenSshEcdsaSha2Nist521Certificate;
import com.sshtools.common.ssh.components.jce.OpenSshEd25519Certificate;
import com.sshtools.common.ssh.components.jce.OpenSshRsaCertificate;
import com.sshtools.common.util.UnsignedInteger64;

public class SshCertificateAuthority {

	public static SshCertificate generateUserCertificate(SshKeyPair key,
			long serial,
			String principalName,
			int validityDays,
			SshKeyPair signedBy) throws SshException, IOException {
		return generateCertificate(key, serial, SshCertificate.SSH_CERT_TYPE_USER,
				principalName, principalName, validityDays, signedBy);
	}
	
	public static SshCertificate generateHostCertificate(SshKeyPair key,
			long serial,
			String hostname,
			int validityDays,
			SshKeyPair signedBy) throws SshException, IOException {
		return generateCertificate(key, serial, SshCertificate.SSH_CERT_TYPE_HOST, hostname, Arrays.asList(hostname),
				validityDays, Collections.<CriticalOption>emptyList(), 
				new CertificateExtension.Builder().defaultExtensions().build(), signedBy);
	}
	
	public static SshCertificate generateCertificate(SshKeyPair key, 
			long serial, 
			int type,
			String keyId,
			String principal,
			int validityDays,
			SshKeyPair signedBy) throws SshException, IOException {
		return generateCertificate(key, serial, type, keyId, Arrays.asList(principal),
				validityDays, Collections.<CriticalOption>emptyList(), 
				new CertificateExtension.Builder().defaultExtensions().build(), signedBy);
	}
	
	public static SshCertificate generateCertificate(SshKeyPair key, 
			long serial, 
			int type,
			String keyId,
			String principal,
			int validityDays,
			List<CertificateExtension> extensions,
			SshKeyPair signedBy) throws SshException, IOException {
		return generateCertificate(key, serial, type, keyId, Arrays.asList(principal),
				validityDays, Collections.<CriticalOption>emptyList(), 
				extensions, signedBy);
	}
	
	public static SshCertificate generateCertificate(SshKeyPair key, 
			long serial, 
			int type,
			String keyId,
			List<String> validPrincipals,
			int validityDays,
			List<CriticalOption> criticalOptions,
			List<CertificateExtension> extensions,
			SshKeyPair signedBy) throws SshException, IOException {
		
		switch(type) {
		case SshCertificate.SSH_CERT_TYPE_HOST:
		case SshCertificate.SSH_CERT_TYPE_USER:
			break;
		default:
			throw new SshException(
					String.format("Invalid certificate type %d", type), 
					SshException.BAD_API_USAGE);
		}
		
		Calendar c = Calendar.getInstance();
		c.setTime(new Date());
		c.set(Calendar.HOUR, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		UnsignedInteger64 validAfter = new UnsignedInteger64(c.getTimeInMillis() / 1000);
		
		c.add(Calendar.DAY_OF_MONTH, validityDays);
		UnsignedInteger64 validBefore = new UnsignedInteger64(c.getTimeInMillis() / 1000);

		@SuppressWarnings("unused")
		String reserved = "";

		OpenSshCertificate cert;
		switch(key.getPublicKey().getAlgorithm()) {
		case "ssh-rsa":
		case "rsa-sha2-256":
		case "rsa-sha2-512":
			cert = new OpenSshRsaCertificate();
			break;
		case "ssh-ed25519":
			cert = new OpenSshEd25519Certificate();
			break;
		case "ecdsa-sha2-nistp256":
			cert = new OpenSshEcdsaSha2Nist256Certificate();
			break;
		case "ecdsa-sha2-nistp384":
			cert = new OpenSshEcdsaSha2Nist384Certificate();
			break;
		case "ecdsa-sha2-nistp521":
			cert = new OpenSshEcdsaSha2Nist521Certificate();
			break;
		default:
			throw new SshException(SshException.BAD_API_USAGE, 
					String.format("Unsupported certificate type %s", key.getPublicKey().getAlgorithm()));
		}
		
		cert.sign(key.getPublicKey(), new UnsignedInteger64(serial), type, keyId, validPrincipals,
				validAfter, validBefore, criticalOptions, extensions, signedBy);
		
		return new SshCertificate(key, cert);
	}
}

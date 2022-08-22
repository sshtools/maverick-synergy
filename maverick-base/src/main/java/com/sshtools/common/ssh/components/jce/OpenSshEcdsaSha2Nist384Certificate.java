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

package com.sshtools.common.ssh.components.jce;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import com.sshtools.common.ssh.components.SshPublicKeyFactory;

public class OpenSshEcdsaSha2Nist384Certificate extends OpenSshEcdsaCertificate {

	public static final String CERT_TYPE = "ecdsa-sha2-nistp384-cert-v01@openssh.com";
	
	public static class OpenSshEcdsaSha2Nist384CertificateFactory implements SshPublicKeyFactory<OpenSshEcdsaSha2Nist384Certificate> {
		@Override
		public OpenSshEcdsaSha2Nist384Certificate create() throws NoSuchAlgorithmException, IOException {
			return new OpenSshEcdsaSha2Nist384Certificate();
		}

		@Override
		public String[] getKeys() {
			return new String[] {  CERT_TYPE };
		}
	}
	
	public OpenSshEcdsaSha2Nist384Certificate() {
		super(CERT_TYPE, JCEAlgorithms.JCE_SHA384WithECDSA, "secp384r1");
	}
	
	public int getPriority() {
		return super.getPriority() + 100;
	}
}

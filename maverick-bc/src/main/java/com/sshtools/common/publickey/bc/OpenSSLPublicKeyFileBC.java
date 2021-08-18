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

package com.sshtools.common.publickey.bc;

import java.io.IOException;
import java.io.StringWriter;

import org.bouncycastle.openssl.jcajce.JcaPEMWriter;

import com.sshtools.common.publickey.SshPublicKeyFile;
import com.sshtools.common.ssh.components.SshPublicKey;

public class OpenSSLPublicKeyFileBC implements SshPublicKeyFile {

	byte[] formattedKey;
	SshPublicKey key;
	String comment;
	OpenSSLPublicKeyFileBC(SshPublicKey key, String comment) {
		this.key = key;
		this.comment = comment;
	}
	
	@Override
	public SshPublicKey toPublicKey() throws IOException {
		return key;
	}

	@Override
	public String getComment() {
		return comment;
	}

	@Override
	public byte[] getFormattedKey() throws IOException {
		
		StringWriter out = new StringWriter();
		try(JcaPEMWriter writer = new JcaPEMWriter(out)) {
			writer.writeObject(key.getJCEPublicKey());
			writer.flush();
			return out.toString().getBytes("UTF-8");
		}
	}

	@Override
	public String getOptions() {
		return "";
	}

}

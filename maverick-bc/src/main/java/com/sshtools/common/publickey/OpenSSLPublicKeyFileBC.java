/**
 * (c) 2002-2019 JADAPTIVE Limited. All Rights Reserved.
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
package com.sshtools.common.publickey;

import java.io.IOException;
import java.io.StringWriter;

import org.bouncycastle.openssl.jcajce.JcaPEMWriter;

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

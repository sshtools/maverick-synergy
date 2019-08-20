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
package com.sshtools.common.ssh.components.jce;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import com.sshtools.common.publickey.SshPublicKeyFileFactory;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;
import com.sshtools.common.util.UnsignedInteger64;

/**
 * To generate a key that supports this use
 * 
 * ssh-keygen -s ca_key -I 2 -n lee,kelly -z 12345 -O force-command=ls -O
 * source-address=192.168.82.0/24 -O no-port-forwarding user_key.pub
 * 
 * @author lee
 * 
 */
public abstract class OpenSshCertificate implements SshPublicKey {

	public static final int SSH_CERT_TYPE_USER = 1;
	public static final int SSH_CERT_TYPE_HOST = 2;

	public static final String PERMIT_X11_FORWARDING = "permit-x11-forwarding";
	public static final String PERMIT_PORT_FORWARDING = "permit-port-forwarding";
	public static final String PERMIT_AGENT_FORWARDING = "permit-agent-forwarding";
	public static final String PERMIT_USER_PTY = "permit-pty";
	public static final String PERMIT_USER_RC = "permit-user-rc";

	public static final String OPTION_FORCE_COMMAND = "force-command";
	public static final String OPTION_SOURCE_ADDRESS = "source-address";

	UnsignedInteger64 serial;
	int type;
	String keyId;
	Set<String> validPrincipals = new HashSet<String>();
	UnsignedInteger64 validAfter;
	UnsignedInteger64 validBefore;
	Map<String, String> criticalOptions = new HashMap<String, String>();
	Set<String> extensions = new HashSet<String>();
	String reserved;
	SshPublicKey signedBy;
	byte[] signature;
	
	public String getEncodingAlgorithm() {
		return getAlgorithm();
	}
	
	protected void encode(ByteArrayWriter writer) throws IOException, SshException {

		writer.writeUINT64(serial);
		
		writer.writeInt(type);
		
		writer.writeString(keyId);
		
		ByteArrayWriter users = new ByteArrayWriter();
		for(String user : validPrincipals) {
			users.writeString(user);
		}
		
		writer.writeBinaryString(users.toByteArray());
		users.close();
		
		writer.writeUINT64(validAfter);
		writer.writeUINT64(validBefore);
		
		ByteArrayWriter options = new ByteArrayWriter();
		for(String option : criticalOptions.keySet()) {
			options.writeString(option);
			options.writeString(criticalOptions.get(option));
		}
		
		writer.writeBinaryString(options.toByteArray());
		options.close();
		
		ByteArrayWriter ext = new ByteArrayWriter();
		for(String e : extensions) {
			ext.writeString(e);
		}
		
		writer.writeBinaryString(ext.toByteArray());
		ext.close();
		
		writer.writeString(reserved);
		
		writer.writeBinaryString(signedBy.getEncoded());
		
		writer.writeBinaryString(signature);
	}
	
	protected void decode(ByteArrayReader reader) throws IOException,
			SshException {

		// ByteArrayWriter data = new ByteArrayWriter();

		serial = reader.readUINT64();
		// data.writeUINT64(serial);

		type = (int) reader.readInt();
		// data.writeInt(type);

		keyId = reader.readString();
		// data.writeString(keyId);

		byte[] buf = reader.readBinaryString();
		// data.writeBinaryString(buf);
		ByteArrayReader tmp = new ByteArrayReader(buf);
		while (tmp.available() > 0) {
			validPrincipals.add(tmp.readString());
		}
		tmp.close();

		validAfter = reader.readUINT64();
		// data.writeUINT64(validAfter);

		validBefore = reader.readUINT64();
		tmp = new ByteArrayReader(reader.readBinaryString());
		while (tmp.available() > 0) {
			criticalOptions.put(tmp.readString(), tmp.readString());
		}
		tmp.close();
		tmp = new ByteArrayReader(reader.readBinaryString());
		while (tmp.available() > 0) {
			String name = tmp.readString().trim();
			if (!name.equals("")) {
				extensions.add(name);
			}
		}
		tmp.close();
		reserved = reader.readString();

		signedBy = SshPublicKeyFileFactory.decodeSSH2PublicKey(reader
				.readBinaryString());

		signature = reader.readBinaryString();

		byte[] data = new byte[reader.array().length - (signature.length + 4)];
		System.arraycopy(reader.array(), 0, data, 0, data.length);

		signedBy.verifySignature(signature, data);
	}

	public SshPublicKey getSignedBy() {
		return signedBy;
	}

	public int getType() {
		return type;
	}

	public Set<String> getPrincipals() {
		return validPrincipals;
	}

	public Set<String> getExtensions() {
		return extensions;
	}

	public boolean isForceCommand() {
		return criticalOptions.containsKey(OPTION_FORCE_COMMAND);
	}

	public String getForcedCommand() {
		return criticalOptions.get(OPTION_FORCE_COMMAND);
	}

	public Set<String> getSourceAddresses() {
		Set<String> tmp = new HashSet<String>();

		if (criticalOptions.containsKey(OPTION_SOURCE_ADDRESS)) {
			StringTokenizer t = new StringTokenizer(
					criticalOptions.get(OPTION_SOURCE_ADDRESS), ",");

			while (t.hasMoreTokens()) {
				tmp.add(t.nextToken());
			}
		}
		return tmp;
	}
	
	public Date getValidBefore() {
		return new Date(validBefore.longValue() * 1000);
	}
	
	public Date getValidAfter() {
		return new Date(validAfter.longValue() * 1000);
	}
	
	public UnsignedInteger64 getSerial() {
		return serial;
	}
	
	public String getKeyId() {
		return keyId;
	}
}

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
/* HEADER */
package com.sshtools.common.publickey;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;

import com.sshtools.common.auth.AuthenticationMechanismFactory;
import com.sshtools.common.auth.Authenticator;
import com.sshtools.common.auth.PublicKeyAuthentication;
import com.sshtools.common.auth.PublicKeyAuthenticationProvider;
import com.sshtools.common.logger.Log;
import com.sshtools.common.policy.AuthenticationPolicy;
import com.sshtools.common.ssh.Packet;
import com.sshtools.common.ssh.Subsystem;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.util.ByteArrayReader;

public class PublicKeySubsystemServer extends Subsystem {

	static final int SUCCESS = 0;
	static final int ACCESS_DENIED = 1;
	static final int STORAGE_EXCEEDED = 2;
	static final int REQUEST_NOT_SUPPPORTED = 3;
	static final int KEY_NOT_FOUND = 4;
	static final int KEY_NOT_SUPPORTED = 5;
	static final int GENERAL_FAILURE = 6;

	public static final String SUBSYSTEM_NAME = "publickey@vandyke.com";

	public PublicKeySubsystemServer() {
		super("publickey");
	}

	@Override
	protected void onSubsystemFree() {
	}

	protected void onMessageReceived(byte[] msg) throws IOException {
		ByteArrayReader bar = new ByteArrayReader(msg);
		try {
			String cmd = bar.readString();
			if (cmd.equals("version")) {
				processVersion(bar);
			} else if (cmd.equals("add")) {
				processAddKey(bar);
			} else if (cmd.equals("remove")) {
				processRemoveKey(bar);
			} else if (cmd.equals("list")) {
				processKeyList(bar);
			} else {
				throw new IOException("The client sent an invalid request");
			}
		} finally {
			onFreeMessage(msg);
			bar.close();
		}

	}

	private void processKeyList(ByteArrayReader bar) throws IOException {
		try {
			
			for (Authenticator p : getProviders()) {
				PublicKeyAuthenticationProvider ap = (PublicKeyAuthenticationProvider) p;
				try {
					for (Iterator<SshPublicKeyFile> i = ap.getKeys(getConnection()); i.hasNext();) {
						SshPublicKeyFile keyFile = i.next();
						Packet packet = new Packet();
						packet.writeString(keyFile.getComment());
						packet.writeString(keyFile.getOptions());
						packet.writeBinaryString(keyFile.getFormattedKey());
						sendMessage(packet);
					}
					return;
				} catch (UnsupportedOperationException uoe) {
				}
			}
			throw new UnsupportedOperationException();
		} catch (SecurityException iae) {
			writeStatusResponse(ACCESS_DENIED, "Access denied.");
		} catch (UnsupportedOperationException uoe) {
			writeStatusResponse(REQUEST_NOT_SUPPPORTED,
					"list not supported.");
		} catch (Exception e) {
			writeStatusResponse(GENERAL_FAILURE, e.getMessage());
		}
		writeStatusResponse(SUCCESS, "OK");
	}

	private void processRemoveKey(ByteArrayReader bar) throws IOException {
		String algorithm = bar.readString();
		byte[] encodedKey = bar.readBinaryString();
		try {
			SshPublicKey key = SshPublicKeyFileFactory.decodeSSH2PublicKey(
					algorithm, encodedKey);
			for (Authenticator p : getProviders()) {
				PublicKeyAuthenticationProvider ap = (PublicKeyAuthenticationProvider) p;
				try {
					ap.remove(key, getConnection());
					writeStatusResponse(SUCCESS, "Public key removed.");
					return;
				} catch (UnsupportedOperationException uoe) {
				}
			}
			throw new UnsupportedOperationException();
		} catch (FileNotFoundException fnfe) {
			writeStatusResponse(KEY_NOT_FOUND, "Remove not supported.");
		} catch (SecurityException iae) {
			writeStatusResponse(ACCESS_DENIED, "Access denied.");
		} catch (UnsupportedOperationException uoe) {
			writeStatusResponse(REQUEST_NOT_SUPPPORTED,
					"Remove not supported.");
		} catch (Exception e) {
			writeStatusResponse(GENERAL_FAILURE, e.getMessage());
		}
	}

	private void processAddKey(ByteArrayReader bar) throws IOException {
		String comment = bar.readString();
		String algorithm = bar.readString();
		byte[] encodedKey = bar.readBinaryString();
		SshPublicKey key = SshPublicKeyFileFactory.decodeSSH2PublicKey(
				algorithm, encodedKey);
		try {
			for (Authenticator p : getProviders()) {
				PublicKeyAuthenticationProvider ap = (PublicKeyAuthenticationProvider) p;
				try {
					ap.add(key, comment, getConnection());
					writeStatusResponse(SUCCESS, "Public key created.");
					return;
				} catch (UnsupportedOperationException uoe) {
				}
			}
			throw new UnsupportedOperationException();
		} catch (UnsupportedOperationException uoe) {
			writeStatusResponse(REQUEST_NOT_SUPPPORTED,
					"Add not supported.");
		} catch (IllegalArgumentException iae) {
			writeStatusResponse(KEY_NOT_SUPPORTED, "Key not supported.");
		} catch (SecurityException iae) {
			writeStatusResponse(ACCESS_DENIED, "Access denied.");
		} catch (Exception e) {
			writeStatusResponse(GENERAL_FAILURE, e.getMessage());
		}
	}

	private void processVersion(ByteArrayReader bar) throws IOException {
		int clientVersion = (int) bar.readInt();
		if(Log.isDebugEnabled()) {
			Log.debug("Client publickey subsystem version " + clientVersion);
		}
		Packet packet = new Packet();
		packet.writeString(context.getPolicy(AuthenticationPolicy.class).getBannerMessage());
		packet.writeInt(1);
		sendMessage(packet);
	}

	private Authenticator[] getProviders() {
		return getContext().getPolicy(AuthenticationMechanismFactory.class).getProviders(
				PublicKeyAuthentication.AUTHENTICATION_METHOD, getConnection());
	}

	void writeStatusResponse(int status, String desc) throws IOException {
		Packet packet = new Packet();
		packet.writeString("status");
		packet.writeInt(status);
		packet.writeString(desc);
		sendMessage(packet);
	}

	@Override
	protected void cleanupSubsystem() {
		
	}

}
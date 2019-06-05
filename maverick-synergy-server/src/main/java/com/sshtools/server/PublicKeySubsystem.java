/* HEADER */
package com.sshtools.server;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;

import com.sshtools.common.auth.AuthenticationMechanismFactory;
import com.sshtools.common.auth.Authenticator;
import com.sshtools.common.auth.PublicKeyAuthentication;
import com.sshtools.common.auth.PublicKeyAuthenticationProvider;
import com.sshtools.common.logger.Log;
import com.sshtools.common.policy.AuthenticationPolicy;
import com.sshtools.common.publickey.SshPublicKeyFile;
import com.sshtools.common.publickey.SshPublicKeyFileFactory;
import com.sshtools.common.ssh.Packet;
import com.sshtools.common.ssh.Subsystem;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.util.ByteArrayReader;

public class PublicKeySubsystem extends Subsystem {

	

	static final int SUCCESS = 0;
	static final int ACCESS_DENIED = 1;
	static final int STORAGE_EXCEEDED = 2;
	static final int REQUEST_NOT_SUPPPORTED = 3;
	static final int KEY_NOT_FOUND = 4;
	static final int KEY_NOT_SUPPORTED = 5;
	static final int GENERAL_FAILURE = 6;

	public static final String SUBSYSTEM_NAME = "publickey@vandyke.com";

	public PublicKeySubsystem() {
		super("publickey");
	}

	@Override
	protected void onSubsystemFree() {
	}

	protected void onMessageReceived(byte[] msg) throws IOException {
		
		try(ByteArrayReader bar = new ByteArrayReader(msg)) {
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
		packet.writeString(getContext().getPolicy(AuthenticationPolicy.class).getBannerMessage());
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
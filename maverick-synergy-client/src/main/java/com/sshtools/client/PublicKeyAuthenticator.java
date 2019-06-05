package com.sshtools.client;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.sshtools.common.ssh.Connection;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.TransportProtocol;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.util.ByteArrayReader;
import com.sshtools.common.util.ByteArrayWriter;

/**
 * Implements public key authentication taking a separately loaded SshKeyPair as the private key for authentication.
 */
public class PublicKeyAuthenticator extends SimpleClientAuthenticator implements ClientAuthenticator, SignatureGenerator {

	public final static int SSH_MSG_USERAUTH_PK_OK = 60;
	
	boolean isAuthenticating = false;
	TransportProtocolClient transport;
	String username;
	Collection<SshPublicKey> publicKeys;
	Collection<SshKeyPair> keypairs;
	SshPublicKey authenticatingKey = null;
	
	public PublicKeyAuthenticator() {
	}
	
	public PublicKeyAuthenticator(SshKeyPair... keys) {
		keypairs = new ArrayList<SshKeyPair>();
		keypairs.addAll(Arrays.asList(keys));
	}
	
	public void setKeyPair(SshKeyPair... pair) {
		keypairs = Arrays.asList(pair);
	}

	@Override
	public void authenticate(TransportProtocolClient transport, String username) {
		
		onStartAuthentication(transport.getConnection());
		
		this.transport = transport;
		this.username = username;

		this.publicKeys = new ArrayList<SshPublicKey>(getSignatureGenerator(transport.getConnection()).getPublicKeys());
		doPublicKeyAuth();

	}

	protected void onStartAuthentication(Connection<SshClientContext> con) {
		
	}
	
	void doPublicKeyAuth() {
		
		try {

			final byte[] msg = generateAuthenticationRequest(generateSignatureData());
			
			transport.postMessage(new AuthenticationMessage(username, "ssh-connection", "publickey") {

				@Override
				public boolean writeMessageIntoBuffer(ByteBuffer buf) {

					super.writeMessageIntoBuffer(buf);
					buf.put(msg);
					return true;
				}
				
			});
		} catch (IOException e) {
			disconnect("Internal error");
		} catch (SshException e) {
			disconnect("Internal error");
		}
	}
	
	private void disconnect(String desc) {
		transport.disconnect(TransportProtocol.AUTH_CANCELLED_BY_USER, desc);
	}
	
	byte[] generateSignatureData() throws IOException,
			SshException {

		ByteArrayWriter baw = new ByteArrayWriter();
		authenticatingKey = publicKeys.iterator().next();
		try {
			baw.writeBinaryString(transport.getSessionKey());
			baw.write(AuthenticationProtocolClient.SSH_MSG_USERAUTH_REQUEST);
			baw.writeString(username);
			baw.writeString("ssh-connection");
			baw.writeString("publickey");
			baw.writeBoolean(isAuthenticating);
			baw.writeString(authenticatingKey.getAlgorithm());
			baw.writeBinaryString(authenticatingKey.getEncoded());

			return baw.toByteArray();

		} finally {
			baw.close();
		}
	}

	byte[] generateAuthenticationRequest(byte[] data) throws IOException, SshException {

		ByteArrayWriter baw = new ByteArrayWriter();
		SshPublicKey key = publicKeys.iterator().next();
		
		try {
			baw.writeBoolean(isAuthenticating);
			baw.writeString(key.getAlgorithm());
			baw.writeBinaryString(key.getEncoded());
	
			if (isAuthenticating) {
	
				byte[] signature = getSignatureGenerator(transport.getConnection()).sign(key, data);
				
				// Format the signature correctly
				ByteArrayWriter sig = new ByteArrayWriter();
	
				try {
					sig.writeString(key.getAlgorithm());
					sig.writeBinaryString(signature);
					baw.writeBinaryString(sig.toByteArray());
				} finally {
					sig.close();
				}
			}
			
			return baw.toByteArray();
		
		} finally {
			baw.close();
		}

	}

	@Override
	public boolean processMessage(ByteArrayReader msg) throws IOException {
		
		switch(msg.read()) {
		case SSH_MSG_USERAUTH_PK_OK:
		{
			isAuthenticating = true;
			doPublicKeyAuth();
			return true;
		}
		case AuthenticationProtocolClient.SSH_MSG_USERAUTH_FAILURE:
		{
			if(!isAuthenticating) {
				publicKeys.remove(authenticatingKey);
				authenticatingKey = null;
				if(!publicKeys.isEmpty()) {
					doPublicKeyAuth();
					return true;
				}
			}
		}
		}

		return false;
	}

	@Override
	public byte[] sign(SshPublicKey key, byte[] data) throws SshException {
		
		SshKeyPair pair = null;
		for(SshKeyPair p : keypairs) {
			if(p.getPublicKey().equals(key)) {
				pair = p;
				break;
			}
		}
		
		if(pair==null) {
			throw new IllegalStateException("Public key is not in the key pair list!");
		}
		try {
			return pair.getPrivateKey().sign(data);
		} catch (IOException e) {
			throw new SshException(e);
		}
	}
	
	public SignatureGenerator getSignatureGenerator(Connection<SshClientContext> con) {
		return this;
	}

	@Override
	public Collection<SshPublicKey> getPublicKeys() {
		List<SshPublicKey> keys = new ArrayList<SshPublicKey>();
		for(SshKeyPair pair : keypairs) {
			keys.add(pair.getPublicKey());
		}
		return keys;
	}


	@Override
	public String getName() {
		return "publickey";
	}
}

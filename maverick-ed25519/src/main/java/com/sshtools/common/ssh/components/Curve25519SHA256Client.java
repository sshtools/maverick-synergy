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
package com.sshtools.common.ssh.components;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import com.sshtools.client.SshClientContext;
import com.sshtools.client.SshKeyExchangeClient;
import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.SshIOException;
import com.sshtools.common.ssh.SshTransport;
import com.sshtools.common.ssh.components.jce.JCEComponentManager;
import com.sshtools.common.sshd.SshMessage;
import com.sshtools.common.util.ByteArrayReader;

public class Curve25519SHA256Client extends SshKeyExchangeClient {

	public static final int SSH_MSG_KEX_ECDH_INIT = 30;
	public static final int SSH_MSG_KEX_ECDH_REPLY = 31;

	public static final String CURVE25519_SHA2 = "curve25519-sha256@libssh.org";

	byte[] f;
	byte[] privateKey;
	byte[] e;

	String clientId;
	String serverId;
	byte[] clientKexInit;
	byte[] serverKexInit;

	public Curve25519SHA256Client() {
		super("SHA-256");
	}

	@Override
	public String getAlgorithm() {
		return CURVE25519_SHA2;
	}

	private void initCrypto()
			throws InvalidKeyException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, SshException {
		e = new byte[32];
		privateKey = new byte[32];
		JCEComponentManager.getSecureRandom().nextBytes(privateKey);
		com.sshtools.common.ssh.components.Curve25519.keygen(e, null, privateKey);
	}

	public void test() {

		try {
			initCrypto();
		} catch (Throwable e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

	@Override
	public void init(SshTransport<SshClientContext> transport, String clientId, String serverId,
			byte[] clientKexInit, byte[] serverKexInit, SshPrivateKey prvkey, SshPublicKey pubkey,
			boolean firstPacketFollows, boolean useFirstPacket) throws IOException, SshException {

		this.transport = transport;
		this.clientId = clientId;
		this.serverId = serverId;
		this.clientKexInit = clientKexInit;
		this.serverKexInit = serverKexInit;

		try {
			initCrypto();

			transport.postMessage(new SshMessage() {
				public boolean writeMessageIntoBuffer(ByteBuffer buf) {

					buf.put((byte) SSH_MSG_KEX_ECDH_INIT);
					buf.putInt(e.length);
					buf.put(e);

					return true;
				}

				public void messageSent(Long sequenceNo) {
					if (Log.isDebugEnabled())
						Log.debug("Sent SSH_MSG_KEX_ECDH_INIT");
				}
			}, true);
		} catch (SshException e) {
			throw new SshIOException(e);
		} catch (Exception e) {
			throw new SshException(e, SshException.KEY_EXCHANGE_FAILED);
		}
	}

	@Override
	public boolean processMessage(byte[] resp) throws SshException, IOException {

		if (resp[0] != SSH_MSG_KEX_ECDH_REPLY) {
			return false;
		}

		if (resp[0] != SSH_MSG_KEX_ECDH_REPLY) {
			throw new SshException("Expected SSH_MSG_KEX_ECDH_REPLY but got message id " + resp[0],
					SshException.KEY_EXCHANGE_FAILED);
		}

		try (ByteArrayReader reply = new ByteArrayReader(resp, 1, resp.length - 1)) {
			hostKey = reply.readBinaryString();
			f = reply.readBinaryString();
			signature = reply.readBinaryString();

			byte[] k = new byte[32];
			com.sshtools.common.ssh.components.Curve25519.curve(k, privateKey, f);
			secret = new BigInteger(1, k);

			calculateExchangeHash();

			transport.sendNewKeys();
		} catch (Exception e) {
			Log.error("Key exchange failed", e);
			throw new SshException("Failed to process key exchange", SshException.INTERNAL_ERROR, e);
		}

		return true;

	}

	protected void calculateExchangeHash() throws SshException {
		Digest hash = (Digest) ComponentManager.getInstance().supportedDigests().getInstance(getHashAlgorithm());

		// The local software version comments
		hash.putString(clientId);

		// The remote software version comments
		hash.putString(serverId);

		// The local kex init payload
		hash.putInt(clientKexInit.length);
		hash.putBytes(clientKexInit);

		// The remote kex init payload
		hash.putInt(serverKexInit.length);
		hash.putBytes(serverKexInit);

		// The host key
		hash.putInt(hostKey.length);
		hash.putBytes(hostKey);

		hash.putInt(e.length);
		hash.putBytes(e);

		hash.putInt(f.length);
		hash.putBytes(f);

		// The diffie hellman k value
		hash.putBigInteger(secret);

		// Do the final output
		exchangeHash = hash.doFinal();
	}

	public String getProvider() {
		return "";
	}
}
